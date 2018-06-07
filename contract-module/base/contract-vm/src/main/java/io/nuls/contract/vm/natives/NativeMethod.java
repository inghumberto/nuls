package io.nuls.contract.vm.natives;

import io.nuls.contract.vm.Frame;
import io.nuls.contract.vm.MethodArgs;
import io.nuls.contract.vm.ObjectRef;
import io.nuls.contract.vm.Result;
import io.nuls.contract.vm.code.MethodCode;
import io.nuls.contract.vm.code.VariableType;
import io.nuls.contract.vm.natives.io.nuls.contract.sdk.NativeAddress;
import io.nuls.contract.vm.natives.io.nuls.contract.sdk.NativeBlock;
import io.nuls.contract.vm.natives.io.nuls.contract.sdk.NativeMsg;
import io.nuls.contract.vm.natives.io.nuls.contract.sdk.NativeUtils;
import io.nuls.contract.vm.natives.java.lang.*;
import io.nuls.contract.vm.util.Log;

public class NativeMethod {

    public static Result run(MethodCode methodCode, MethodArgs methodArgs, Frame frame) {

        if (methodCode.getInstructions().size() > 0) {
            return null;
        }

        if ("registerNatives".equals(methodCode.getName())) {
            return new Result(methodCode.getReturnVariableType());
        }

        Log.nativeMethod(methodCode);

        Result result = null;
        switch (methodCode.getClassCode().getName()) {
            case NativeClass.TYPE:
                result = NativeClass.run(methodCode, methodArgs, frame);
                break;
            case NativeDouble.TYPE:
                result = NativeDouble.run(methodCode, methodArgs, frame);
                break;
            case NativeFloat.TYPE:
                result = NativeFloat.run(methodCode, methodArgs, frame);
                break;
            case NativeObject.TYPE:
                result = NativeObject.run(methodCode, methodArgs, frame);
                break;
            case NativeSystem.TYPE:
                result = NativeSystem.run(methodCode, methodArgs, frame);
                break;
            case NativeStrictMath.TYPE:
                result = NativeStrictMath.run(methodCode, methodArgs, frame);
                break;
            case NativeString.TYPE:
                result = NativeString.run(methodCode, methodArgs, frame);
                break;
            case NativeThrowable.TYPE:
                result = NativeThrowable.run(methodCode, methodArgs, frame);
                break;
            case NativeBlock.TYPE:
                result = NativeBlock.run(methodCode, methodArgs, frame);
                break;
            case NativeUtils.TYPE:
                result = NativeUtils.run(methodCode, methodArgs, frame);
                break;
            case NativeMsg.TYPE:
                result = NativeMsg.run(methodCode, methodArgs, frame);
                break;
            case NativeAddress.TYPE:
                result = NativeAddress.run(methodCode, methodArgs, frame);
                break;
            default:
                frame.nonsupportMethod(methodCode);
                break;
        }

        Log.nativeMethodResult(result);

        return result;
    }

    public static Result result(MethodCode methodCode, Object resultValue, Frame frame) {
        VariableType variableType = methodCode.getReturnVariableType();
        Result result = new Result(variableType);
        if (variableType.isNotVoid()) {
            result.value(resultValue);
            if (resultValue == null) {
                frame.getOperandStack().pushRef(null);
            } else if (variableType.isPrimitive()) {
                frame.getOperandStack().push(resultValue, variableType);
            } else if (resultValue instanceof ObjectRef) {
                frame.getOperandStack().pushRef((ObjectRef) resultValue);
            } else {
                throw new IllegalArgumentException("unknown result value");
            }
        }
        return result;
    }

}
