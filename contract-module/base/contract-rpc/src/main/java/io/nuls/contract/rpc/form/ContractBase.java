package io.nuls.contract.rpc.form;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * @desription:
 * @author: PierreLuo
 * @date: 2018/4/21
 */
@ApiModel(value = "智能合约表单数据")
public class ContractBase {
    @ApiModelProperty(name = "sender", value = "交易创建者", required = true)
    private String sender;
    @ApiModelProperty(name = "naLimit", value = "最大Na消耗", required = true)
    private long naLimit;
    @ApiModelProperty(name = "value", value = "交易附带的货币量", required = false)
    private long value;
    @ApiModelProperty(name = "price", value = "执行合约单价", required = true)
    private byte price;
    @ApiModelProperty(name = "password", value = "钱包密码", required = true)
    private String password;
    @ApiModelProperty(name = "remark", value = "备注", required = false)
    private String remark;
    @ApiModelProperty(name = "contractAddress", value = "智能合约地址", required = true)
    private String contractAddress;

    public String getContractAddress() {
        return contractAddress;
    }

    public void setContractAddress(String contractAddress) {
        this.contractAddress = contractAddress;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public long getNaLimit() {
        return naLimit;
    }

    public void setNaLimit(long naLimit) {
        this.naLimit = naLimit;
    }

    public long getValue() {
        return value;
    }

    public void setValue(long value) {
        this.value = value;
    }

    public byte getPrice() {
        return price;
    }

    public void setPrice(byte price) {
        this.price = price;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
