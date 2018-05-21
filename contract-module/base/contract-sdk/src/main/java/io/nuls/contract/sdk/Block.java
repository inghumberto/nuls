package io.nuls.contract.sdk;

public final class Block {

    public static native byte[] blockhash(long blockNumber);

    public static native Address coinbase();

    public static native long number();

    public static native long timestamp();

}
