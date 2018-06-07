package io.nuls.contract.vm.natives.java.lang;

import io.nuls.contract.vm.Frame;
import io.nuls.contract.vm.MethodArgs;
import io.nuls.contract.vm.ObjectRef;
import io.nuls.contract.vm.Result;
import io.nuls.contract.vm.code.MethodCode;
import io.nuls.contract.vm.code.VariableType;
import io.nuls.contract.vm.natives.NativeMethod;

public class NativeThrowable {

    public static final String TYPE = "java/lang/Throwable";

    public static Result run(MethodCode methodCode, MethodArgs methodArgs, Frame frame) {
        Result result = null;
        switch (methodCode.getName()) {
            case "fillInStackTrace":
                result = fillInStackTrace(methodCode, methodArgs, frame);
                break;
            case "getStackTraceDepth":
                result = getStackTraceDepth(methodCode, methodArgs, frame);
                break;
            case "getStackTraceElement":
                result = getStackTraceElement(methodCode, methodArgs, frame);
                break;
            default:
                frame.nonsupportMethod(methodCode);
                break;
        }
        return result;
    }

    private static Result fillInStackTrace(MethodCode methodCode, MethodArgs methodArgs, Frame frame) {
        int dummy = (int) methodArgs.getInvokeArgs()[0];
        ObjectRef objectRef = methodArgs.getObjectRef();

        int size = frame.getVm().getVmStack().size();
        ObjectRef stackTraceElementsRef = frame.getHeap().newArray(VariableType.STACK_TRACE_ELEMENT_ARRAY_TYPE, size);
        frame.getHeap().putField(objectRef, "stackTraceElements", stackTraceElementsRef);
        for (int i = 0; i < size; i++) {
            Frame frame1 = frame.getVm().getVmStack().get(i);
            ObjectRef declaringClass = frame.getHeap().newString(frame1.getMethodCode().getClassName());
            ObjectRef methodName = frame.getHeap().newString(frame1.getMethodCode().getName());
            ObjectRef fileName = frame.getHeap().newString(frame1.getMethodCode().getClassName());
            int lineNumber = 0;
            ObjectRef stackTraceElementRef = frame.getHeap().newObject(VariableType.STACK_TRACE_ELEMENT_TYPE, null, declaringClass, methodName, fileName, lineNumber);
            frame.getHeap().putArray(stackTraceElementsRef, i, stackTraceElementRef);
        }

        Result result = NativeMethod.result(methodCode, objectRef, frame);
        return result;
    }

    private static Result getStackTraceDepth(MethodCode methodCode, MethodArgs methodArgs, Frame frame) {
        ObjectRef objectRef = methodArgs.getObjectRef();
        ObjectRef stackTraceElementsRef = (ObjectRef) frame.getHeap().getField(objectRef, "stackTraceElements");
        int depth = stackTraceElementsRef.getDimensions()[0];
        Result result = NativeMethod.result(methodCode, depth, frame);
        return result;
    }

    private static Result getStackTraceElement(MethodCode methodCode, MethodArgs methodArgs, Frame frame) {
        int index = (int) methodArgs.getInvokeArgs()[0];
        ObjectRef objectRef = methodArgs.getObjectRef();
        ObjectRef stackTraceElementsRef = (ObjectRef) frame.getHeap().getField(objectRef, "stackTraceElements");
        ObjectRef stackTraceElementRef = (ObjectRef) frame.getHeap().getArray(stackTraceElementsRef, index);
        Result result = NativeMethod.result(methodCode, stackTraceElementRef, frame);
        return result;
    }

}
