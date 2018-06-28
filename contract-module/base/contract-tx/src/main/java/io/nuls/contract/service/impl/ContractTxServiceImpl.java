/**
 * MIT License
 * <p>
 * Copyright (c) 2017-2018 nuls.io
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package io.nuls.contract.service.impl;

import io.nuls.account.ledger.model.CoinDataResult;
import io.nuls.account.ledger.service.AccountLedgerService;
import io.nuls.account.model.Account;
import io.nuls.account.service.AccountService;
import io.nuls.account.util.AccountTool;
import io.nuls.contract.constant.ContractConstant;
import io.nuls.contract.constant.ContractErrorCode;
import io.nuls.contract.entity.BlockHeaderDto;
import io.nuls.contract.entity.tx.CallContractTransaction;
import io.nuls.contract.entity.tx.CreateContractTransaction;
import io.nuls.contract.entity.tx.DeleteContractTransaction;
import io.nuls.contract.entity.txdata.CallContractData;
import io.nuls.contract.entity.txdata.CreateContractData;
import io.nuls.contract.entity.txdata.DeleteContractData;
import io.nuls.contract.helper.VMHelper;
import io.nuls.contract.service.ContractTxService;
import io.nuls.contract.storage.service.ContractAddressStorageService;
import io.nuls.contract.util.VMContext;
import io.nuls.contract.vm.program.ProgramCall;
import io.nuls.contract.vm.program.ProgramCreate;
import io.nuls.contract.vm.program.ProgramExecutor;
import io.nuls.contract.vm.program.ProgramResult;
import io.nuls.core.tools.log.Log;
import io.nuls.core.tools.param.AssertUtil;
import io.nuls.core.tools.str.StringUtils;
import io.nuls.kernel.cfg.NulsConfig;
import io.nuls.kernel.exception.NulsException;
import io.nuls.kernel.func.TimeService;
import io.nuls.kernel.lite.annotation.Autowired;
import io.nuls.kernel.lite.annotation.Service;
import io.nuls.kernel.lite.core.bean.InitializingBean;
import io.nuls.kernel.model.*;
import io.nuls.kernel.script.P2PKHScriptSig;
import io.nuls.kernel.utils.AddressTool;
import io.nuls.kernel.utils.TransactionFeeCalculator;
import io.nuls.protocol.service.TransactionService;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;

/**
 * @desription:
 * @author: PierreLuo
 * @date: 2018/5/22
 */
@Service
public class ContractTxServiceImpl implements ContractTxService, InitializingBean {

    private static final String GET = "get";

    @Autowired
    private AccountService accountService;
    @Autowired
    private AccountLedgerService accountLedgerService;
    @Autowired
    private TransactionService transactionService;
    @Autowired
    private ContractAddressStorageService contractAddressStorageService;
    @Autowired
    private VMHelper vmHelper;
    @Autowired
    private VMContext vmContext;

    private ProgramExecutor programExecutor;

    @Override
    public void afterPropertiesSet() throws NulsException {
        programExecutor = vmHelper.getProgramExecutor();
    }

    /**
     * 创建包含智能合约的交易
     * 如果是创建合约的交易，交易仅仅用于创建合约，合约内部不执行复杂逻辑
     *
     * @param sender          交易创建者
     * @param value           交易附带的货币量
     * @param naLimit         最大Na消耗
     * @param price           执行合约单价
     * @param contractAddress 合约地址
     * @param contractCode    合约代码
     * @param args            参数列表
     * @param password        账户密码
     * @param remark          备注
     * @return
     */
    @Override
    public Result contractCreateTx(String sender, Na value, Na naLimit, byte price,
                                   byte[] contractCode, String[] args,
                                   String password, String remark) {
        try {
            AssertUtil.canNotEmpty(sender, "the sender address can not be empty");
            AssertUtil.canNotEmpty(contractCode, "the contractCode can not be empty");
            value = Na.ZERO;

            Result<Account> accountResult = accountService.getAccount(sender);
            if (accountResult.isFailed()) {
                return accountResult;
            }

            Account account = accountResult.getData();
            // 验证账户密码
            if (accountService.isEncrypted(account).isSuccess() && account.isLocked()) {
                AssertUtil.canNotEmpty(password, "the password can not be empty");

                Result passwordResult = accountService.validPassword(account, password);
                if (passwordResult.isFailed()) {
                    return passwordResult;
                }
            }

            // 生成一个地址作为智能合约地址
            Address contractAddress = AccountTool.createContractAddress();

            byte[] contractAddressBytes = contractAddress.getAddressBytes();
            byte[] senderBytes = AddressTool.getAddress(sender);

            CreateContractTransaction tx = new CreateContractTransaction();
            if (StringUtils.isNotBlank(remark)) {
                try {
                    tx.setRemark(remark.getBytes(NulsConfig.DEFAULT_ENCODING));
                } catch (UnsupportedEncodingException e) {
                    Log.error(e);
                    throw new RuntimeException(e);
                }
            }
            tx.setTime(TimeService.currentTimeMillis());


            // 计算CoinData
            /*
             * 智能合约计算手续费以消耗的Gas*Price为根据，然而创建交易时并不执行智能合约，
             * 所以此时交易的CoinData是不固定的，比实际要多，
             * 打包时执行智能合约，真实的手续费已算出，然而tx的手续费已扣除，
             * 多扣除的费用会以CoinBase交易还给Sender
             */
            CoinData coinData = new CoinData();
            // 向智能合约账户转账
            if(!Na.ZERO.equals(value)) {
                Coin toCoin = new Coin(contractAddressBytes, value);
                coinData.getTo().add(toCoin);
            }

            // 当前区块高度
            BlockHeaderDto blockHeader = vmContext.getBlockHeader();
            long blockHeight = blockHeader.getHeight();
            // 当前区块状态根
            byte[] prevStateRoot = blockHeader.getStateRoot();
            // 执行VM估算Gas消耗
            ProgramCreate programCreate = new ProgramCreate();
            programCreate.setContractAddress(contractAddressBytes);
            programCreate.setSender(senderBytes);
            programCreate.setValue(BigInteger.valueOf(value.getValue()));
            programCreate.setPrice(price);
            programCreate.setNaLimit(naLimit.getValue());
            programCreate.setNumber(blockHeight);
            programCreate.setContractCode(contractCode);
            if(args != null) {
                programCreate.setArgs(args);
            }
            ProgramExecutor track = programExecutor.begin(prevStateRoot);
            ProgramResult programResult = track.create(programCreate);
            if(programResult.isError()) {
                Result result = Result.getFailed(ContractErrorCode.DATA_ERROR);
                result.setMsg(programResult.getErrorMessage());
                return result;
            }
            long gasUsed = programResult.getGasUsed();
            // 预估1.5倍Gas
            gasUsed += gasUsed >> 1;
            Na imputedNa = Na.valueOf(gasUsed * price);
            // 总花费
            Na totalNa = imputedNa.add(value);
            CoinDataResult coinDataResult = accountLedgerService.getCoinData(senderBytes, totalNa, tx.size() + P2PKHScriptSig.DEFAULT_SERIALIZE_LENGTH, TransactionFeeCalculator.MIN_PRECE_PRE_1024_BYTES);
            if (!coinDataResult.isEnough()) {
                return Result.getFailed(ContractErrorCode.BALANCE_NOT_ENOUGH);
            }
            coinData.setFrom(coinDataResult.getCoinList());
            // 找零的UTXO
            if (coinDataResult.getChange() != null) {
                coinData.getTo().add(coinDataResult.getChange());
            }
            tx.setCoinData(coinData);

            // 组装txData
            CreateContractData createContractData = new CreateContractData();
            createContractData.setSender(senderBytes);
            createContractData.setContractAddress(contractAddressBytes);
            createContractData.setValue(value.getValue());
            createContractData.setNaLimit(naLimit.getValue());
            createContractData.setPrice(price);
            createContractData.setCodeLen(contractCode.length);
            createContractData.setCode(contractCode);
            // 本次交易使用的Gas消耗
            createContractData.setTxGasUsed(imputedNa.getValue());
            if(args != null) {
                createContractData.setArgsCount((byte) args.length);
                createContractData.setArgs(args);
            }
            tx.setTxData(createContractData);

            tx.setHash(NulsDigestData.calcDigestData(tx.serialize()));
            // 交易签名
            P2PKHScriptSig sig = new P2PKHScriptSig();
            sig.setPublicKey(account.getPubKey());
            sig.setSignData(accountService.signData(tx.getHash().serialize(), account, password));
            tx.setScriptSig(sig.serialize());

            // 保存未确认交易到本地账本
            Result saveResult = accountLedgerService.verifyAndSaveUnconfirmedTransaction(tx);
            if (saveResult.isFailed()) {
                return saveResult;
            }

            // 广播交易
            Result sendResult = transactionService.broadcastTx(tx);
            if (sendResult.isFailed()) {
                accountLedgerService.rollbackTransaction(tx);
                return sendResult;
            }

            return Result.getSuccess().setData(tx.getHash().getDigestHex());
        } catch (IOException e) {
            Log.error(e);
            Result result = Result.getFailed(ContractErrorCode.CONTRACT_TX_CREATE_ERROR);
            result.setMsg(e.getMessage());
            return result;
        } catch (NulsException e) {
            Log.error(e);
            return Result.getFailed(e.getErrorCode());
        } catch (Exception e) {
            Log.error(e);
            Result result = Result.getFailed(ContractErrorCode.CONTRACT_TX_CREATE_ERROR);
            result.setMsg(e.getMessage());
            return result;
        }
    }

    /**
     * 创建调用智能合约的交易
     *
     * @param sender          交易创建者
     * @param value           交易附带的货币量
     * @param naLimit         最大Na消耗
     * @param price           执行合约单价
     * @param contractAddress 合约地址
     * @param methodName      方法名
     * @param methodDesc      方法签名，如果方法名不重复，可以不传
     * @param args            参数列表
     * @param password        账户密码
     * @param remark          备注
     * @return
     */
    @Override
    public Result contractCallTx(String sender, Na value, Na naLimit, byte price, String contractAddress,
                                 String methodName, String methodDesc, String[] args,
                                 String password, String remark) {
        try {
            AssertUtil.canNotEmpty(sender, "the sender address can not be empty");
            AssertUtil.canNotEmpty(contractAddress, "the contractAddress can not be empty");
            AssertUtil.canNotEmpty(methodName, "the methodName can not be empty");
            if (value == null) {
                value = Na.ZERO;
            }

            Result<Account> accountResult = accountService.getAccount(sender);
            if (accountResult.isFailed()) {
                return accountResult;
            }

            Account account = accountResult.getData();
            // 验证账户密码
            if (accountService.isEncrypted(account).isSuccess() && account.isLocked()) {
                AssertUtil.canNotEmpty(password, "the password can not be empty");

                Result passwordResult = accountService.validPassword(account, password);
                if (passwordResult.isFailed()) {
                    return passwordResult;
                }
            }

            if(ContractConstant.BALANCE_TRIGGER_METHOD_NAME.equals(methodName)
                    && ContractConstant.BALANCE_TRIGGER_METHOD_DESC.equals(methodDesc)){
                if(naLimit == null) {
                    //TODO pierre NaLimit设置一个默认值, 执行预估合约需要NaLimit为参数，而设置一个默认的NaLimit又需要执行预估合约来做参考
                }
            }

            byte[] senderBytes = AddressTool.getAddress(sender);
            byte[] contractAddressBytes = AddressTool.getAddress(contractAddress);

            // 当前区块高度
            BlockHeaderDto blockHeader = vmContext.getBlockHeader();
            long blockHeight = blockHeader.getHeight();
            // 当前区块状态根
            byte[] prevStateRoot = blockHeader.getStateRoot();

            // 组装VM执行数据
            ProgramCall programCall = new ProgramCall();
            programCall.setContractAddress(contractAddressBytes);
            programCall.setSender(senderBytes);
            programCall.setValue(BigInteger.valueOf(value.getValue()));
            programCall.setPrice(price);
            programCall.setNaLimit(naLimit.getValue());
            programCall.setNumber(blockHeight);
            programCall.setMethodName(methodName);
            programCall.setMethodDesc(methodDesc);
            programCall.setArgs(args);

            // 如果方法名前缀是get，则是不上链的合约调用，同步执行合约代码，不改变状态根，并返回值
            if(methodName.startsWith(GET)) {
                ProgramExecutor track = programExecutor.begin(prevStateRoot);
                ProgramResult programResult = track.call(programCall);
                Result result = null;
                if(programResult.isError()) {
                    result = Result.getFailed(ContractErrorCode.DATA_ERROR);
                    result.setMsg(programResult.getErrorMessage());
                } else {
                    result = Result.getSuccess();
                    result.setData(programResult.getResult());
                }
                return result;
            }


            // 创建链上交易，包含智能合约
            CallContractTransaction tx = new CallContractTransaction();
            if (StringUtils.isNotBlank(remark)) {
                try {
                    tx.setRemark(remark.getBytes(NulsConfig.DEFAULT_ENCODING));
                } catch (UnsupportedEncodingException e) {
                    Log.error(e);
                    throw new RuntimeException(e);
                }
            }
            tx.setTime(TimeService.currentTimeMillis());

            // 计算CoinData
            /*
             * 智能合约计算手续费以消耗的Gas*Price为根据，然而创建交易时并不执行智能合约，
             * 所以此时交易的CoinData是不固定的，比实际要多，
             * 打包时执行智能合约，真实的手续费已算出，然而tx的手续费已扣除，
             * 多扣除的费用会以CoinBase交易还给Sender
             */
            CoinData coinData = new CoinData();
            // 向智能合约账户转账
            if(!Na.ZERO.equals(value)) {
                Coin toCoin = new Coin(contractAddressBytes, value);
                coinData.getTo().add(toCoin);
            }

            // 执行VM估算Gas消耗
            ProgramExecutor track = programExecutor.begin(prevStateRoot);
            ProgramResult programResult = track.call(programCall);

            if(programResult.isError()) {
                Result result = Result.getFailed(ContractErrorCode.DATA_ERROR);
                result.setMsg(programResult.getErrorMessage());
                return result;
            }
            long gasUsed = programResult.getGasUsed();
            // 预估1.5倍Gas
            gasUsed += gasUsed >> 1;
            Na imputedNa = Na.valueOf(gasUsed * price);
            // 总花费
            Na totalNa = imputedNa.add(value);
            CoinDataResult coinDataResult = accountLedgerService.getCoinData(senderBytes, totalNa, tx.size() + P2PKHScriptSig.DEFAULT_SERIALIZE_LENGTH, TransactionFeeCalculator.MIN_PRECE_PRE_1024_BYTES);
            if (!coinDataResult.isEnough()) {
                return Result.getFailed(ContractErrorCode.BALANCE_NOT_ENOUGH);
            }
            coinData.setFrom(coinDataResult.getCoinList());
            // 找零的UTXO
            if (coinDataResult.getChange() != null) {
                coinData.getTo().add(coinDataResult.getChange());
            }
            tx.setCoinData(coinData);

            // 组装txData
            CallContractData callContractData = new CallContractData();
            callContractData.setContractAddress(contractAddressBytes);
            callContractData.setSender(senderBytes);
            callContractData.setValue(value.getValue());
            callContractData.setPrice(price);
            callContractData.setNaLimit(naLimit.getValue());
            callContractData.setMethodName(methodName);
            callContractData.setMethodDesc(methodDesc);
            // 本次交易使用的Gas消耗
            callContractData.setTxGasUsed(imputedNa.getValue());
            if(args != null) {
                callContractData.setArgsCount((byte) args.length);
                callContractData.setArgs(args);
            }
            tx.setTxData(callContractData);

            tx.setHash(NulsDigestData.calcDigestData(tx.serialize()));
            // 交易签名
            P2PKHScriptSig sig = new P2PKHScriptSig();
            sig.setPublicKey(account.getPubKey());
            sig.setSignData(accountService.signData(tx.getHash().serialize(), account, password));
            tx.setScriptSig(sig.serialize());

            // 保存未确认交易到本地账本
            Result saveResult = accountLedgerService.verifyAndSaveUnconfirmedTransaction(tx);
            if (saveResult.isFailed()) {
                return saveResult;
            }

            // 广播
            Result sendResult = transactionService.broadcastTx(tx);
            if (sendResult.isFailed()) {
                // 失败则回滚
                accountLedgerService.rollbackTransaction(tx);
                return sendResult;
            }

            return Result.getSuccess().setData(tx.getHash().getDigestHex());
        } catch (IOException e) {
            Log.error(e);
            Result result = Result.getFailed(ContractErrorCode.CONTRACT_TX_CREATE_ERROR);
            result.setMsg(e.getMessage());
            return result;
        } catch (NulsException e) {
            Log.error(e);
            return Result.getFailed(e.getErrorCode());
        } catch (Exception e) {
            Log.error(e);
            Result result = Result.getFailed(ContractErrorCode.CONTRACT_TX_CREATE_ERROR);
            result.setMsg(e.getMessage());
            return result;
        }
    }

    /**
     * 创建删除智能合约的交易
     *
     * @param sender          交易创建者
     * @param contractAddress 合约地址
     * @param password        账户密码
     * @param remark          备注
     * @return
     */
    @Override
    public Result contractDeleteTx(String sender, String contractAddress,
                                   String password, String remark) {
        try {
            AssertUtil.canNotEmpty(sender, "the sender address can not be empty");
            AssertUtil.canNotEmpty(contractAddress, "the contractAddress can not be empty");

            Result<Account> accountResult = accountService.getAccount(sender);
            if (accountResult.isFailed()) {
                return accountResult;
            }

            Account account = accountResult.getData();
            // 验证账户密码
            if (accountService.isEncrypted(account).isSuccess() && account.isLocked()) {
                AssertUtil.canNotEmpty(password, "the password can not be empty");

                Result passwordResult = accountService.validPassword(account, password);
                if (passwordResult.isFailed()) {
                    return passwordResult;
                }
            }

            DeleteContractTransaction tx = new DeleteContractTransaction();
            if (StringUtils.isNotBlank(remark)) {
                try {
                    tx.setRemark(remark.getBytes(NulsConfig.DEFAULT_ENCODING));
                } catch (UnsupportedEncodingException e) {
                    Log.error(e);
                    throw new RuntimeException(e);
                }
            }
            tx.setTime(TimeService.currentTimeMillis());

            byte[] senderBytes = AddressTool.getAddress(sender);
            byte[] contractAddressBytes = AddressTool.getAddress(contractAddress);

            // 组装txData
            DeleteContractData deleteContractData = new DeleteContractData();
            deleteContractData.setContractAddress(contractAddressBytes);
            deleteContractData.setSender(senderBytes);

            tx.setTxData(deleteContractData);

            // 计算CoinData
            /*
             * 没有Gas消耗，在终止智能合约里
             */
            CoinData coinData = new CoinData();

            // 总花费 终止智能合约的交易手续费按普通交易计算手续费
            CoinDataResult coinDataResult = accountLedgerService.getCoinData(senderBytes, Na.ZERO, tx.size() + P2PKHScriptSig.DEFAULT_SERIALIZE_LENGTH, TransactionFeeCalculator.MIN_PRECE_PRE_1024_BYTES);
            if (!coinDataResult.isEnough()) {
                return Result.getFailed(ContractErrorCode.BALANCE_NOT_ENOUGH);
            }
            coinData.setFrom(coinDataResult.getCoinList());
            // 找零的UTXO
            if (coinDataResult.getChange() != null) {
                coinData.getTo().add(coinDataResult.getChange());
            }
            tx.setCoinData(coinData);
            tx.setHash(NulsDigestData.calcDigestData(tx.serialize()));
            // 交易签名
            P2PKHScriptSig sig = new P2PKHScriptSig();
            sig.setPublicKey(account.getPubKey());
            sig.setSignData(accountService.signData(tx.getHash().serialize(), account, password));
            tx.setScriptSig(sig.serialize());

            // 保存删除合约的交易到本地账本
            Result saveResult = accountLedgerService.verifyAndSaveUnconfirmedTransaction(tx);
            if (saveResult.isFailed()) {
                return saveResult;
            }
            // 广播交易
            Result sendResult = transactionService.broadcastTx(tx);
            if (sendResult.isFailed()) {
                // 失败则回滚
                accountLedgerService.rollbackTransaction(tx);
                return sendResult;
            }
            return Result.getSuccess().setData(tx.getHash().getDigestHex());
        } catch (IOException e) {
            Log.error(e);
            Result result = Result.getFailed(ContractErrorCode.CONTRACT_TX_CREATE_ERROR);
            result.setMsg(e.getMessage());
            return result;
        } catch (NulsException e) {
            Log.error(e);
            return Result.getFailed(e.getErrorCode());
        } catch (Exception e) {
            Log.error(e);
            Result result = Result.getFailed(ContractErrorCode.CONTRACT_TX_CREATE_ERROR);
            result.setMsg(e.getMessage());
            return result;
        }
    }

}
