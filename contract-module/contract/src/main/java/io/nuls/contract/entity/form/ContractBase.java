package io.nuls.contract.entity.form;

/**
 * @desription:
 * @author: PierreLuo
 * @date: 2018/4/21
 */
public class ContractBase {
    private String sender;
    private long naLimit;
    private long value;
    private int price;
    private transient String password;
    private String remark;
    private String contractAddress;

    public String getContractAddress() {
        return contractAddress;
    }

    public void setContractAddress(String contractAddress) {
        this.contractAddress = contractAddress;
    }

    //todo world status

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

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
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
