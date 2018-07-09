package io.nuls.contract.vm.natives.io.nuls.contract.sdk;

import io.nuls.contract.vm.Frame;
import io.nuls.contract.vm.MethodArgs;
import io.nuls.contract.vm.ObjectRef;
import io.nuls.contract.vm.Result;
import io.nuls.contract.vm.code.MethodCode;
import io.nuls.contract.vm.natives.NativeMethod;
import org.spongycastle.util.encoders.Hex;

public class NativeUtils {

    public static final String TYPE = "io/nuls/contract/sdk/Utils";

    public static Result run(MethodCode methodCode, MethodArgs methodArgs, Frame frame) {
        Result result = null;
        switch (methodCode.getName()) {
            case "revert":
                result = revert(methodCode, methodArgs, frame);
                break;
            case "emit":
                result = emit(methodCode, methodArgs, frame);
                break;
            case "encodeHexString":
                result = encodeHexString(methodCode, methodArgs, frame);
                break;
            case "decodeHex":
                result = decodeHex(methodCode, methodArgs, frame);
                break;
            default:
                frame.nonsupportMethod(methodCode);
                break;
        }
        return result;
    }

    private static Result revert(MethodCode methodCode, MethodArgs methodArgs, Frame frame) {
        ObjectRef objectRef = (ObjectRef) methodArgs.getInvokeArgs()[0];
        String errorMessage = null;
        if (objectRef != null) {
            errorMessage = frame.getHeap().toString(objectRef);
        }
        frame.getVm().revert(errorMessage);
        Result result = NativeMethod.result(methodCode, null, frame);
        return result;
    }

    private static Result emit(MethodCode methodCode, MethodArgs methodArgs, Frame frame) {
        ObjectRef objectRef = (ObjectRef) methodArgs.getInvokeArgs()[0];
        String str = frame.getHeap().toString(objectRef);
        frame.getVm().getEvents().add(str);
        Result result = NativeMethod.result(methodCode, null, frame);
        return result;
    }

    private static Result encodeHexString(MethodCode methodCode, MethodArgs methodArgs, Frame frame) {
        ObjectRef objectRef = (ObjectRef) methodArgs.getInvokeArgs()[0];
        byte[] bytes = (byte[]) frame.getHeap().getObject(objectRef);
        String str = Hex.toHexString(bytes);
        ObjectRef strRef = frame.getHeap().newString(str);
        Result result = NativeMethod.result(methodCode, strRef, frame);
        return result;
    }

    private static Result decodeHex(MethodCode methodCode, MethodArgs methodArgs, Frame frame) {
        ObjectRef objectRef = (ObjectRef) methodArgs.getInvokeArgs()[0];
        String data = (String) frame.getHeap().getObject(objectRef);
        byte[] bytes = Hex.decode(data);
        ObjectRef ref = frame.getHeap().newArray(bytes);
        Result result = NativeMethod.result(methodCode, ref, frame);
        return result;
    }

}
