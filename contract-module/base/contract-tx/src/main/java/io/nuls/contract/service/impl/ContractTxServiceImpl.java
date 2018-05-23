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
import io.nuls.contract.constant.ContractErrorCode;
import io.nuls.contract.entity.tx.CreateContractTransaction;
import io.nuls.contract.entity.txdata.CreateContractData;
import io.nuls.contract.helper.VMHelper;
import io.nuls.contract.service.ContractTxService;
import io.nuls.contract.vm.program.ProgramCreate;
import io.nuls.contract.vm.program.ProgramExecutor;
import io.nuls.contract.vm.program.ProgramResult;
import io.nuls.core.tools.crypto.Base58;
import io.nuls.core.tools.log.Log;
import io.nuls.core.tools.param.AssertUtil;
import io.nuls.core.tools.str.StringUtils;
import io.nuls.kernel.cfg.NulsConfig;
import io.nuls.kernel.exception.NulsException;
import io.nuls.kernel.func.TimeService;
import io.nuls.kernel.lite.annotation.Autowired;
import io.nuls.kernel.lite.annotation.Service;
import io.nuls.kernel.lite.core.bean.InitializingBean;
import io.nuls.kernel.model.CoinData;
import io.nuls.kernel.model.Na;
import io.nuls.kernel.model.NulsDigestData;
import io.nuls.kernel.model.Result;
import io.nuls.kernel.script.P2PKHScriptSig;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * @desription:
 * @author: PierreLuo
 * @date: 2018/5/22
 */
@Service
public class ContractTxServiceImpl implements ContractTxService, InitializingBean {

    @Autowired
    private AccountService accountService;
    @Autowired
    private AccountLedgerService accountLedgerService;

    private ProgramExecutor programExecutor;

    @Override
    public void afterPropertiesSet() throws NulsException {
        programExecutor = VMHelper.HELPER.getProgramExecutor();
    }

    /**
     * 创建包含智能合约的交易
     *
     * @param sender          交易创建者
     * @param value           交易附带的货币量
     * @param naLimit         最大Na消耗
     * @param price           执行合约单价
     * @param contractAddress 合约地址
     * @param contractCode    合约代码
     * @param args            参数列表
     * @param password        钱包密码
     * @param remark          备注
     * @return
     */
    @Override
    public Result contractCreateTx(String sender, Na value, Na naLimit, byte price,
                                   String contractAddress, byte[] contractCode, String[] args,
                                   String password, String remark) {
        try {
            AssertUtil.canNotEmpty(sender, "the sender address can not be empty");
            AssertUtil.canNotEmpty(contractAddress, "the contractAddress can not be empty");
            AssertUtil.canNotEmpty(contractCode, "the contractCode can not be empty");
            if (value == null) {
                value = Na.ZERO;
            }

            Result<Account> accountResult = accountService.getAccount(sender);
            if (accountResult.isFailed()) {
                return accountResult;
            }

            //TODO pierre 校验合约地址
            // coding

            Account account = accountResult.getData();
            // 验证钱包密码
            if (accountService.isEncrypted(account).isSuccess()) {
                AssertUtil.canNotEmpty(password, "the password can not be empty");

                Result passwordResult = accountService.validPassword(account, password);
                if (passwordResult.isFailed()) {
                    return passwordResult;
                }
            }

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

            byte[] senderBytes = Base58.decode(sender);
            byte[] contractAddressBytes = Base58.decode(contractAddress);

            // 组装txData
            CreateContractData createContractData = new CreateContractData();
            createContractData.setSender(senderBytes);
            createContractData.setContractAddress(contractAddressBytes);
            createContractData.setNaLimit(naLimit.getValue());
            createContractData.setPrice(price);
            createContractData.setCodeLen(contractCode.length);
            createContractData.setCode(contractCode);
            if(args != null) {
                createContractData.setArgsCount((byte) args.length);
                createContractData.setArgs(args);
            }
            tx.setTxData(createContractData);

            //计算CoinData
            /*
            智能合约计算手续费以消耗的Gas*Price为根据，然而创建交易时并不执行智能合约，
            所以此时交易的CoinData是不固定的，比实际要多，
            打包时执行智能合约，真实的手续费已算出，然而tx的手续费已扣除，
            多扣除的费用会以CoinBase交易还给Sender
             */
            CoinData coinData = new CoinData();
            // 当前区块高度//TODO pierre 不提交执行创建智能合约是否可以不用传入区块高度和状态根
            long blockHeight = -1;
            // 前一区块状态根//TODO pierre
            byte[] prevStateRoot = null;
            // 执行VM估算Gas消耗
            ProgramCreate programCreate = new ProgramCreate();
            programCreate.setContractAddress(createContractData.getContractAddress());
            programCreate.setSender(createContractData.getSender());
            programCreate.setPrice(createContractData.getPrice());
            programCreate.setNaLimit(createContractData.getNaLimit());
            programCreate.setNumber(blockHeight);
            programCreate.setContractCode(createContractData.getCode());
            if(args != null) {
                programCreate.setArgs(args);
            }
            ProgramExecutor track = programExecutor.begin(prevStateRoot);
            ProgramResult programResult = track.create(programCreate);
            if(programResult.isError()) {
                Result result = Result.getFailed(programResult.getErrorMessage());
                result.setErrorCode(ContractErrorCode.DATA_ERROR);
                return result;
            }
            long gasUsed = programResult.getGasUsed();
            // 预估1.5倍Gas
            gasUsed += gasUsed >> 1;
            Na imputedNa = Na.valueOf(gasUsed * price);
            CoinDataResult coinDataResult = accountLedgerService.getCoinData(senderBytes, imputedNa, tx.size() + P2PKHScriptSig.DEFAULT_SERIALIZE_LENGTH);
            if (!coinDataResult.isEnough()) {
                return Result.getFailed(ContractErrorCode.BALANCE_NOT_ENOUGH);
            }
            coinData.setFrom(coinDataResult.getCoinList());
            tx.setCoinData(coinData);



            tx.setHash(NulsDigestData.calcDigestData(tx.serialize()));
            // 交易签名
            P2PKHScriptSig sig = new P2PKHScriptSig();
            sig.setPublicKey(account.getPubKey());
            sig.setSignData(accountService.signData(tx.getHash().serialize(), account, password));
            tx.setScriptSig(sig.serialize());

            return Result.getSuccess().setData(tx.getHash().getDigestHex());
        } catch (IOException e) {
            Log.error(e);
            return Result.getFailed(e.getMessage());
        } catch (NulsException e) {
            Log.error(e);
            return Result.getFailed(e.getErrorCode());
        } catch (Exception e) {
            Log.error(e);
            return Result.getFailed(e.getMessage());
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
     * @param password        钱包密码
     * @param remark          备注
     * @return
     */
    @Override
    public Result contractCallTx(String sender, Na value, Na naLimit, byte price, String contractAddress,
                                 String methodName, String methodDesc, String[] args,
                                 String password, String remark) {
        //TODO pierre auto-generated method stub
        return null;
    }

    /**
     * 创建删除智能合约的交易
     *
     * @param sender          交易创建者
     * @param contractAddress 合约地址
     * @param password        钱包密码
     * @param remark          备注
     * @return
     */
    @Override
    public Result contractDeleteTx(String sender, String contractAddress,
                                   String password, String remark) {
        //TODO pierre auto-generated method stub
        return null;
    }

}
