package io.nuls.contract.vm.natives.java.lang;

import io.nuls.contract.vm.Frame;
import io.nuls.contract.vm.MethodArgs;
import io.nuls.contract.vm.Result;
import io.nuls.contract.vm.code.MethodCode;
import io.nuls.contract.vm.natives.NativeMethod;
import org.apache.commons.lang3.reflect.MethodUtils;

import java.lang.reflect.InvocationTargetException;

public class NativeStrictMath {

    public static final String TYPE = "java/lang/StrictMath";

    public static Result run(MethodCode methodCode, MethodArgs methodArgs, Frame frame) {
        Object invokeResult = null;
        try {
            invokeResult = MethodUtils.invokeStaticMethod(StrictMath.class, methodCode.getName(), methodArgs.getInvokeArgs());
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
        Result result = NativeMethod.result(methodCode, invokeResult, frame);
        return result;
    }

}
