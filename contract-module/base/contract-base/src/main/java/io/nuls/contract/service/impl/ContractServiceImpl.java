/*
 * MIT License
 *
 * Copyright (c) 2017-2018 nuls.io
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */
package io.nuls.contract.service.impl;

import io.nuls.account.ledger.model.CoinDataResult;
import io.nuls.account.ledger.model.TransactionInfo;
import io.nuls.account.ledger.service.AccountLedgerService;
import io.nuls.contract.constant.ContractConstant;
import io.nuls.contract.constant.ContractErrorCode;
import io.nuls.contract.dto.ContractResult;
import io.nuls.contract.dto.ContractTransfer;
import io.nuls.contract.entity.tx.CallContractTransaction;
import io.nuls.contract.entity.tx.ContractTransferTransaction;
import io.nuls.contract.entity.tx.CreateContractTransaction;
import io.nuls.contract.entity.tx.DeleteContractTransaction;
import io.nuls.contract.entity.txdata.CallContractData;
import io.nuls.contract.entity.txdata.ContractTransferData;
import io.nuls.contract.entity.txdata.CreateContractData;
import io.nuls.contract.entity.txdata.DeleteContractData;
import io.nuls.contract.helper.VMHelper;
import io.nuls.contract.ledger.manager.ContractBalanceManager;
import io.nuls.contract.ledger.service.ContractTransactionInfoService;
import io.nuls.contract.ledger.service.ContractUtxoService;
import io.nuls.contract.ledger.util.ContractLedgerUtil;
import io.nuls.contract.service.ContractService;
import io.nuls.contract.service.ContractTxService;
import io.nuls.contract.storage.po.TransactionInfoPo;
import io.nuls.contract.storage.service.ContractAddressStorageService;
import io.nuls.contract.storage.service.ContractExecuteResultStorageService;
import io.nuls.contract.storage.service.ContractTransferTransactionStorageService;
import io.nuls.contract.util.ContractCoinComparator;
import io.nuls.contract.vm.program.*;
import io.nuls.core.tools.array.ArraysTool;
import io.nuls.core.tools.log.Log;
import io.nuls.core.tools.str.StringUtils;
import io.nuls.kernel.cfg.NulsConfig;
import io.nuls.kernel.constant.KernelErrorCode;
import io.nuls.kernel.exception.NulsException;
import io.nuls.kernel.lite.annotation.Autowired;
import io.nuls.kernel.lite.annotation.Service;
import io.nuls.kernel.lite.core.bean.InitializingBean;
import io.nuls.kernel.model.*;
import io.nuls.kernel.utils.AddressTool;
import io.nuls.kernel.utils.VarInt;
import io.nuls.kernel.validate.ValidateResult;
import io.nuls.ledger.constant.LedgerErrorCode;
import io.nuls.ledger.service.LedgerService;

import java.io.IOException;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static io.nuls.ledger.util.LedgerUtil.asBytes;
import static io.nuls.ledger.util.LedgerUtil.asString;

/**
 * @Desription:
 * @Author: PierreLuo
 * @Date:
 */
@Service
public class ContractServiceImpl implements ContractService, InitializingBean {

    @Autowired
    private ContractTransactionInfoService contractTransactionInfoService;

    @Autowired
    private ContractUtxoService contractUtxoService;

    @Autowired
    private ContractTransferTransactionStorageService contractTransferTransactionStorageService;

    @Autowired
    private ContractAddressStorageService contractAddressStorageService;

    @Autowired
    private ContractExecuteResultStorageService contractExecuteResultStorageService;

    @Autowired
    private ContractBalanceManager contractBalanceManager;

    @Autowired
    private AccountLedgerService accountLedgerService;

    @Autowired
    private ContractTxService contractTxService;

    @Autowired
    private LedgerService ledgerService;

    @Autowired
    private VMHelper vmHelper;

    private ProgramExecutor programExecutor;

    private Lock saveLock = new ReentrantLock();
    private Lock lock = new ReentrantLock();

    @Override
    public void afterPropertiesSet() throws NulsException {
        programExecutor = vmHelper.getProgramExecutor();
    }

    /**
     * @param number        当前块编号
     * @param prevStateRoot 上一区块状态根
     * @param create        创建智能合约的参数
     * @return
     */
    private Result<ContractResult> createContract(long number, byte[] prevStateRoot, CreateContractData create) {
        if(number < 0) {
            return Result.getFailed(ContractErrorCode.PARAMETER_ERROR);
        }
        if(prevStateRoot == null) {
            return Result.getFailed(ContractErrorCode.NULL_PARAMETER);
        }
        try {
            byte[] contractAddress = create.getContractAddress();
            ProgramCreate programCreate = new ProgramCreate();
            programCreate.setContractAddress(contractAddress);
            programCreate.setSender(create.getSender());
            programCreate.setValue(BigInteger.valueOf(create.getValue()));
            programCreate.setPrice(create.getPrice());
            programCreate.setGasLimit(create.getGasLimit());
            programCreate.setNumber(number);
            programCreate.setContractCode(create.getCode());

            ProgramExecutor track = programExecutor.begin(prevStateRoot);
            ProgramResult programResult = track.create(programCreate);
            track.commit();

            // current state root
            byte[] stateRoot = track.getRoot();
            ContractResult contractResult = new ContractResult();
            if(!programResult.isSuccess()) {
                Result<ContractResult> result = Result.getFailed(ContractErrorCode.CONTRACT_EXECUTE_ERROR);
                contractResult.setError(programResult.isError());
                contractResult.setRevert(programResult.isRevert());
                contractResult.setErrorMessage(programResult.getErrorMessage());
                contractResult.setStackTrace(programResult.getStackTrace());
                contractResult.setNonce(programResult.getNonce());
                contractResult.setGasUsed(programResult.getGasUsed());
                contractResult.setStateRoot(stateRoot);
                contractResult.setBalance(programResult.getBalance());
                contractResult.setContractAddress(contractAddress);
                contractResult.setRemark(ContractConstant.CREATE);
                result.setMsg(programResult.getErrorMessage());
                result.setData(contractResult);
                return result;
            }

            // 返回已使用gas、状态根、消息事件、合约转账
            contractResult.setError(false);
            contractResult.setRevert(false);
            contractResult.setStackTrace(programResult.getStackTrace());
            contractResult.setNonce(programResult.getNonce());
            contractResult.setGasUsed(programResult.getGasUsed());
            contractResult.setStateRoot(stateRoot);
            contractResult.setBalance(programResult.getBalance());
            contractResult.setEvents(programResult.getEvents());
            contractResult.setTransfers(generateContractTransfer(programResult.getTransfers()));
            contractResult.setContractAddress(contractAddress);
            contractResult.setRemark(ContractConstant.CREATE);

            Result<ContractResult> result = Result.getSuccess();
            result.setData(contractResult);
            return result;
        } catch (Exception e) {
            Log.error(e);
            Result result = Result.getFailed(ContractErrorCode.CONTRACT_EXECUTE_ERROR);
            result.setMsg(e.getMessage());
            return result;
        }
    }

    private List<ContractTransfer> generateContractTransfer(List<ProgramTransfer> transfers) {
        if(transfers == null || transfers.size() == 0) {
            return new ArrayList<>(0);
        }
        List<ContractTransfer> resultList = new ArrayList<>(transfers.size());
        ContractTransfer contractTransfer;
        for(ProgramTransfer transfer : transfers) {
            contractTransfer = new ContractTransfer();
            contractTransfer.setFrom(transfer.getFrom());
            contractTransfer.setTo(transfer.getTo());
            contractTransfer.setValue(Na.valueOf(transfer.getValue().longValue()));
            contractTransfer.setFee(Na.ZERO);
            resultList.add(contractTransfer);
        }
        return resultList;
    }

    /**
     * @param number        当前块编号
     * @param prevStateRoot 上一区块状态根
     * @param call          调用智能合约的参数
     * @return
     */
    private Result<ContractResult> callContract(long number, byte[] prevStateRoot, CallContractData call) {
        if(number < 0) {
            return Result.getFailed(ContractErrorCode.PARAMETER_ERROR);
        }
        if(prevStateRoot == null) {
            return Result.getFailed(ContractErrorCode.NULL_PARAMETER);
        }
        try {
            byte[] contractAddress = call.getContractAddress();
            ProgramCall programCall = new ProgramCall();
            programCall.setContractAddress(contractAddress);
            programCall.setSender(call.getSender());
            programCall.setValue(BigInteger.valueOf(call.getValue()));
            programCall.setPrice(call.getPrice());
            programCall.setGasLimit(call.getGasLimit());
            programCall.setNumber(number);
            programCall.setMethodName(call.getMethodName());
            programCall.setMethodDesc(call.getMethodDesc());
            programCall.setArgs(call.getArgs());

            ProgramExecutor track = programExecutor.begin(prevStateRoot);
            ProgramResult programResult = track.call(programCall);
            track.commit();

            // current state root
            byte[] stateRoot = track.getRoot();
            ContractResult contractResult = new ContractResult();
            if(!programResult.isSuccess()) {
                Result<ContractResult> result = Result.getFailed(ContractErrorCode.CONTRACT_EXECUTE_ERROR);
                contractResult.setError(programResult.isError());
                contractResult.setRevert(programResult.isRevert());
                contractResult.setErrorMessage(programResult.getErrorMessage());
                contractResult.setStackTrace(programResult.getStackTrace());
                contractResult.setNonce(programResult.getNonce());
                contractResult.setGasUsed(programResult.getGasUsed());
                contractResult.setStateRoot(stateRoot);
                contractResult.setBalance(programResult.getBalance());
                contractResult.setContractAddress(contractAddress);
                contractResult.setValue(programCall.getValue().longValue());
                contractResult.setRemark(ContractConstant.CALL);
                result.setMsg(programResult.getErrorMessage());
                result.setData(contractResult);
                return result;
            }

            // 返回调用结果、已使用Gas、状态根、消息事件、合约转账等
            contractResult.setError(false);
            contractResult.setRevert(false);
            contractResult.setStackTrace(programResult.getStackTrace());
            contractResult.setNonce(programResult.getNonce());
            contractResult.setResult(programResult.getResult());
            contractResult.setGasUsed(programResult.getGasUsed());
            contractResult.setStateRoot(stateRoot);
            contractResult.setBalance(programResult.getBalance());
            contractResult.setEvents(programResult.getEvents());
            contractResult.setTransfers(generateContractTransfer(programResult.getTransfers()));
            contractResult.setContractAddress(contractAddress);
            contractResult.setValue(programCall.getValue().longValue());
            contractResult.setRemark(ContractConstant.CALL);

            Result<ContractResult> result = Result.getSuccess();
            result.setData(contractResult);
            return result;
        } catch (Exception e) {
            Log.error(e);
            Result result = Result.getFailed(ContractErrorCode.CONTRACT_EXECUTE_ERROR);
            result.setMsg(e.getMessage());
            return result;
        }
    }

    /**
     * @param number        当前块编号
     * @param prevStateRoot 上一区块状态根
     * @param delete        删除智能合约的参数
     * @return
     */
    private Result<ContractResult> deleteContract(long number, byte[] prevStateRoot, DeleteContractData delete) {
        if(number < 0) {
            return Result.getFailed(ContractErrorCode.PARAMETER_ERROR);
        }
        if(prevStateRoot == null) {
            return Result.getFailed(ContractErrorCode.NULL_PARAMETER);
        }
        try {
            byte[] contractAddress = delete.getContractAddress();
            ProgramExecutor track = programExecutor.begin(prevStateRoot);
            ProgramResult programResult = track.stop(contractAddress, delete.getSender());
            track.commit();

            // current state root
            byte[] stateRoot = track.getRoot();
            ContractResult contractResult = new ContractResult();
            if(!programResult.isSuccess()) {
                Result<ContractResult> result = Result.getFailed(ContractErrorCode.CONTRACT_EXECUTE_ERROR);
                contractResult.setError(programResult.isError());
                contractResult.setRevert(programResult.isRevert());
                contractResult.setErrorMessage(programResult.getErrorMessage());
                contractResult.setStackTrace(programResult.getStackTrace());
                contractResult.setNonce(programResult.getNonce());
                contractResult.setGasUsed(programResult.getGasUsed());
                contractResult.setStateRoot(stateRoot);
                contractResult.setBalance(programResult.getBalance());
                contractResult.setContractAddress(contractAddress);
                contractResult.setRemark(ContractConstant.DELETE);
                result.setMsg(programResult.getErrorMessage());
                result.setData(contractResult);
                return result;
            }

            // 返回状态根
            contractResult.setError(false);
            contractResult.setRevert(false);
            contractResult.setStackTrace(programResult.getStackTrace());
            contractResult.setNonce(programResult.getNonce());
            contractResult.setStateRoot(stateRoot);
            contractResult.setBalance(programResult.getBalance());
            contractResult.setContractAddress(contractAddress);
            contractResult.setRemark(ContractConstant.DELETE);

            Result<ContractResult> result = Result.getSuccess();
            result.setData(contractResult);
            return result;
        } catch (Exception e) {
            Log.error(e);
            Result result = Result.getFailed(ContractErrorCode.CONTRACT_EXECUTE_ERROR);
            result.setMsg(e.getMessage());
            return result;
        }
    }

    @Override
    public boolean isContractAddress(byte[] addressBytes) {
        return ContractLedgerUtil.isContractAddress(addressBytes);
    }

    @Deprecated
    public Result<Integer> saveUnconfirmedTransaction(Transaction tx) {
        saveLock.lock();
        try{
            if (tx == null) {
                return Result.getFailed(ContractErrorCode.NULL_PARAMETER);
            }

            // 合约账本不处理非合约相关交易
            if(!ContractLedgerUtil.isRelatedTransaction(tx)) {
                return Result.getSuccess().setData(new Integer(0));
            }

            // 获取tx中是智能合约的地址列表
            List<byte[]> addresses = ContractLedgerUtil.getRelatedAddresses(tx);

            if (addresses == null || addresses.size() == 0) {
                return Result.getSuccess().setData(new Integer(0));
            }

            TransactionInfoPo txInfoPo = new TransactionInfoPo(tx);
            txInfoPo.setStatus(TransactionInfo.UNCONFIRMED);

            Result result = contractTransactionInfoService.saveTransactionInfo(txInfoPo, addresses);
            if (result.isFailed()) {
                return result;
            }

            result = contractUtxoService.saveUtxoForContractAddress(tx);
            if (result.isFailed()) {
                contractTransactionInfoService.deleteTransactionInfo(txInfoPo);
                return result;
            }

            return result;
        } finally {
            saveLock.unlock();
        }
    }

    private Result<Integer> saveConfirmedTransaction(Transaction tx) {
        if (tx == null) {
            return Result.getFailed(ContractErrorCode.NULL_PARAMETER);
        }

        // 获取tx中是智能合约地址列表
        List<byte[]> addresses = ContractLedgerUtil.getRelatedAddresses(tx);

        // 合约账本不处理非合约相关交易
        if (addresses == null || addresses.size() == 0) {
            return Result.getSuccess().setData(new Integer(0));
        }

        TransactionInfoPo txInfoPo = new TransactionInfoPo(tx);
        txInfoPo.setStatus(TransactionInfo.CONFIRMED);

        Result result = contractTransactionInfoService.saveTransactionInfo(txInfoPo, addresses);
        if (result.isFailed()) {
            return result;
        }

        // 保存UTXO
        result = contractUtxoService.saveUtxoForContractAddress(tx);
        if (result.isFailed()) {
            contractTransactionInfoService.deleteTransactionInfo(txInfoPo);
            return result;
        }

        // 合约转账交易需要保存交易信息到合约账本中
        if(tx instanceof ContractTransferTransaction) {
            result = contractTransferTransactionStorageService.saveContractTransferTx(tx.getHash(), tx);
            if (result.isFailed()) {
                contractUtxoService.deleteUtxoOfTransaction(tx);
                contractTransactionInfoService.deleteTransactionInfo(txInfoPo);
                return result;
            }
        }
        result.setData(new Integer(1));
        return result;
    }

    @Override
    public Result<Integer> saveConfirmedTransactionList(List<Transaction> txs) {
        List<Transaction> savedTxList = new ArrayList<>();
        Result result;
        for (int i = 0; i < txs.size(); i++) {
            result = saveConfirmedTransaction(txs.get(i));
            if (result.isSuccess()) {
                if(result.getData() != null && (int) result.getData() == 1) {
                    savedTxList.add(txs.get(i));
                }
            } else {
                rollbackTransaction(savedTxList, false);
                return result;
            }
        }
        return Result.getSuccess().setData(savedTxList.size());
    }

    private Result<Integer> rollbackTransaction(Transaction tx) {
        // 判断交易是否与合约相关
        if (!ContractLedgerUtil.isRelatedTransaction(tx)) {
            return Result.getSuccess().setData(new Integer(0));
        }

        // 获取tx中是智能合约地址的地址列表
        List<byte[]> addresses = ContractLedgerUtil.getRelatedAddresses(tx);

        if (addresses == null || addresses.size() == 0) {
            return Result.getSuccess().setData(new Integer(0));
        }

        TransactionInfoPo txInfoPo = new TransactionInfoPo(tx);
        Result result = contractTransactionInfoService.deleteTransactionInfo(txInfoPo);

        if (result.isFailed()) {
            return result;
        }

        // 合约转账交易需要删除交易
        if(tx instanceof ContractTransferTransaction) {
            result = contractTransferTransactionStorageService.deleteContractTransferTx(tx.getHash());
            if (result.isFailed()) {
                txInfoPo.setStatus(TransactionInfo.CONFIRMED);
                contractTransactionInfoService.saveTransactionInfo(txInfoPo, addresses);
                return result;
            }
        }
        // 回滚UTXO并刷新余额
        result = contractUtxoService.deleteUtxoOfTransaction(tx);

        return result;
    }

    private Result<Integer> rollbackTransactionList(List<Transaction> txs) {
        Result result = Result.getSuccess().setData(txs.size());
        result = rollbackTransaction(txs, true);
        return result;
    }

    @Override
    public Result saveContractExecuteResult(NulsDigestData hash, ContractResult result) {
        if (hash == null || result == null) {
            return Result.getFailed(ContractErrorCode.NULL_PARAMETER);
        }
        return contractExecuteResultStorageService.saveContractExecuteResult(hash, result);
    }

    @Override
    public Result deleteContractExecuteResult(NulsDigestData hash) {
        if (hash == null) {
            return Result.getFailed(ContractErrorCode.NULL_PARAMETER);
        }
        return contractExecuteResultStorageService.deleteContractExecuteResult(hash);
    }

    @Override
    public boolean isExistContractExecuteResult(NulsDigestData hash) {
        if (hash == null) {
            return false;
        }
        return contractExecuteResultStorageService.isExistContractExecuteResult(hash);
    }

    @Override
    public ContractResult getContractExecuteResult(NulsDigestData hash) {
        if (hash == null) {
            return null;
        }
        return contractExecuteResultStorageService.getContractExecuteResult(hash);
    }

    private Result<Integer> rollbackTransaction(List<Transaction> txs, boolean isCheckContract) {
        List<Transaction> txListToRollback;
        if (isCheckContract) {
            // 过滤掉与合约地址无关的交易，保留与合约相关的交易
            txListToRollback = filterRelatedTransaction(txs);
        } else {
            txListToRollback = txs;
        }
        // 回滚确认交易
        for (int i = txListToRollback.size() - 1; i >= 0; i--) {
            rollbackTransaction(txListToRollback.get(i));
        }
        return Result.getSuccess().setData(new Integer(txListToRollback.size()));
    }

    private List<Transaction> filterRelatedTransaction(List<Transaction> txs) {
        List<Transaction> resultTxs = new ArrayList<>();
        if (txs == null || txs.size() == 0) {
            return resultTxs;
        }

        Transaction tmpTx;
        for (int i = 0; i < txs.size(); i++) {
            tmpTx = txs.get(i);
            if (ContractLedgerUtil.isRelatedTransaction(tmpTx)) {
                resultTxs.add(tmpTx);
            }
        }
        return resultTxs;
    }

    private Result<ContractTransferTransaction> transfer(byte[] from, byte[] to, Na values, Na fee, boolean isSendBack, long blockTime,
                                                        Map<String, Coin> toMaps,
                                                        Map<String, Coin> contractUsedCoinMap) {
        try {
            if(!ContractLedgerUtil.isContractAddress(from)) {
                return Result.getFailed(ContractErrorCode.CONTRACT_ADDRESS_NOT_EXIST);
            }
            ContractTransferTransaction tx = new ContractTransferTransaction();
            tx.setTime(blockTime);

            CoinData coinData = new CoinData();
            Coin toCoin = new Coin(to, values.subtract(fee));
            coinData.getTo().add(toCoin);

            // 加入toMaps、contractUsedCoinMap组装UTXO
            // 组装时不操作toMaps, 用contractUsedCoinList(是否使用Map)来检查UTXO是否已经被使用，如果已使用则跳过
            CoinDataResult coinDataResult = getContractSpecialTransferCoinData(from, values, toMaps, contractUsedCoinMap);
            if (!coinDataResult.isEnough()) {
                return Result.getFailed(LedgerErrorCode.BALANCE_NOT_ENOUGH);
            }
            coinData.setFrom(coinDataResult.getCoinList());
            if (coinDataResult.getChange() != null) {
                coinData.getTo().add(coinDataResult.getChange());
            }
            tx.setCoinData(coinData);
            byte successByte;
            byte[] remark = null;
            if(isSendBack) {
                successByte = 0;
                remark = ContractConstant.SEND_BACK_REMARK.getBytes(NulsConfig.DEFAULT_ENCODING);
            } else {
                successByte = 1;
            }
            tx.setRemark(remark);
            tx.setTxData(new ContractTransferData(successByte));
            tx.setHash(NulsDigestData.calcDigestData(tx.serializeForHash()));

            // 合约转账交易不需要签名
            return Result.getSuccess().setData(tx);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * @param address
     * @param amount
     * @return
     * @throws NulsException
     */
    public CoinDataResult getContractSpecialTransferCoinData(byte[] address, Na amount, Map<String, Coin> toMaps, Map<String, Coin> contractUsedCoinMap) {
        lock.lock();
        try {
            CoinDataResult coinDataResult = new CoinDataResult();
            List<Coin> coinList = contractBalanceManager.getCoinListByAddress(address);
            //pierre add contract coin key
            Set<Map.Entry<String, Coin>> entries = toMaps.entrySet();
            Coin toCoin;
            Coin cloneCoin;
            String key;
            for(Map.Entry<String, Coin> entry : entries) {
                key = entry.getKey();
                toCoin = entry.getValue();
                if (Arrays.equals(toCoin.getOwner(), address)) {
                    cloneCoin = new Coin(asBytes(key), toCoin.getNa(), toCoin.getLockTime());
                    cloneCoin.setFrom(toCoin.getFrom());
                    cloneCoin.setKey(key);
                    coinList.add(cloneCoin);
                }
            }

            if (coinList.isEmpty()) {
                coinDataResult.setEnough(false);
                return coinDataResult;
            }

            // 将所有余额从小到大排序
            Collections.sort(coinList, ContractCoinComparator.getInstance());

            boolean enough = false;
            List<Coin> coins = new ArrayList<>();
            Na values = Na.ZERO;
            Coin coin;
            // 将所有余额从小到大排序后，累计未花费的余额
            for (int i = 0, length = coinList.size(); i < length; i++) {
                coin = coinList.get(i);
                if (!coin.usable()) {
                    continue;
                }
                if (coin.getNa().equals(Na.ZERO)) {
                    continue;
                }
                // for contract, 使用过的UTXO不能再使用
                if(StringUtils.isNotBlank(coin.getKey())) {
                    if(contractUsedCoinMap.containsKey(coin.getKey())) {
                        continue;
                    }
                    contractUsedCoinMap.put(coin.getKey(), coin);
                }
                coins.add(coin);

                // 每次累加一条未花费余额
                values = values.add(coin.getNa());
                if (values.isGreaterOrEquals(amount)) {
                    // 余额足够后，需要判断是否找零
                    Na change = values.subtract(amount);
                    if (change.isGreaterThan(Na.ZERO)) {
                        Coin changeCoin = new Coin();
                        changeCoin.setOwner(address);
                        changeCoin.setNa(change);
                        coinDataResult.setChange(changeCoin);
                    }
                    enough = true;
                    coinDataResult.setEnough(true);
                    coinDataResult.setFee(Na.ZERO);
                    coinDataResult.setCoinList(coins);
                    break;
                }
            }
            if (!enough) {
                coinDataResult.setEnough(false);
                return coinDataResult;
            }
            return coinDataResult;
        } finally {
            lock.unlock();
        }
    }


    @Override
    public Result<ContractResult> invokeContract(Transaction tx, long height, byte[] stateRoot) {
        if(tx == null || height < 0 || stateRoot == null) {
            return Result.getFailed(KernelErrorCode.PARAMETER_ERROR);
        }
        int txType = tx.getType();
        if(ContractLedgerUtil.isContractTransaction(tx)) {
            // 打包、验证区块，合约只执行一次
            ContractResult contractExecutedResult = getContractExecuteResult(tx.getHash());
            if(contractExecutedResult != null) {
                if(contractExecutedResult.isSuccess()) {
                    return Result.getSuccess().setData(contractExecutedResult);
                } else {
                    return Result.getFailed().setData(contractExecutedResult);
                }
            }
        }

        if (txType == ContractConstant.TX_TYPE_CREATE_CONTRACT) {
            CreateContractTransaction createContractTransaction = (CreateContractTransaction) tx;
            CreateContractData createContractData = createContractTransaction.getTxData();
            Result<ContractResult> result = createContract(height, stateRoot, createContractData);
            return result;
        } else if(txType == ContractConstant.TX_TYPE_CALL_CONTRACT) {
            CallContractTransaction callContractTransaction = (CallContractTransaction) tx;
            CallContractData callContractData = callContractTransaction.getTxData();
            Result<ContractResult> result = callContract(height, stateRoot, callContractData);
            // 刷新临时余额
            if(result.isSuccess()) {
                // 增加转入
                long value = callContractData.getValue();
                byte[] contractAddress = callContractData.getContractAddress();
                if(value > 0) {
                    contractBalanceManager.addTempBalance(contractAddress, value);
                }
                // 扣除转出
                ContractResult contractResult = result.getData();
                List<ContractTransfer> transfers = contractResult.getTransfers();
                if(transfers != null && transfers.size() > 0) {
                    Na outAmount = Na.ZERO;
                    for(ContractTransfer transfer : transfers) {
                        outAmount = outAmount.add(transfer.getValue());
                    }
                    contractBalanceManager.minusTempBalance(contractAddress, outAmount.getValue());
                }
            } else {
                // 合约调用失败，把需要退还的UTXO记录到结果对象中
                result.getData().setValue(callContractData.getValue());
            }
            return result;
        } else if(txType == ContractConstant.TX_TYPE_DELETE_CONTRACT) {
            DeleteContractTransaction deleteContractTransaction = (DeleteContractTransaction) tx;
            DeleteContractData deleteContractData = deleteContractTransaction.getTxData();
            Result<ContractResult> result = deleteContract(height, stateRoot, deleteContractData);
            return result;
        } else {
            return Result.getSuccess();
        }
    }

    @Override
    public void rollbackContractTempBalance(Transaction tx, ContractResult contractResult) {
        if(tx != null && tx.getType() == ContractConstant.TX_TYPE_CALL_CONTRACT) {
            CallContractTransaction callContractTransaction = (CallContractTransaction) tx;
            CallContractData callContractData = callContractTransaction.getTxData();
            byte[] contractAddress = callContractData.getContractAddress();
            // 增加转出
            List<ContractTransfer> transfers = contractResult.getTransfers();
            if(transfers != null && transfers.size() > 0) {
                Na outAmount = Na.ZERO;
                for(ContractTransfer transfer : transfers) {
                    outAmount = outAmount.add(transfer.getValue());
                }
                contractBalanceManager.minusTempBalance(contractAddress, outAmount.getValue());
            }
            // 扣除转入
            long value = callContractData.getValue();
            if(value > 0) {
                contractBalanceManager.addTempBalance(contractAddress, value);
            }
        }
    }

    @Override
    public void createContractTempBalance() {
        contractBalanceManager.createTempBalanceMap();
    }

    @Override
    public void removeContractTempBalance() {
        contractBalanceManager.removeTempBalanceMap();
    }

    @Override
    public ValidateResult verifyContractTransferCoinData(ContractTransferTransaction contractTransferTx, Map<String, Coin> toMaps, Set<String> fromSet) {
        Transaction repeatTx = ledgerService.getTx(contractTransferTx.getHash());

        if (repeatTx != null) {
            return ValidateResult.getSuccessResult();
        }
        return ledgerService.verifyCoinData(contractTransferTx, toMaps, fromSet);
    }

    @Override
    public void rollbackContractTransferTxs(Map<String, ContractTransferTransaction> successContractTransferTxs, Map<String, Coin> toMaps, Set<String> fromSet, Map<String, Coin> contractUsedCoinMap) {
        if(successContractTransferTxs != null && successContractTransferTxs.size() > 0) {
            Collection<ContractTransferTransaction> values = successContractTransferTxs.values();
            for(Transaction tx : values) {
                rollbackVerifyData(tx, toMaps, fromSet);
                rollbackTransaction(tx);
            }
        }
    }

    @Override
    public void rollbackContractTransferTx(ContractTransferTransaction tx, Map<String, Coin> toMaps, Set<String> fromSet, Map<String, Coin> contractUsedCoinMap) {
        if(tx != null) {
            rollbackVerifyData(tx, toMaps, fromSet);
            rollbackTransaction(tx);
        }
    }

    @Override
    public Result contractCallTx(byte[] sender, Na value, Long gasLimit, Long price, byte[] contractAddress, String methodName, String methodDesc, String[] args, String password, String remark) {
        return contractTxService.contractCallTx(AddressTool.getStringAddressByBytes(sender), value, gasLimit, price, AddressTool.getStringAddressByBytes(contractAddress), methodName, methodDesc, args, password, remark);
    }

    @Override
    public Result<ContractTransferTransaction> createContractTransferTx(ContractTransfer transfer, long blockTime, Map<String, Coin> toMaps, Map<String, Coin> contractUsedCoinMap) {
        Result<ContractTransferTransaction> result = null;
        result = transfer(transfer.getFrom(), transfer.getTo(), transfer.getValue(), transfer.getFee(), transfer.isSendBack(), blockTime, toMaps, contractUsedCoinMap);
        if(result.isSuccess()) {
            result.getData().setTransfer(transfer);
        } else {
            Log.error("contract transfer failed. Reason: " + result);
        }
        return result;
    }

    @Override
    public void rollbackVerifyData(Transaction tx, Map<String, Coin> toMaps, Set<String> fromSet) {
        if (tx == null || tx.getCoinData() == null || toMaps == null || fromSet == null) {
            return;
        }
        try {
            CoinData coinData = tx.getCoinData();
            List<Coin> froms = coinData.getFrom();
            for (Coin from : froms) {
                fromSet.remove(asString(from.getOwner()));
            }
            List<Coin> tos = coinData.getTo();
            byte[] txBytes = new byte[0];

            txBytes = tx.getHash().serialize();
            Coin to;
            for (int i = 0, toLength = tos.size(); i < toLength; i++) {
                to = tos.get(i);
                toMaps.remove(asString(ArraysTool.concatenate(txBytes, new VarInt(i).encode())));
            }
        } catch (IOException e) {
            Log.error(e);
            return;
        }
    }
}
