package io.nuls.contract.vm.program.impl;

import io.nuls.contract.vm.ObjectRef;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ProgramContext {

    private ObjectRef address;

    private ObjectRef sender;

    //private ObjectRef balance;

    private long gasPrice;

    private long gas;

    //private long gasLimit;

    private ObjectRef value;

    private ObjectRef coinbase;

    private long timestamp;

    private long number;

    //private long difficulty;

    //private ObjectRef data;

}
