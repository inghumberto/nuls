package io.nuls.contract.vm.code;

import org.objectweb.asm.tree.LabelNode;

public class LocalVariableCode {

    private String name;

    private String desc;

    private String signature;

    private LabelNode start;

    private LabelNode end;

    private int index;

    private VariableType variableType;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public LabelNode getStart() {
        return start;
    }

    public void setStart(LabelNode start) {
        this.start = start;
    }

    public LabelNode getEnd() {
        return end;
    }

    public void setEnd(LabelNode end) {
        this.end = end;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public VariableType getVariableType() {
        return variableType;
    }

    public void setVariableType(VariableType variableType) {
        this.variableType = variableType;
    }

}
