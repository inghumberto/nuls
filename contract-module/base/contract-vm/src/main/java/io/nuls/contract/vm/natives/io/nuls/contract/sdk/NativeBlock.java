package io.nuls.contract.vm.natives.io.nuls.contract.sdk;

import io.nuls.contract.entity.BlockHeader;
import io.nuls.contract.vm.Frame;
import io.nuls.contract.vm.MethodArgs;
import io.nuls.contract.vm.ObjectRef;
import io.nuls.contract.vm.Result;
import io.nuls.contract.vm.code.MethodCode;
import io.nuls.contract.vm.natives.NativeMethod;
import io.nuls.core.tools.crypto.Hex;

public class NativeBlock {

    public static final String TYPE = "io/nuls/contract/sdk/Block";

    public static Result run(MethodCode methodCode, MethodArgs methodArgs, Frame frame) {
        Result result = null;
        switch (methodCode.getName()) {
            case "blockhash":
                result = blockhash(methodCode, methodArgs, frame);
                break;
            case "coinbase":
                result = coinbase(methodCode, methodArgs, frame);
                break;
            case "number":
                result = number(methodCode, methodArgs, frame);
                break;
            case "timestamp":
                result = timestamp(methodCode, methodArgs, frame);
                break;
            default:
                frame.nonsupportMethod(methodCode);
                break;
        }
        return result;
    }

    private static Result blockhash(MethodCode methodCode, MethodArgs methodArgs, Frame frame) {
        long blockNumber = (long) methodArgs.getInvokeArgs()[0];
        BlockHeader blockHeader = frame.getVm().getBlockHeader(blockNumber);
        ObjectRef objectRef = null;
        if (blockHeader != null) {
            byte[] blockhash = Hex.decode(blockHeader.getHash());
            objectRef = frame.getHeap().newArray(blockhash);
        }
        Result result = NativeMethod.result(methodCode, objectRef, frame);
        return result;
    }

    private static Result coinbase(MethodCode methodCode, MethodArgs methodArgs, Frame frame) {
        Result result = NativeMethod.result(methodCode, frame.getVm().getProgramContext().getCoinbase(), frame);
        return result;
    }

    private static Result number(MethodCode methodCode, MethodArgs methodArgs, Frame frame) {
        Result result = NativeMethod.result(methodCode, frame.getVm().getProgramContext().getNumber(), frame);
        return result;
    }

    private static Result timestamp(MethodCode methodCode, MethodArgs methodArgs, Frame frame) {
        Result result = NativeMethod.result(methodCode, frame.getVm().getProgramContext().getTimestamp(), frame);
        return result;
    }

}
