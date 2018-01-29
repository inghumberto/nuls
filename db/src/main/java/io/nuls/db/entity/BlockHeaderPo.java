/**
 * MIT License
 *
 * Copyright (c) 2017-2018 nuls.io
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package io.nuls.db.entity;

/**
 * @author Niels
 */
public class BlockHeaderPo {

    /**
     *
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column block.hash
     *
     * @mbg.generated
     */
    private String hash;

    /**
     *
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column block.height
     *
     * @mbg.generated
     */
    private Long height;

    /**
     *
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column block.pre_hash
     *
     * @mbg.generated
     */
    private String preHash;

    /**
     *
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column block.merkle_hash
     *
     * @mbg.generated
     */
    private String merkleHash;

    /**
     *
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column block.create_time
     *
     * @mbg.generated
     */
    private Long createTime;

    /**
     *
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column block.consensus_address
     *
     * @mbg.generated
     */
    private String consensusAddress;

    /**
     *
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column block.varsion
     *
     * @mbg.generated
     */
    private Integer version;

    /**
     *
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column block.txCount
     *
     * @mbg.generated
     */
    private Long txCount;

    /**
     *
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column block.sign
     *
     * @mbg.generated
     */
    private byte[] sign;

    private long roundIndex;

    private byte[] extend;

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column block.hash
     *
     * @return the value of block.hash
     *
     * @mbg.generated
     */
    public String getHash() {
        return hash;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column block.hash
     *
     * @param hash the value for block.hash
     *
     * @mbg.generated
     */
    public void setHash(String hash) {
        this.hash = hash == null ? null : hash.trim();
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column block.height
     *
     * @return the value of block.height
     *
     * @mbg.generated
     */
    public Long getHeight() {
        return height;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column block.height
     *
     * @param height the value for block.height
     *
     * @mbg.generated
     */
    public void setHeight(Long height) {
        this.height = height;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column block.pre_hash
     *
     * @return the value of block.pre_hash
     *
     * @mbg.generated
     */
    public String getPreHash() {
        return preHash;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column block.pre_hash
     *
     * @param preHash the value for block.pre_hash
     *
     * @mbg.generated
     */
    public void setPreHash(String preHash) {
        this.preHash = preHash == null ? null : preHash.trim();
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column block.merkle_hash
     *
     * @return the value of block.merkle_hash
     *
     * @mbg.generated
     */
    public String getMerkleHash() {
        return merkleHash;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column block.merkle_hash
     *
     * @param merkleHash the value for block.merkle_hash
     *
     * @mbg.generated
     */
    public void setMerkleHash(String merkleHash) {
        this.merkleHash = merkleHash == null ? null : merkleHash.trim();
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column block.create_time
     *
     * @return the value of block.create_time
     *
     * @mbg.generated
     */
    public Long getCreateTime() {
        return createTime;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column block.create_time
     *
     * @param createTime the value for block.create_time
     *
     * @mbg.generated
     */
    public void setCreateTime(Long createTime) {
        this.createTime = createTime;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column block.consensus_address
     *
     * @return the value of block.consensus_address
     *
     * @mbg.generated
     */
    public String getConsensusAddress() {
        return consensusAddress;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column block.consensus_address
     *
     * @param consensusAddress the value for block.consensus_address
     *
     * @mbg.generated
     */
    public void setConsensusAddress(String consensusAddress) {
        this.consensusAddress = consensusAddress == null ? null : consensusAddress.trim();
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column block.varsion
     *
     * @return the value of block.varsion
     *
     * @mbg.generated
     */
    public Integer getVersion() {
        return version;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column block.varsion
     *
     * @param varsion the value for block.varsion
     *
     * @mbg.generated
     */
    public void setVersion(Integer varsion) {
        this.version = varsion;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column block.txCount
     *
     * @return the value of block.txCount
     *
     * @mbg.generated
     */
    public Long getTxCount() {
        return txCount;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column block.txCount
     *
     * @param txCount the value for block.txCount
     *
     * @mbg.generated
     */
    public void setTxCount(Long txCount) {
        this.txCount = txCount;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column block.sign
     *
     * @return the value of block.sign
     *
     * @mbg.generated
     */
    public byte[] getSign() {
        return sign;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column block.sign
     *
     * @param sign the value for block.sign
     *
     * @mbg.generated
     */
    public void setSign(byte[] sign) {
        this.sign = sign;
    }

    public long getRoundIndex() {
        return roundIndex;
    }

    public void setRoundIndex(long roundIndex) {
        this.roundIndex = roundIndex;
    }

    public byte[] getExtend() {
        return extend;
    }

    public void setExtend(byte[] extend) {
        this.extend = extend;
    }
}
