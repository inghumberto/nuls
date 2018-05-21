package io.nuls.contract.sdk;

import java.math.BigInteger;

public class Msg {

    public static native long gas();

    public static native Address sender();

    public static native BigInteger value();

    public static native long gasprice();

    public static native Address address();

}
