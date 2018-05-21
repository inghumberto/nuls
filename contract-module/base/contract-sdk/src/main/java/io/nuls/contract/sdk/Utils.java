package io.nuls.contract.sdk;

public class Utils {

    public static void require(boolean expression) {
        if (!expression) {
            throw new IllegalArgumentException();
        }
    }

    public static void require(boolean expression, String errorMessage) {
        if (!expression) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    public static native void emit(Event event);

    public static native String encodeHexString(byte[] data);

    public static native byte[] decodeHex(String data);

}
