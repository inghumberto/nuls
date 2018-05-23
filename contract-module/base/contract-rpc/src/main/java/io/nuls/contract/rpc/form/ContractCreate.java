package io.nuls.contract.rpc.form;

/**
 * @desription:
 * @author: PierreLuo
 * @date: 2018/4/20
 */
public class ContractCreate extends ContractBase {

    private byte[] contractCode;
    private String[] args;

    public byte[] getContractCode() {
        return contractCode;
    }

    public void setContractCode(byte[] contractCode) {
        this.contractCode = contractCode;
    }

    public String[] getArgs() {
        return args;
    }

    public void setArgs(String[] args) {
        this.args = args;
    }

    public void args(String... args) {
        this.args = args;
    }

}
