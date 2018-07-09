package io.nuls.contract.sdk;

public class Utils {

    private Utils() {
    }

    /**
     * 检查条件，如果条件不满足则回滚
     *
     * @param expression
     */
    public static void require(boolean expression) {
        if (!expression) {
            revert();
        }
    }

    /**
     * 检查条件，如果条件不满足则回滚
     *
     * @param expression
     * @param errorMessage
     */
    public static void require(boolean expression, String errorMessage) {
        if (!expression) {
            revert(errorMessage);
        }
    }

    /**
     * 终止执行并还原改变的状态
     */
    public static void revert() {
        revert(null);
    }

    /**
     * 终止执行并还原改变的状态
     *
     * @param errorMessage
     */
    public static native void revert(String errorMessage);

    /**
     * 发送事件
     *
     * @param event
     */
    public static native void emit(Event event);

    /**
     * byte数组转为16进制字符串
     *
     * @param data
     * @return
     */
    public static native String encodeHexString(byte[] data);

    /**
     * 16进制字符串转为byte数组
     *
     * @param data
     * @return
     */
    public static native byte[] decodeHex(String data);

}
