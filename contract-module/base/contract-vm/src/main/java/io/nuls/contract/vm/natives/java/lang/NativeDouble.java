package io.nuls.contract.vm.natives.java.lang;

import io.nuls.contract.vm.Frame;
import io.nuls.contract.vm.MethodArgs;
import io.nuls.contract.vm.Result;
import io.nuls.contract.vm.code.MethodCode;
import io.nuls.contract.vm.natives.NativeMethod;

public class NativeDouble {

    public static final String TYPE = "java/lang/Double";

    public static Result run(MethodCode methodCode, MethodArgs methodArgs, Frame frame) {
        Result result = null;
        switch (methodCode.getName()) {
            case "doubleToRawLongBits":
                result = doubleToRawLongBits(methodCode, methodArgs, frame);
                break;
            case "longBitsToDouble":
                result = longBitsToDouble(methodCode, methodArgs, frame);
                break;
            default:
                frame.nonsupportMethod(methodCode);
                break;
        }
        return result;
    }

    private static Result doubleToRawLongBits(MethodCode methodCode, MethodArgs methodArgs, Frame frame) {
        double value = (double) methodArgs.getInvokeArgs()[0];
        long bits = Double.doubleToRawLongBits(value);
        Result result = NativeMethod.result(methodCode, bits, frame);
        return result;
    }

    private static Result longBitsToDouble(MethodCode methodCode, MethodArgs methodArgs, Frame frame) {
        long bits = (long) methodArgs.getInvokeArgs()[0];
        double d = Double.longBitsToDouble(bits);
        Result result = NativeMethod.result(methodCode, d, frame);
        return result;
    }

}
