package io.nuls.contract.service.impl;

import io.nuls.contract.constant.ContractErrorCode;
import io.nuls.contract.dto.ContractResult;
import io.nuls.contract.entity.txdata.CallContractData;
import io.nuls.contract.entity.txdata.CreateContractData;
import io.nuls.contract.entity.txdata.DeleteContractData;
import io.nuls.contract.helper.VMHelper;
import io.nuls.contract.service.ContractService;
import io.nuls.contract.vm.program.ProgramCall;
import io.nuls.contract.vm.program.ProgramCreate;
import io.nuls.contract.vm.program.ProgramExecutor;
import io.nuls.contract.vm.program.ProgramResult;
import io.nuls.core.tools.log.Log;
import io.nuls.kernel.exception.NulsException;
import io.nuls.kernel.lite.annotation.Service;
import io.nuls.kernel.lite.core.bean.InitializingBean;
import io.nuls.kernel.model.Result;

import java.math.BigInteger;

/**
 * @Desription:
 * @Author: PierreLuo
 * @Date:
 */
@Service
public class ContractServiceImpl implements ContractService, InitializingBean {

    private ProgramExecutor programExecutor;

    @Override
    public void afterPropertiesSet() throws NulsException {
        programExecutor = VMHelper.HELPER.getProgramExecutor();
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
            programCreate.setSender(create.getAddress());
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
            programCall.setSender(call.getAddress());
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
        //TODO auto-generated method stub
        return null;
    }

    @Override
    public Result<Object> getVmStatus() {
        //TODO auto-generated method stub
        return null;
    }

}
