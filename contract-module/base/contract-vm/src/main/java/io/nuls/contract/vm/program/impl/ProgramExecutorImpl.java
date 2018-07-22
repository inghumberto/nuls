package io.nuls.contract.vm.program.impl;

import io.nuls.contract.util.VMContext;
import io.nuls.contract.vm.ObjectRef;
import io.nuls.contract.vm.Result;
import io.nuls.contract.vm.VM;
import io.nuls.contract.vm.VMFactory;
import io.nuls.contract.vm.code.ClassCode;
import io.nuls.contract.vm.code.ClassCodeLoader;
import io.nuls.contract.vm.code.MethodCode;
import io.nuls.contract.vm.program.*;
import io.nuls.contract.vm.util.Validators;
import io.nuls.db.service.DBService;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.ethereum.core.AccountState;
import org.ethereum.core.Repository;
import org.ethereum.db.RepositoryRoot;
import org.ethereum.util.FastByteComparisons;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.ConstraintViolation;
import java.math.BigInteger;
import java.util.*;

public class ProgramExecutorImpl implements ProgramExecutor {

    private static final Logger log = LoggerFactory.getLogger(ProgramExecutorImpl.class);

    private VMContext vmContext;

    private KeyValueSource keyValueSource;

    private Repository repository;

    private byte[] prevStateRoot;

    private boolean revert;

    private boolean committed;

    private boolean getter;

    public ProgramExecutorImpl(VMContext vmContext, DBService dbService) {
        this(vmContext, new KeyValueSource(dbService), null, null);
    }

    private ProgramExecutorImpl(VMContext vmContext, KeyValueSource keyValueSource, Repository repository, byte[] prevStateRoot) {
        this.vmContext = vmContext;
        this.keyValueSource = keyValueSource;
        this.repository = repository;
        this.prevStateRoot = prevStateRoot;
    }

    @Override
    public ProgramExecutor begin(byte[] prevStateRoot) {
        Repository repository = new RepositoryRoot(keyValueSource, prevStateRoot);
        return new ProgramExecutorImpl(vmContext, keyValueSource, repository, prevStateRoot);
    }

    @Override
    public void commit() {
        if (!revert && !getter) {
            repository.commit();
            committed = true;
        }
    }

    @Override
    public byte[] getRoot() {
        if (committed) {
            return repository.getRoot();
        } else {
            return this.prevStateRoot;
        }
    }

    @Override
    public ProgramResult create(ProgramCreate programCreate) {
        ProgramInvoke programInvoke = new ProgramInvoke();
        programInvoke.setAddress(programCreate.getContractAddress());
        programInvoke.setSender(programCreate.getSender());
        programInvoke.setGasPrice(programCreate.getPrice());
        programInvoke.setGas(programCreate.getGasLimit());
        programInvoke.setValue(programCreate.getValue() != null ? programCreate.getValue() : BigInteger.ZERO);
        programInvoke.setNumber(programCreate.getNumber());
        programInvoke.setData(programCreate.getContractCode());
        programInvoke.setMethodName("<init>");
        programInvoke.args(programCreate.getArgs() != null ? programCreate.getArgs() : new String[0]);
        programInvoke.setEstimateGas(programCreate.isEstimateGas());
        return execute(programInvoke);
    }

    @Override
    public ProgramResult call(ProgramCall programCall) {
        ProgramInvoke programInvoke = new ProgramInvoke();
        programInvoke.setAddress(programCall.getContractAddress());
        programInvoke.setSender(programCall.getSender());
        programInvoke.setGasPrice(programCall.getPrice());
        programInvoke.setGas(programCall.getGasLimit());
        programInvoke.setValue(programCall.getValue() != null ? programCall.getValue() : BigInteger.ZERO);
        programInvoke.setNumber(programCall.getNumber());
        programInvoke.setMethodName(programCall.getMethodName());
        programInvoke.setMethodDesc(programCall.getMethodDesc());
        programInvoke.args(programCall.getArgs() != null ? programCall.getArgs() : new String[0]);
        programInvoke.setEstimateGas(programCall.isEstimateGas());
        return execute(programInvoke);
    }

    public ProgramResult execute(ProgramInvoke programInvoke) {
        Set<ConstraintViolation<ProgramInvoke>> constraintViolations = Validators.validate(programInvoke);
        if (!constraintViolations.isEmpty()) {
            String message = Validators.message(constraintViolations);
            return revert(message);
        }

        try {
            List<ClassCode> classCodes;
            boolean newContract = false;
            AccountState accountState = repository.getAccountState(programInvoke.getAddress());
            if (accountState == null) {
                classCodes = ClassCodeLoader.loadJar(programInvoke.getData());
                ProgramChecker.check(classCodes);
                accountState = repository.createAccount(programInvoke.getAddress(), programInvoke.getSender());
                repository.saveCode(programInvoke.getAddress(), programInvoke.getData());
                newContract = true;
            } else {
                if ("<init>".equals(programInvoke.getMethodName())) {
                    return revert("can't invoke <init> method");
                }
                if (accountState.getNonce().compareTo(BigInteger.ZERO) <= 0) {
                    return revert("contract has been stopped");
                }
                byte[] codes = repository.getCode(programInvoke.getAddress());
                classCodes = ClassCodeLoader.loadJar(codes);
            }

            VM vm = VMFactory.createVM();
            vm.getMethodArea().loadClassCodes(classCodes);
            ClassCode contractClassCode = getContractClassCode(classCodes);
            MethodCode methodCode = vm.getMethodArea().loadMethod(contractClassCode.getName(), programInvoke.getMethodName(), programInvoke.getMethodDesc());

            if (methodCode == null) {
                return revert(String.format("can't find method %s.%s", programInvoke.getMethodName(), programInvoke.getMethodDesc()));
            }
            if (!methodCode.isPublic()) {
                return revert("can only invoke public method");
            }
            if (methodCode.getArgsVariableType().size() != programInvoke.getArgs().length) {
                return revert("method args error");
            }

            ObjectRef objectRef;
            if (newContract) {
                objectRef = vm.getHeap().newContract(programInvoke.getAddress(), contractClassCode, repository);
            } else {
                objectRef = vm.getHeap().loadContract(programInvoke.getAddress(), contractClassCode, repository);
            }

            vm.setProgramExecutor(this);
            vm.setGasUsed(programInvoke.getData() == null ? 0 : programInvoke.getData().length);
            vm.run(objectRef, methodCode, vmContext, programInvoke);

            if (vm.isRevert()) {
                return revert(vm.getErrorMessage(), vm.getStackTrace());
            }

            vm.getHeap().contractState();

            repository.increaseNonce(programInvoke.getAddress());

            ProgramResult programResult = new ProgramResult();
            programResult.setGasUsed(vm.getGasUsed());
            programResult.setNonce(repository.getNonce(programInvoke.getAddress()));

            if (vm.isError()) {
                programResult.setStackTrace(vm.getStackTrace());
                return programResult.error(vm.getErrorMessage());
            }

            if (!vm.getResult().isError() && !vm.getResult().isException()) {
                programResult.setTransfers(vm.getTransfers());
                programResult.setEvents(vm.getEvents());
                if (programInvoke.getValue() != null && programInvoke.getValue().compareTo(BigInteger.ZERO) > 0) {
                    repository.addBalance(programInvoke.getAddress(), programInvoke.getValue());
                }
                for (ProgramTransfer programTransfer : vm.getTransfers()) {
                    if (!programTransfer.isChangeContractBalance()) {
                        programTransfer.setChangeContractBalance(true);
                        repository.addBalance(programTransfer.getFrom(), programTransfer.getValue().negate());
                    }
                }
            }
            programResult.setBalance(repository.getBalance(programInvoke.getAddress()));

            Object resultValue = vm.getResult().getValue();
            if (resultValue != null) {
                if (resultValue instanceof ObjectRef) {
                    if (vm.getResult().isError() || vm.getResult().isException()) {
                        vm.setResult(new Result());
                        String error = vm.getHeap().toString((ObjectRef) resultValue);
                        String stackTrace = vm.getHeap().stackTrace((ObjectRef) resultValue);
                        programResult.error(error);
                        programResult.setStackTrace(stackTrace);
                    } else {
                        String result = vm.getHeap().toString((ObjectRef) resultValue);
                        programResult.setResult(result);
                    }
                } else {
                    programResult.setResult(resultValue.toString());
                }
            }

            if (methodCode.isGetter()) {
                getter = true;
                programResult.getter();
            }

            return programResult;
        } catch (Exception e) {
            log.error("", e);
            ProgramResult programResult = revert(e.getMessage());
            programResult.setStackTrace(ExceptionUtils.getStackTrace(e));
            return programResult;
        }
    }

    private ProgramResult revert(String errorMessage) {
        return revert(errorMessage, null);
    }

    private ProgramResult revert(String errorMessage, String stackTrace) {
        ProgramResult programResult = new ProgramResult();
        programResult.setStackTrace(stackTrace);
        this.revert = true;
        return programResult.revert(errorMessage);
    }

    @Override
    public ProgramResult stop(byte[] address, byte[] sender) {
        AccountState accountState = repository.getAccountState(address);
        if (accountState == null) {
            return revert("can't find contract");
        }
        if (!FastByteComparisons.equal(sender, accountState.getOwner())) {
            return revert("only the owner can stop the contract");
        }
        if (BigInteger.ZERO.compareTo(accountState.getBalance()) != 0) {
            return revert("contract balance is not zero");
        }

        repository.setNonce(address, BigInteger.ZERO);

        ProgramResult programResult = new ProgramResult();

        return programResult;
    }

    @Override
    public ProgramStatus status(byte[] address) {
        AccountState accountState = repository.getAccountState(address);
        if (accountState == null) {
            return ProgramStatus.not_found;
        } else {
            BigInteger nonce = repository.getNonce(address);
            if (BigInteger.ZERO.compareTo(nonce) == 0) {
                return ProgramStatus.stop;
            } else {
                return ProgramStatus.normal;
            }
        }
    }

    @Override
    public List<ProgramMethod> method(byte[] address) {
        List<ProgramMethod> programMethods = new ArrayList<>();
        byte[] codes = repository.getCode(address);
        if (codes == null || codes.length < 1) {
            return programMethods;
        }
        List<ClassCode> classCodes = ClassCodeLoader.loadJar(codes);

        ClassCode contractClassCode = getContractClassCode(classCodes);
        Map<String, MethodCode> methodCodes = new HashMap<>();
        contractMethods(methodCodes, classCodes, contractClassCode);

        methodCodes.forEach((s, methodCode) -> {
            ProgramMethod method = new ProgramMethod();
            method.setName(methodCode.getName());
            method.setDesc(methodCode.getDesc());
            List<String> args = new ArrayList<>();
            methodCode.getArgsVariableType().forEach(variableType -> {
                args.add(variableType.getNormalDesc());
            });
            method.setArgs(args);
            method.setReturnArg(methodCode.getReturnVariableType().getNormalDesc());
            programMethods.add(method);
        });

        return programMethods;
    }

    private ClassCode getContractClassCode(List<ClassCode> classCodes) {
        return classCodes.stream().filter(classCode -> classCode.getInterfaces().contains(ProgramConstants.CONTRACT_INTERFACE_NAME)
                || ProgramConstants.CONTRACT_INTERFACE_NAME.equals(classCode.getSuperName())).findFirst().get();
    }

    private void contractMethods(Map<String, MethodCode> methodCodes, List<ClassCode> classCodes, ClassCode classCode) {
        classCode.getMethods().stream().filter(methodCode -> {
            if (methodCode.isPublic() && methodCode.isNotAbstract()) {
                return true;
            } else {
                return false;
            }
        }).forEach(methodCode -> {
            String name = methodCode.getName() + "." + methodCode.getDesc();
            methodCodes.putIfAbsent(name, methodCode);
        });
        String superName = classCode.getSuperName();
        if (StringUtils.isNotEmpty(superName)) {
            classCodes.stream().filter(code -> superName.equals(code.getName())).findFirst()
                    .ifPresent(code -> {
                        contractMethods(methodCodes, classCodes, code);
                    });
        }
    }

}
