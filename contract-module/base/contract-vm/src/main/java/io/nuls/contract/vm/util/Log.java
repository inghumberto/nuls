package io.nuls.contract.vm.util;

import io.nuls.contract.vm.OpCode;
import io.nuls.contract.vm.Result;
import io.nuls.contract.vm.code.MethodCode;

import java.util.Arrays;

public class Log {

    // TODO: 2018/4/27

    public static void loadClass(String className) {
        System.out.println("load class: " + className);
    }

    public static void runMethod(MethodCode methodCode) {
        String log = "run method: " + methodCode.getClassName() + "." + methodCode.getName() + methodCode.getDesc();
        System.out.println(log);
    }

    public static void continueMethod(MethodCode methodCode) {
        String log = "continue method: " + methodCode.getClassName() + "." + methodCode.getName() + methodCode.getDesc();
        System.out.println(log);
    }

    public static void endMethod(MethodCode methodCode) {
        String log = "end method: " + methodCode.getClassName() + "." + methodCode.getName() + methodCode.getDesc();
        System.out.println(log);
    }

    public static void nativeMethod(MethodCode methodCode) {
        String log = "native method: " + methodCode.getClassName() + "." + methodCode.getName() + methodCode.getDesc();
        System.out.println(log);
    }

    public static void nativeMethodResult(Result result) {
        String log = "native method result: " + result;
        System.out.println(log);
    }

    public static void opcode(OpCode opCode, Object... args) {
        String log = opCode.name();
        if (args != null && args.length > 0) {
            log += " " + Arrays.toString(args);
        }
        System.out.println(log);
    }

    public static void result(OpCode opCode, Object result, Object... args) {
        String log = opCode + " " + Arrays.toString(args) + " " + result;
        System.out.println(log);
    }

    public static void log(OpCode opCode, Object... args) {
        if (args == null) {
            System.out.println(opCode);
        } else {
            System.out.println(opCode + " " + Arrays.toString(args));
        }
    }

}
