package io.nuls.contract.vm.code;

import lombok.Builder;
import lombok.Getter;
import org.objectweb.asm.tree.LabelNode;

@Getter
@Builder
public class LocalVariableCode {

    private String name;

    private String desc;

    private String signature;

    private LabelNode start;

    private LabelNode end;

    private int index;

    private VariableType variableType;

}
