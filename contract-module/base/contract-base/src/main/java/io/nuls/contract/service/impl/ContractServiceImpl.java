package io.nuls.contract.service.impl;

import io.nuls.contract.dto.ContractResult;
import io.nuls.contract.entity.form.*;
import io.nuls.contract.entity.tx.CallContractTransaction;
import io.nuls.contract.entity.tx.CreateContractTransaction;
import io.nuls.contract.entity.tx.DeleteContractTransaction;
import io.nuls.contract.entity.txdata.CallContractData;
import io.nuls.contract.entity.txdata.CreateContractData;
import io.nuls.contract.entity.txdata.DeleteContractData;
import io.nuls.contract.service.ContractService;
import io.nuls.core.tools.log.Log;
import io.nuls.kernel.cfg.NulsConfig;
import io.nuls.kernel.exception.NulsException;
import io.nuls.kernel.lite.annotation.Service;
import io.nuls.kernel.model.Na;
import io.nuls.kernel.model.Result;
import io.nuls.kernel.validate.ValidateResult;

/**
 * @Desription:
 * @Author: PierreLuo
 * @Date:
 */
@Service
public class ContractServiceImpl implements ContractService {

    private Na getTotalNa(ContractBase contract) {
        return null;
    }

    private byte[] long48ToBytes(long x) {
        byte[] bb = new byte[6];
        bb[5] = (byte) (0xFF & x >> 40);
        bb[4] = (byte) (0xFF & x >> 32);
        bb[3] = (byte) (0xFF & x >> 24);
        bb[2] = (byte) (0xFF & x >> 16);
        bb[1] = (byte) (0xFF & x >> 8);
        bb[0] = (byte) (0xFF & x >> 0);
        return bb;
    }

    /**
     * @param create
     * @return
     */
    @Override
    public Result<ContractResult> createContract(ContractCreate create) {
        CreateContractTransaction tx = null;
        Result<ContractResult> result = null;
        try {
            //TODO 调用智能合约
            byte[] contractAddress = null;
            // 调用智能合约，返回智能合约地址, 使用的Na, ABI
            ContractResult contractResult = new ContractResult();
            contractResult.setContractAddress(new String(contractAddress, NulsConfig.DEFAULT_ENCODING));
            long naUsed = contractResult.getNaUsed();

            // CoinTransferData(OperationType type, Na totalNa, String from, Na fee)
            //TODO 待定项: 交易金额-totalNa

            //TODO validate
            ValidateResult validateResult = tx.verify();
            if (validateResult.isFailed()) {
                throw new NulsException(validateResult.getErrorCode());
            }

            // 赋值txData
            CreateContractData txData = new CreateContractData();
            txData.setAddress(create.getSender().getBytes(NulsConfig.DEFAULT_ENCODING));
            txData.setContractAddress(contractAddress);
            txData.setCodeLen(create.getContractCode().length);
            txData.setCode(create.getContractCode());
            txData.setNaLimit(long48ToBytes(create.getNaLimit())); // 问题, long转成6字节数组
            txData.setPrice((byte) create.getPrice());
            txData.setArgsCount((byte) create.getArgs().length);
            txData.setArgs(create.getArgs());

            //TODO saveLocal

            //TODO brodcaster

            //TODO 返回内容
            result = Result.getSuccess();
        } catch (Exception e) {
            Log.error(e);
            result = Result.getFailed(e.getMessage());
        }
        return result;
    }

    /**
     * @param call
     * @return
     */
    @Override
    public Result<ContractResult> callContract(ContractCall call) {
        CallContractTransaction tx = null;
        Result<ContractResult> result = null;
        try {
            //TODO 调用智能合约
            byte[] contractAddress = String.valueOf(call.getArgs()[0]).getBytes(NulsConfig.DEFAULT_ENCODING);
            // 调用智能合约，返回智能合约地址, 使用的Na, ABI
            ContractResult contractResult = new ContractResult();
            contractResult.setContractAddress(new String(contractAddress, NulsConfig.DEFAULT_ENCODING));
            long naUsed = contractResult.getNaUsed();

            // CoinTransferData(OperationType type, Na totalNa, String from, String to, Na fee)
            //TODO 待定项: 交易金额-totalNa, 交易对象-to, 交易手续费-fee

            // 赋值txData
            CallContractData txData = new CallContractData();
            txData.setAddress(call.getSender().getBytes(NulsConfig.DEFAULT_ENCODING));
            txData.setContractAddress(contractAddress);
            txData.setNaLimit(long48ToBytes(call.getNaLimit())); // 问题
            txData.setPrice((byte) call.getPrice());
            txData.setArgsCount((byte) call.getArgs().length);
            txData.setArgs(call.getArgs()); // 问题


            //TODO validate
            ValidateResult validateResult = tx.verify();
            if (validateResult.isFailed()) {
                throw new NulsException(validateResult.getErrorCode());
            }

            //TODO saveLocal
            tx.setTxData(txData);

            //TODO brodcaster

            //TODO 返回内容
            result = Result.getSuccess();
        } catch (Exception e) {
            Log.error(e);
            result = Result.getFailed(e.getMessage());
        }
        return result;
    }

    /**
     * @param delete
     * @return
     */
    @Override
    public Result<ContractResult> deleteContract(ContractDelete delete) {
        DeleteContractTransaction tx = null;
        Result<ContractResult> result = null;
        try {
            //TODO 调用智能合约
            byte[] contractAddress = String.valueOf(delete.getArgs()[0]).getBytes(NulsConfig.DEFAULT_ENCODING);
            // 调用智能合约，返回智能合约地址, 使用的Na, ABI
            ContractResult contractResult = new ContractResult();
            contractResult.setContractAddress(new String(contractAddress, NulsConfig.DEFAULT_ENCODING));
            long naUsed = contractResult.getNaUsed();

            // CoinTransferData(OperationType type, Na totalNa, String from, String to, Na fee)
            //TODO 待定项: 交易金额-totalNa, 交易对象-to, 交易手续费-fee

            // 赋值txData
            DeleteContractData txData = new DeleteContractData();
            txData.setAddress(delete.getSender().getBytes(NulsConfig.DEFAULT_ENCODING));
            txData.setContractAddress(contractAddress);
            txData.setNaLimit(long48ToBytes(delete.getNaLimit())); // 问题
            txData.setPrice((byte) delete.getPrice());
            txData.setArgsCount((byte) delete.getArgs().length);
            txData.setArgs(delete.getArgs()); // 问题


            //TODO validate
            ValidateResult validateResult = tx.verify();
            if (validateResult.isFailed()) {
                throw new NulsException(validateResult.getErrorCode());
            }

            //TODO saveLocal
            tx.setTxData(txData);

            //TODO brodcaster

            //TODO 返回内容
            result = Result.getSuccess();
        } catch (Exception e) {
            Log.error(e);
            result = Result.getFailed(e.getMessage());
        }
        return result;
    }

    /**
     * @param update
     * @return
     */
    @Override
    public Result<ContractResult> updateContract(ContractUpdate update) {
        //TODO 更新合约
        return null;
    }

    @Override
    public Result<Object> getContractInfo(String address) {
        //TODO auto-generated method stub
        return null;
    }

    @Override
    public Result<Object> getVmStatus() {
        //TODO auto-generated method stub
        return null;
    }

}
