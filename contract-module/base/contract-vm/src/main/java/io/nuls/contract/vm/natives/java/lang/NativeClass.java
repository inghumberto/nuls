package io.nuls.contract.vm.natives.java.lang;

import io.nuls.contract.vm.Frame;
import io.nuls.contract.vm.MethodArgs;
import io.nuls.contract.vm.ObjectRef;
import io.nuls.contract.vm.Result;
import io.nuls.contract.vm.code.ClassCode;
import io.nuls.contract.vm.code.MethodCode;
import io.nuls.contract.vm.code.VariableType;
import io.nuls.contract.vm.natives.NativeMethod;

public class NativeClass {

    public static final String TYPE = "java/lang/Class";

    public static Result run(MethodCode methodCode, MethodArgs methodArgs, Frame frame) {
        Result result = null;
        switch (methodCode.getName()) {
            case "getPrimitiveClass":
                result = getPrimitiveClass(methodCode, methodArgs, frame);
                break;
            case "getComponentType":
                result = getComponentType(methodCode, methodArgs, frame);
                break;
            case "isArray":
                result = isArray(methodCode, methodArgs, frame);
                break;
            case "isPrimitive":
                result = isPrimitive(methodCode, methodArgs, frame);
                break;
            case "isInterface":
                result = isInterface(methodCode, methodArgs, frame);
                break;
            case "desiredAssertionStatus0":
                result = desiredAssertionStatus0(methodCode, methodArgs, frame);
                break;
            default:
                frame.nonsupportMethod(methodCode);
                break;
        }
        return result;
    }

    private static Result getPrimitiveClass(MethodCode methodCode, MethodArgs methodArgs, Frame frame) {
        ObjectRef objectRef = (ObjectRef) methodArgs.getInvokeArgs()[0];
        String name = (String) frame.getHeap().getObject(objectRef);
        VariableType variableType = VariableType.valueOf(name);
        ObjectRef classRef = frame.getHeap().getClassRef(variableType.getDesc());
        Result result = NativeMethod.result(methodCode, classRef, frame);
        return result;
    }

    private static Result getComponentType(MethodCode methodCode, MethodArgs methodArgs, Frame frame) {
        ObjectRef objectRef = methodArgs.getObjectRef();
        VariableType variableType = VariableType.valueOf(objectRef.getRef());
        ObjectRef classRef = null;
        if (variableType.isArray()) {
            classRef = frame.getHeap().getClassRef(variableType.getComponentType().getDesc());
        }
        Result result = NativeMethod.result(methodCode, classRef, frame);
        return result;
    }

    private static Result isArray(MethodCode methodCode, MethodArgs methodArgs, Frame frame) {
        ObjectRef objectRef = methodArgs.getObjectRef();
        VariableType variableType = VariableType.valueOf(objectRef.getRef());
        boolean b = variableType.isArray();
        Result result = NativeMethod.result(methodCode, b, frame);
        return result;
    }

    private static Result isPrimitive(MethodCode methodCode, MethodArgs methodArgs, Frame frame) {
        ObjectRef objectRef = methodArgs.getObjectRef();
        VariableType variableType = VariableType.valueOf(objectRef.getRef());
        boolean b = variableType.isPrimitive();
        Result result = NativeMethod.result(methodCode, b, frame);
        return result;
    }

    private static Result isInterface(MethodCode methodCode, MethodArgs methodArgs, Frame frame) {
        ObjectRef objectRef = methodArgs.getObjectRef();
        VariableType variableType = VariableType.valueOf(objectRef.getRef());
        ClassCode classCode = frame.getMethodArea().loadClass(variableType.getType());
        boolean b = !variableType.isArray() && classCode.isInterface();
        Result result = NativeMethod.result(methodCode, b, frame);
        return result;
    }

    private static Result desiredAssertionStatus0(MethodCode methodCode, MethodArgs methodArgs, Frame frame) {
        boolean status = false;
        Result result = NativeMethod.result(methodCode, status, frame);
        return result;
    }

}
