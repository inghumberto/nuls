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

    public native BigInteger balance();

    public native void transfer(BigInteger value);

    public native void call(String methodName, String methodDesc, String[] args);

    public void call(String methodName, String[] args) {
        call(methodName, null, args);
    }

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
