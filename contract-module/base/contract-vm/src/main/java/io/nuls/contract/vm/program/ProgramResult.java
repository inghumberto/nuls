package io.nuls.contract.vm.program;

import java.util.ArrayList;
import java.util.List;

public class ProgramResult {

    private long gasUsed;

    private String result;

    private boolean error;

    private String errorMessage;

    private List<ProgramTransfer> transfers = new ArrayList<>();

    public ProgramResult error(String errorMessage) {
        this.error = true;
        this.errorMessage = errorMessage;
        return this;
    }

    public ProgramResult() {
    }

    public long getGasUsed() {
        return gasUsed;
    }

    public void setGasUsed(long gasUsed) {
        this.gasUsed = gasUsed;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public boolean isError() {
        return error;
    }

    public void setError(boolean error) {
        this.error = error;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public List<ProgramTransfer> getTransfers() {
        return transfers;
    }

    public void setTransfers(List<ProgramTransfer> transfers) {
        this.transfers = transfers;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ProgramResult that = (ProgramResult) o;

        if (gasUsed != that.gasUsed) return false;
        if (error != that.error) return false;
        if (result != null ? !result.equals(that.result) : that.result != null) return false;
        if (errorMessage != null ? !errorMessage.equals(that.errorMessage) : that.errorMessage != null) return false;
        return transfers != null ? transfers.equals(that.transfers) : that.transfers == null;
    }

    @Override
    public int hashCode() {
        int result1 = (int) (gasUsed ^ (gasUsed >>> 32));
        result1 = 31 * result1 + (result != null ? result.hashCode() : 0);
        result1 = 31 * result1 + (error ? 1 : 0);
        result1 = 31 * result1 + (errorMessage != null ? errorMessage.hashCode() : 0);
        result1 = 31 * result1 + (transfers != null ? transfers.hashCode() : 0);
        return result1;
    }

    @Override
    public String toString() {
        return "ProgramResult{" +
                "gasUsed=" + gasUsed +
                ", result=" + result +
                ", error=" + error +
                ", errorMessage=" + errorMessage +
                ", transfers=" + transfers +
                '}';
    }

}
