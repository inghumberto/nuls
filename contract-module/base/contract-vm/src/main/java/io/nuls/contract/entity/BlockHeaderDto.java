package io.nuls.contract.entity;


import io.nuls.core.tools.crypto.Hex;
import io.nuls.kernel.model.BlockHeader;

import java.io.IOException;
import java.io.Serializable;

/**
 * @Desription:
 * @Author: PierreLuo
 * @Date: 2018/5/2
 */
public class BlockHeaderDto implements Serializable {

    private String hash;
    private String preHash;
    private String merkleHash;
    private long time;
    private long height;
    private long txCount;
    //23 bytes
    private byte[] packingAddress;
    private String signature ;
    private byte[] stateRoot;

    public BlockHeaderDto() {}

    public BlockHeaderDto(BlockHeader header) throws IOException {
        this.hash = header.getHash().getDigestHex();
        this.preHash = header.getPreHash().getDigestHex();
        this.merkleHash = header.getMerkleHash().getDigestHex();
        this.time = header.getTime();
        this.height = header.getHeight();
        this.txCount = header.getTxCount();
        this.packingAddress = header.getPackingAddress();
        this.signature = Hex.encode(header.getScriptSig().serialize());
        this.stateRoot = header.getStateRoot();
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getPreHash() {
        return preHash;
    }

    public void setPreHash(String preHash) {
        this.preHash = preHash;
    }

    public String getMerkleHash() {
        return merkleHash;
    }

    public void setMerkleHash(String merkleHash) {
        this.merkleHash = merkleHash;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public long getHeight() {
        return height;
    }

    public void setHeight(long height) {
        this.height = height;
    }

    public long getTxCount() {
        return txCount;
    }

    public void setTxCount(long txCount) {
        this.txCount = txCount;
    }

    public byte[] getPackingAddress() {
        return packingAddress;
    }

    public void setPackingAddress(byte[] packingAddress) {
        this.packingAddress = packingAddress;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public byte[] getStateRoot() {
        return stateRoot;
    }

    public void setStateRoot(byte[] stateRoot) {
        this.stateRoot = stateRoot;
    }
}
