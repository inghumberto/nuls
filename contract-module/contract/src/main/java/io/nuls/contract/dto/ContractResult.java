package io.nuls.contract.dto;

/**
 * @desription:
 * @author: PierreLuo
 * @date: 2018/4/20
 */
public class ContractResult {
    /**
     * 合约执行结果
     */
    private String result;
    /**
     * 已使用Gas
     */
    private long gasUsed;
    /**
     * 状态根
     */
    private byte[] stateRoot;

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public byte[] getStateRoot() {
        return stateRoot;
    }

    public void setStateRoot(byte[] stateRoot) {
        this.stateRoot = stateRoot;
    }

    public long getGasUsed() {
        return gasUsed;
    }

    public void setGasUsed(long gasUsed) {
        this.gasUsed = gasUsed;
    }

}
