package io.nuls.contract.vm.program;

import java.math.BigInteger;
import java.util.Arrays;

public class ProgramTransfer {

    private byte[] from;

    private byte[] to;

    private BigInteger value;

    private long gasUsed;

    public ProgramTransfer() {
    }

    public byte[] getFrom() {
        return from;
    }

    public void setFrom(byte[] from) {
        this.from = from;
    }

    public byte[] getTo() {
        return to;
    }

    public void setTo(byte[] to) {
        this.to = to;
    }

    public BigInteger getValue() {
        return value;
    }

    public void setValue(BigInteger value) {
        this.value = value;
    }

    public long getGasUsed() {
        return gasUsed;
    }

    public void setGasUsed(long gasUsed) {
        this.gasUsed = gasUsed;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ProgramTransfer that = (ProgramTransfer) o;

        if (gasUsed != that.gasUsed) return false;
        if (!Arrays.equals(from, that.from)) return false;
        if (!Arrays.equals(to, that.to)) return false;
        return value != null ? value.equals(that.value) : that.value == null;
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(from);
        result = 31 * result + Arrays.hashCode(to);
        result = 31 * result + (value != null ? value.hashCode() : 0);
        result = 31 * result + (int) (gasUsed ^ (gasUsed >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "ProgramTransfer{" +
                "from=" + Arrays.toString(from) +
                ", to=" + Arrays.toString(to) +
                ", value=" + value +
                ", gasUsed=" + gasUsed +
                '}';
    }

}
