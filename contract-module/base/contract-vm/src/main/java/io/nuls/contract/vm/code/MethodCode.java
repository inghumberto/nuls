package io.nuls.contract.vm.code;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.List;

@Getter
@Builder
@ToString
public class MethodCode {

    private int access;

    private String name;

    private String desc;

    private String signature;

    private List<String> exceptions;

    private List<ParameterNode> parameters;

    private List<AnnotationNode> visibleAnnotations;

    private List<AnnotationNode> invisibleAnnotations;

    private List<TypeAnnotationNode> visibleTypeAnnotations;

    private List<TypeAnnotationNode> invisibleTypeAnnotations;

    private List<Attribute> attrs;

    private Object annotationDefault;

    private List<AnnotationNode>[] visibleParameterAnnotations;

    private List<AnnotationNode>[] invisibleParameterAnnotations;

    private InsnList instructions;

    private List<TryCatchBlockNode> tryCatchBlocks;

    private int maxStack;

    private int maxLocals;

    private List<LocalVariableCode> localVariables;

    private List<LocalVariableAnnotationNode> visibleLocalVariableAnnotations;

    private List<LocalVariableAnnotationNode> invisibleLocalVariableAnnotations;

    //private boolean visited;

    private String className;

    private VariableType returnVariableType;

    private List<VariableType> argsVariableType;

    public boolean isPublic() {
        return (access & Opcodes.ACC_PUBLIC) != 0;
    }

    public boolean isStatic() {
        return (access & Opcodes.ACC_STATIC) != 0;
    }

    public boolean isAbstract() {
        return (access & Opcodes.ACC_ABSTRACT) != 0;
    }

    public boolean isNotAbstract() {
        return !isAbstract();
    }

}
