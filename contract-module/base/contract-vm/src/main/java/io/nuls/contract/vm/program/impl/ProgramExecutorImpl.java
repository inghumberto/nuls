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

    public ProgramExecutorImpl(VMContext vmContext, DBService dbService) {
        this(vmContext, new KeyValueSource(dbService), null);
    }

    private ProgramExecutorImpl(VMContext vmContext, KeyValueSource keyValueSource, Repository repository) {
        this.vmContext = vmContext;
        this.keyValueSource = keyValueSource;
        this.repository = repository;
    }

    @Override
    public ProgramExecutor begin(byte[] prevStateRoot) {
        this.prevStateRoot = prevStateRoot;
        Repository repository = new RepositoryRoot(keyValueSource, prevStateRoot);
        return new ProgramExecutorImpl(vmContext, keyValueSource, repository);
    }

    @Override
    public ProgramExecutor startTracking() {
        Repository track = repository.startTracking();
        return new ProgramExecutorImpl(vmContext, keyValueSource, track);
    }

    @Override
    public void commit() {
        if (!revert) {
            repository.commit();
        }
    }

    @Override
    public byte[] getRoot() {
        if (!revert) {
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
        return execute(programInvoke);
    }

    public ProgramResult execute(ProgramInvoke programInvoke) {
        ProgramResult programResult = new ProgramResult();
        Set<ConstraintViolation<ProgramInvoke>> constraintViolations = Validators.validate(programInvoke);
        if (!constraintViolations.isEmpty()) {
            String message = Validators.message(constraintViolations);
            return revert(programResult, message);
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
                    return revert(programResult, "can't invoke <init> method");
                }
                if (accountState.getNonce().compareTo(BigInteger.ZERO) <= 0) {
                    return revert(programResult, "contract has been stopped");
                }
                byte[] codes = repository.getCode(programInvoke.getAddress());
                classCodes = ClassCodeLoader.loadJar(codes);
            }

            VM vm = VMFactory.createVM();
            vm.getMethodArea().loadClassCodes(classCodes);
            ClassCode contractClassCode = getContractClassCode(classCodes);
            MethodCode methodCode = vm.getMethodArea().loadMethod(contractClassCode.getName(), programInvoke.getMethodName(), programInvoke.getMethodDesc());

            if (methodCode == null) {
                return revert(programResult, String.format("can't find method %s.%s", programInvoke.getMethodName(), programInvoke.getMethodDesc()));
            }
            if (!methodCode.isPublic()) {
                return revert(programResult, "can only invoke public method");
            }
            if (methodCode.getArgsVariableType().size() != programInvoke.getArgs().length) {
                return revert(programResult, "method args error");
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
                return revert(programResult, vm.getErrorMessage());
            }

            vm.getHeap().contractState();

            repository.increaseNonce(programInvoke.getAddress());

            programResult.setGasUsed(vm.getGasUsed());
            programResult.setTransfers(vm.getTransfers());
            programResult.setEvents(vm.getEvents());
            programResult.setNonce(repository.getNonce(programInvoke.getAddress()));
            if (!vm.getResult().isError() && !vm.getResult().isException()) {
                if (programInvoke.getValue() != null && programInvoke.getValue().compareTo(BigInteger.ZERO) > 0) {
                    repository.addBalance(programInvoke.getAddress(), programInvoke.getValue());
                }
                for (ProgramTransfer programTransfer : vm.getTransfers()) {
                    repository.addBalance(programTransfer.getFrom(), programTransfer.getValue().negate());
                }
            }
            programResult.setBalance(repository.getBalance(programInvoke.getAddress()));

            if (vm.isError()) {
                return programResult.error(vm.getErrorMessage());
            }

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
        } catch (Exception e) {
            log.error("", e);
            return revert(programResult, e.getMessage());
        }

        return programResult;
    }

    private ProgramResult revert(ProgramResult programResult, String errorMessage) {
        this.revert = true;
        return programResult.revert(errorMessage);
    }

    @Override
    public ProgramResult stop(byte[] address, byte[] sender) {
        ProgramResult programResult = new ProgramResult();
        AccountState accountState = repository.getAccountState(address);
        if (accountState == null) {
            return revert(programResult, "can't find contract");
        } else if (!FastByteComparisons.equal(sender, accountState.getOwner())) {
            return revert(programResult, "only the owner can stop the contract");
        } else {
            repository.setNonce(address, BigInteger.ZERO);
        }
        return programResult;
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
