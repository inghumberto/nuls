package io.nuls.contract.vm.program;

import lombok.Data;

import java.math.BigInteger;

@Data
public class ProgramCall {

    /**
     * 当前块编号
     */
    private long number;

    /**
     * 调用者
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
     * 方法名
     */
    private String methodName;

    /**
     * 方法签名，如果方法名不重复，可以不传
     */
    private String methodDesc;

    /**
     * 参数列表
     */
    private String[] args;

    public void args(String... args) {
        this.args = args;
    }

}
