package io.nuls.contract.sdk;

import java.math.BigInteger;
import java.util.Arrays;

public class Address {

    private final byte[] value;

    public Address(byte[] value) {
        this.value = value;
    }

    public Address(String value) {
        this.value = Utils.decodeHex(value);
    }

    public byte[] getValue() {
        return value;
    }

    /**
     * 获取该地址的余额（只能获取合约地址余额）
     *
     * @return
     */
    public native BigInteger balance();

    /**
     * 合约向该地址转账（不能转给合约地址）
     *
     * @param value 转账金额（多少Na）
     */
    public native void transfer(BigInteger value);

    /**
     * 调用该地址的合约方法
     *
     * @param methodName 方法名
     * @param methodDesc 方法签名
     * @param args       参数
     * @param value      附带的货币量（多少Na）
     */
    public native void call(String methodName, String methodDesc, String[] args, BigInteger value);

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Address address = (Address) o;
        return Arrays.equals(value, address.value);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(value);
    }

    @Override
    public String toString() {
        return Utils.encodeHexString(value);
    }

}
