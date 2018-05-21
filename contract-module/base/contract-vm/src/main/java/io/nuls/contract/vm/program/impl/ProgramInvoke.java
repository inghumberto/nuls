package io.nuls.contract.vm.program.impl;

import lombok.Builder;
import lombok.Getter;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.math.BigInteger;

@Getter
@Builder
public class ProgramInvoke {

    /**
     * 合约地址
     * 创建合约时候需要传入生成的新地址
     */
    @NotNull
    private byte[] address;

    /**
     * 交易发起者地址
     */
    @NotNull
    private byte[] sender;

    /**
     * 合约余额
     */
    //@NotNull
    //@Min(0)
    //private BigInteger balance;

    /**
     * 交易发起者配置的gas价格
     */
    @Min(0)
    private long gasPrice;

    /**
     * 交易发起者提供的gas
     */
    @Min(0)
    private long gas;

    /**
     * 当前块gas限量
     */
    //@Min(0)
    //private long gasLimit;

    /**
     * 交易附带的货币量
     */
    @Min(0)
    private BigInteger value;

    /**
     * 随机数
     */
    //@Min(0)
    //private BigInteger nonce;

    /**
     * 上一个区块hash
     */
    //@NotNull
    //private byte[] prevHash;

    /**
     * 上一个区块状态根
     */
    //@NotNull
    //private byte[] prevStateRoot;

    /**
     * 当前块旷工地址
     */
    //@NotNull
    //private byte[] coinbase;

    /**
     * 当前块的时间戳
     */
    //@Min(0)
    //private long timestamp;

    /**
     * 当前块编号
     */
    @Min(0)
    private long number;

    /**
     * 当前块的难度系数
     */
    //private long difficulty;

    /**
     * 合约创建时候，传入代码
     */
    private byte[] data;

    /**
     * 调用方法名
     */
    private String methodName;

    /**
     * 调用方法签名
     */
    private String methodDesc;

    /**
     * 调用方法参数
     */
    private String[] args;

    public void args(String... args) {
        this.args = args;
    }

}
