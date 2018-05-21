package io.nuls.contract.vm.program;

import lombok.Data;

import java.math.BigInteger;

@Data
public class ProgramTransfer {

    private byte[] from;

    private byte[] to;

    private BigInteger value;

}
