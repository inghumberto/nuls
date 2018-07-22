package io.nuls.contract.vm.program;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Objects;

public class ProgramTransfer {

    private byte[] from;

    private byte[] to;

    private BigInteger value;

    private boolean changeContractBalance;

    public ProgramTransfer(byte[] from, byte[] to, BigInteger value) {
        this.from = from;
        this.to = to;
        this.value = value;
    }

    public byte[] getFrom() {
        return from;
    }

    public byte[] getTo() {
        return to;
    }

    public BigInteger getValue() {
        return value;
    }

    public boolean isChangeContractBalance() {
        return changeContractBalance;
    }

    public void setChangeContractBalance(boolean changeContractBalance) {
        this.changeContractBalance = changeContractBalance;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProgramTransfer that = (ProgramTransfer) o;
        return changeContractBalance == that.changeContractBalance &&
                Arrays.equals(from, that.from) &&
                Arrays.equals(to, that.to) &&
                Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {

        int result = Objects.hash(value, changeContractBalance);
        result = 31 * result + Arrays.hashCode(from);
        result = 31 * result + Arrays.hashCode(to);
        return result;
    }

    @Override
    public String toString() {
        return "ProgramTransfer{" +
                "from=" + Arrays.toString(from) +
                ", to=" + Arrays.toString(to) +
                ", value=" + value +
                ", changeContractBalance=" + changeContractBalance +
                '}';
    }

}
