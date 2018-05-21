package io.nuls.contract.vm.code;

import lombok.Builder;
import lombok.Getter;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.TypeAnnotationNode;

import java.util.List;

@Getter
@Builder
public class FieldCode {

    private int access;

    private String name;

    private String desc;

    private String signature;

    private Object value;

    private List<AnnotationNode> visibleAnnotations;

    private List<AnnotationNode> invisibleAnnotations;

    private List<TypeAnnotationNode> visibleTypeAnnotations;

    private List<TypeAnnotationNode> invisibleTypeAnnotations;

    private List<Attribute> attrs;

    private VariableType variableType;

    public boolean isStatic() {
        return (access & Opcodes.ACC_STATIC) != 0;
    }

    public boolean isNotStatic() {
        return !isStatic();
    }

    public boolean isFinal() {
        return (access & Opcodes.ACC_FINAL) != 0;
    }

    public boolean isNotFinal() {
        return !isFinal();
    }

}
