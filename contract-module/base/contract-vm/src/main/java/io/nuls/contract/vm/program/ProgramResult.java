package io.nuls.contract.vm.program;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
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

}
