package io.nuls.contract.vm.util;

public class VmUtils {

    public static String stackTrace(Throwable throwable) {
        StringBuilder s = new StringBuilder();
        s.append(throwable.toString());
        s.append("\n");
        StackTraceElement[] trace = throwable.getStackTrace();
        for (StackTraceElement traceElement : trace) {
            s.append("\tat " + traceElement);
            s.append("\n");
        }
        return s.toString();
    }

}
