package io.nuls.contract.vm.natives.io.nuls.contract.sdk;

import io.nuls.contract.vm.*;
import io.nuls.contract.vm.code.MethodCode;
import io.nuls.contract.vm.natives.NativeMethod;
import io.nuls.contract.vm.program.ProgramCall;
import io.nuls.contract.vm.program.ProgramResult;
import io.nuls.contract.vm.program.ProgramTransfer;
import io.nuls.contract.vm.program.impl.ProgramInvoke;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;

public class NativeAddress {

    public static final String TYPE = "io/nuls/contract/sdk/Address";

    public static Result run(MethodCode methodCode, MethodArgs methodArgs, Frame frame) {
        Result result = null;
        switch (methodCode.getName()) {
            case "balance":
                result = balance(methodCode, methodArgs, frame);
                break;
            case "transfer":
                result = transfer(methodCode, methodArgs, frame);
                break;
            case "call":
                result = call(methodCode, methodArgs, frame);
                break;
            default:
                frame.nonsupportMethod(methodCode);
                break;
        }
        return result;
    }

    private static BigInteger balance(byte[] address, Frame frame) {
        BigInteger balance = BigInteger.ZERO;
        if (frame.getVm().getVmContext() != null) {
            balance = frame.getVm().getVmContext().getBalance(address);
        }
        return balance;
    }

    private static Result balance(MethodCode methodCode, MethodArgs methodArgs, Frame frame) {
        ObjectRef objectRef = methodArgs.getObjectRef();
        String address = (String) frame.getHeap().getObject(objectRef);
        BigInteger balance = balance(Hex.decode(address), frame);
        ObjectRef balanceRef = frame.getHeap().newBigInteger(balance.toString());
        Result result = NativeMethod.result(methodCode, balanceRef, frame);
        return result;
    }

    private static Result transfer(MethodCode methodCode, MethodArgs methodArgs, Frame frame) {
        ObjectRef addressRef = methodArgs.getObjectRef();
        ObjectRef valueRef = (ObjectRef) methodArgs.getInvokeArgs()[0];
        String address = (String) frame.getHeap().getObject(addressRef);
        BigInteger value = (BigInteger) frame.getHeap().getObject(valueRef);
        byte[] from = frame.getVm().getProgramInvoke().getAddress();
        byte[] to = Hex.decode(address);
        if (frame.getHeap().existContract(to)) {
            throw new RuntimeException("Cannot transfer to contract");
        }
        BigInteger balance = balance(from, frame);
        if (balance.compareTo(value) < 0) {
            throw new RuntimeException("Not enough balance");
        }

        ProgramTransfer programTransfer = new ProgramTransfer(from, to, value);
        frame.getVm().setGasUsed(frame.getVm().getGasUsed() + GasCost.TRANSFER);
        frame.getVm().getTransfers().add(programTransfer);
        Result result = NativeMethod.result(methodCode, null, frame);
        return result;
    }

    private static Result call(MethodCode methodCode, MethodArgs methodArgs, Frame frame) {
        ObjectRef addressRef = methodArgs.getObjectRef();
        ObjectRef methodNameRef = (ObjectRef) methodArgs.getInvokeArgs()[0];
        ObjectRef methodDescRef = (ObjectRef) methodArgs.getInvokeArgs()[1];
        ObjectRef argsRef = (ObjectRef) methodArgs.getInvokeArgs()[2];
        ObjectRef valueRef = (ObjectRef) methodArgs.getInvokeArgs()[3];

        String address = frame.getHeap().toString(addressRef);
        String methodName = (String) frame.getHeap().getObject(methodNameRef);
        String methodDesc = (String) frame.getHeap().getObject(methodDescRef);
        String[] args = (String[]) frame.getHeap().getObject(argsRef);
        BigInteger value = (BigInteger) frame.getHeap().getObject(valueRef);

        ProgramInvoke programInvoke = frame.getVm().getProgramInvoke();
        ProgramCall programCall = new ProgramCall();
        programCall.setNumber(programInvoke.getNumber());
        programCall.setSender(programInvoke.getAddress());
        programCall.setValue(value != null ? value : BigInteger.ZERO);
        programCall.setGasLimit(programInvoke.getGas() - frame.getVm().getGasUsed());
        programCall.setPrice(programInvoke.getGasPrice());
        programCall.setContractAddress(Hex.decode(address));
        programCall.setMethodName(methodName);
        programCall.setMethodDesc(methodDesc);
        programCall.setArgs(args);

        ProgramResult programResult = frame.getVm().getProgramExecutor().call(programCall);

        frame.getVm().setGasUsed(frame.getVm().getGasUsed() + programResult.getGasUsed());
        if (!programResult.isError()) {
            if (programCall.getValue().compareTo(BigInteger.ZERO) > 0) {
                ProgramTransfer programTransfer = new ProgramTransfer(programInvoke.getAddress(), Hex.decode(address), programCall.getValue());
                frame.getVm().getTransfers().add(programTransfer);
            }
            frame.getVm().getTransfers().addAll(programResult.getTransfers());
            frame.getVm().getEvents().addAll(programResult.getEvents());
        } else {
            ObjectRef errorRef = frame.getHeap().newString(programResult.getErrorMessage());
            frame.getVm().getResult().error(errorRef);
        }

        Result result = NativeMethod.result(methodCode, null, frame);
        return result;
    }

}
