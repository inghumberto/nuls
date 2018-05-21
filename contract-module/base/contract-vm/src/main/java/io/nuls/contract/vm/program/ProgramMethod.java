package io.nuls.contract.vm.program;

import lombok.Data;

import java.util.List;

@Data
public class ProgramMethod {

    private String name;

    private String desc;

    private List<String> args;

    private String returnArg;

}
