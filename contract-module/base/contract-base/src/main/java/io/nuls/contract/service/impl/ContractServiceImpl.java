package io.nuls.contract.service.impl;

import io.nuls.account.ledger.model.TransactionInfo;
import io.nuls.account.ledger.service.AccountLedgerService;
import io.nuls.contract.constant.ContractErrorCode;
import io.nuls.contract.dto.ContractResult;
import io.nuls.contract.entity.txdata.CallContractData;
import io.nuls.contract.entity.txdata.CreateContractData;
import io.nuls.contract.entity.txdata.DeleteContractData;
import io.nuls.contract.helper.VMHelper;
import io.nuls.contract.ledger.service.ContractTransactionInfoService;
import io.nuls.contract.ledger.service.ContractUtxoService;
import io.nuls.contract.ledger.util.ContractLedgerUtil;
import io.nuls.contract.service.ContractService;
import io.nuls.contract.storage.po.TransactionInfoPo;
import io.nuls.contract.vm.program.ProgramCall;
import io.nuls.contract.vm.program.ProgramCreate;
import io.nuls.contract.vm.program.ProgramExecutor;
import io.nuls.contract.vm.program.ProgramResult;
import io.nuls.core.tools.log.Log;
import io.nuls.kernel.constant.KernelErrorCode;
import io.nuls.kernel.exception.NulsException;
import io.nuls.kernel.lite.annotation.Autowired;
import io.nuls.kernel.lite.annotation.Service;
import io.nuls.kernel.lite.core.bean.InitializingBean;
import io.nuls.kernel.model.Result;
import io.nuls.kernel.model.Transaction;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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
    private AccountLedgerService accountLedgerService;

    @Autowired
    private VMHelper vmHelper;

    private ProgramExecutor programExecutor;

    private Lock saveLock = new ReentrantLock();

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
    @Override
    public Result<ContractResult> createContract(long number, byte[] prevStateRoot, CreateContractData create) {
        if(number < 0) {
            return Result.getFailed(ContractErrorCode.PARAMETER_ERROR);
        }
        if(prevStateRoot == null) {
            return Result.getFailed(ContractErrorCode.NULL_PARAMETER);
        }
        try {
            ProgramCreate programCreate = new ProgramCreate();
            programCreate.setContractAddress(create.getContractAddress());
            programCreate.setSender(create.getSender());
            programCreate.setValue(BigInteger.valueOf(create.getValue()));
            programCreate.setPrice(create.getPrice());
            programCreate.setNaLimit(create.getNaLimit());
            programCreate.setNumber(number);
            programCreate.setContractCode(create.getCode());

            ProgramExecutor track = programExecutor.begin(prevStateRoot);
            ProgramResult programResult = track.create(programCreate);
            track.commit();

            if(programResult.isError()) {
                Result result = Result.getFailed(programResult.getErrorMessage());
                result.setErrorCode(ContractErrorCode.CONTRACT_EXECUTE_ERROR);
                return result;
            }
            // current state root
            byte[] stateRoot = track.getRoot();
            ContractResult contractResult = new ContractResult();
            // 返回已使用gas和状态根
            contractResult.setGasUsed(programResult.getGasUsed());
            contractResult.setStateRoot(stateRoot);

            Result<ContractResult> result = Result.getSuccess();
            result.setData(contractResult);
            return result;
        } catch (Exception e) {
            Log.error(e);
            return Result.getFailed(e.getMessage());
        }
    }

    /**
     * @param number        当前块编号
     * @param prevStateRoot 上一区块状态根
     * @param call          调用智能合约的参数
     * @return
     */
    @Override
    public Result<ContractResult> callContract(long number, byte[] prevStateRoot, CallContractData call) {
        if(number < 0) {
            return Result.getFailed(ContractErrorCode.PARAMETER_ERROR);
        }
        if(prevStateRoot == null) {
            return Result.getFailed(ContractErrorCode.NULL_PARAMETER);
        }
        try {
            ProgramCall programCall = new ProgramCall();
            programCall.setContractAddress(call.getContractAddress());
            programCall.setSender(call.getSender());
            programCall.setValue(BigInteger.valueOf(call.getValue()));
            programCall.setPrice(call.getPrice());
            programCall.setNaLimit(call.getNaLimit());
            programCall.setNumber(number);
            programCall.setMethodName(call.getMethodName());
            programCall.setMethodDesc(call.getMethodDesc());
            programCall.setArgs(call.getArgs());

            ProgramExecutor track = programExecutor.begin(prevStateRoot);
            ProgramResult programResult = track.call(programCall);
            track.commit();

            if(programResult.isError()) {
                Result result = Result.getFailed(programResult.getErrorMessage());
                result.setErrorCode(ContractErrorCode.CONTRACT_EXECUTE_ERROR);
                return result;
            }
            // current state root
            byte[] stateRoot = track.getRoot();
            ContractResult contractResult = new ContractResult();
            // 返回调用结果、已使用Gas和状态根
            contractResult.setResult(programResult.getResult());
            contractResult.setGasUsed(programResult.getGasUsed());
            contractResult.setStateRoot(stateRoot);

            Result<ContractResult> result = Result.getSuccess();
            result.setData(contractResult);
            return result;
        } catch (Exception e) {
            Log.error(e);
            return Result.getFailed(e.getMessage());
        }
    }

    /**
     * @param number        当前块编号
     * @param prevStateRoot 上一区块状态根
     * @param delete        删除智能合约的参数
     * @return
     */
    @Override
    public Result<ContractResult> deleteContract(long number, byte[] prevStateRoot, DeleteContractData delete) {
        if(number < 0) {
            return Result.getFailed(ContractErrorCode.PARAMETER_ERROR);
        }
        if(prevStateRoot == null) {
            return Result.getFailed(ContractErrorCode.NULL_PARAMETER);
        }
        try {

            ProgramExecutor track = programExecutor.begin(prevStateRoot);
            ProgramResult programResult = track.stop(delete.getContractAddress(), delete.getSender());
            track.commit();

            if(programResult.isError()) {
                Result result = Result.getFailed(programResult.getErrorMessage());
                result.setErrorCode(ContractErrorCode.CONTRACT_EXECUTE_ERROR);
                return result;
            }
            // current state root
            byte[] stateRoot = track.getRoot();
            ContractResult contractResult = new ContractResult();
            // 返回状态根
            contractResult.setStateRoot(stateRoot);

            Result<ContractResult> result = Result.getSuccess();
            result.setData(contractResult);
            return result;
        } catch (Exception e) {
            Log.error(e);
            return Result.getFailed(e.getMessage());
        }
    }

    @Override
    public Result<Object> getContractInfo(String address) {
        //TODO pierre auto-generated method stub
        return null;
    }

    @Override
    public Result<Object> getVmStatus() {
        //TODO pierre auto-generated method stub
        return null;
    }

    @Override
    public boolean isContractAddress(byte[] addressBytes) {
        return ContractLedgerUtil.isContractAddress(addressBytes);
    }

    @Override
    public Result<Integer> saveUnconfirmedTransaction(Transaction tx) {
        saveLock.lock();
        try{
            if (tx == null) {
                return Result.getFailed(KernelErrorCode.NULL_PARAMETER);
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

            for (int i = 0; i < addresses.size(); i++) {
                //TODO pierre 更新合约账户余额
                //balanceManager.refreshBalance(addresses.get(i));
            }
            return result;
        } finally {
            saveLock.unlock();
        }
    }

    @Override
    public Result<Integer> saveConfirmedTransaction(Transaction tx) {
        if (tx == null) {
            return Result.getFailed(KernelErrorCode.NULL_PARAMETER);
        }

        // 获取tx中是智能合约的地址列表
        List<byte[]> addresses = ContractLedgerUtil.getRelatedAddresses(tx);

        if (addresses == null || addresses.size() == 0) {
            return Result.getSuccess().setData(new Integer(0));
        }

        TransactionInfoPo txInfoPo = new TransactionInfoPo(tx);
        // 判断当前节点是否是创建当前交易的节点，如果不是则需要保存UTXO
        boolean isCreateTxNode = contractTransactionInfoService.isDbExistTransactionInfo(txInfoPo, addresses.get(0));

        txInfoPo.setStatus(TransactionInfo.CONFIRMED);

        Result result = contractTransactionInfoService.saveTransactionInfo(txInfoPo, addresses);
        if (result.isFailed()) {
            return result;
        }


        // 不是创建交易的节点则需要保存UTXO
        if (!isCreateTxNode) {
            result = contractUtxoService.saveUtxoForContractAddress(tx);
            if (result.isFailed()) {
                contractTransactionInfoService.deleteTransactionInfo(txInfoPo);
                return result;
            }
        }

        for (int i = 0; i < addresses.size(); i++) {
            //TODO pierre 更新合约账户余额
            //balanceManager.refreshBalance(addresses.get(i));
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
        //TODO pierre 更新合约账户余额
        //balanceManager.refreshBalance();
        return Result.getSuccess().setData(savedTxList.size());
    }

    @Override
    public Result<Integer> rollbackTransaction(Transaction tx) {
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
        result = contractUtxoService.deleteUtxoOfTransaction(tx);

        for (int i = 0; i < addresses.size(); i++) {
            //TODO pierre 更新合约账户余额
            //balanceManager.refreshBalance(addresses.get(i));
        }

        return result;
    }

    @Override
    public Result<Integer> rollbackTransaction(List<Transaction> txs) {
        Result result = Result.getSuccess().setData(txs.size());
        result = rollbackTransaction(txs, true);
        if (result.isSuccess()) {
            //TODO pierre 更新合约账户余额
            //balanceManager.refreshBalance();
        }
        return result;
    }

    private Result<Integer> rollbackTransaction(List<Transaction> txs, boolean isCheckContract) {
        List<Transaction> txListToRollback;
        if (isCheckContract) {
            // 过滤tx为包含合约地址的交易
            txListToRollback = filterRelatedTransaction(txs);
        } else {
            txListToRollback = txs;
        }
        // 回滚确认交易
        for (int i = txListToRollback.size() - 1; i >= 0; i--) {
            rollbackTransaction(txListToRollback.get(i));
        }
        // 还原成未确认交易
        for (int i = 0, length = txListToRollback.size(); i < length; i++) {
            saveUnconfirmedTransaction(txListToRollback.get(i));
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

}
