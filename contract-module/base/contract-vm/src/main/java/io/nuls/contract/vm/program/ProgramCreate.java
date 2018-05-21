package io.nuls.contract.vm.program;

import lombok.Data;

import java.math.BigInteger;

@Data
public class ProgramCreate {

    /**
     * 当前块编号
     */
    private long number;

    /**
     * 创建者
     */
    private byte[] sender;

    /**
     * 交易附带的货币量
     */
    private BigInteger value;

    /**
     * 最大Na消耗
     */
    private long naLimit;

    /**
     * 执行合约单价
     */
    private long price;

    /**
     * 合约地址
     */
    private byte[] contractAddress;

    /**
     * 合约代码
     */
    private byte[] contractCode;

    /**
     * 参数列表
     */
    private String[] args;

    public void args(String... args) {
        this.args = args;
    }

}
