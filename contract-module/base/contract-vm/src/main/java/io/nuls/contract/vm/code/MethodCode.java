package io.nuls.contract.vm.code;

import org.objectweb.asm.Attribute;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.Arrays;
import java.util.List;

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

    public int getAccess() {
        return access;
    }

    public void setAccess(int access) {
        this.access = access;
    }

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

    public List<String> getExceptions() {
        return exceptions;
    }

    public void setExceptions(List<String> exceptions) {
        this.exceptions = exceptions;
    }

    public List<ParameterNode> getParameters() {
        return parameters;
    }

    public void setParameters(List<ParameterNode> parameters) {
        this.parameters = parameters;
    }

    public List<AnnotationNode> getVisibleAnnotations() {
        return visibleAnnotations;
    }

    public void setVisibleAnnotations(List<AnnotationNode> visibleAnnotations) {
        this.visibleAnnotations = visibleAnnotations;
    }

    public List<AnnotationNode> getInvisibleAnnotations() {
        return invisibleAnnotations;
    }

    public void setInvisibleAnnotations(List<AnnotationNode> invisibleAnnotations) {
        this.invisibleAnnotations = invisibleAnnotations;
    }

    public List<TypeAnnotationNode> getVisibleTypeAnnotations() {
        return visibleTypeAnnotations;
    }

    public void setVisibleTypeAnnotations(List<TypeAnnotationNode> visibleTypeAnnotations) {
        this.visibleTypeAnnotations = visibleTypeAnnotations;
    }

    public List<TypeAnnotationNode> getInvisibleTypeAnnotations() {
        return invisibleTypeAnnotations;
    }

    public void setInvisibleTypeAnnotations(List<TypeAnnotationNode> invisibleTypeAnnotations) {
        this.invisibleTypeAnnotations = invisibleTypeAnnotations;
    }

    public List<Attribute> getAttrs() {
        return attrs;
    }

    public void setAttrs(List<Attribute> attrs) {
        this.attrs = attrs;
    }

    public Object getAnnotationDefault() {
        return annotationDefault;
    }

    public void setAnnotationDefault(Object annotationDefault) {
        this.annotationDefault = annotationDefault;
    }

    public List<AnnotationNode>[] getVisibleParameterAnnotations() {
        return visibleParameterAnnotations;
    }

    public void setVisibleParameterAnnotations(List<AnnotationNode>[] visibleParameterAnnotations) {
        this.visibleParameterAnnotations = visibleParameterAnnotations;
    }

    public List<AnnotationNode>[] getInvisibleParameterAnnotations() {
        return invisibleParameterAnnotations;
    }

    public void setInvisibleParameterAnnotations(List<AnnotationNode>[] invisibleParameterAnnotations) {
        this.invisibleParameterAnnotations = invisibleParameterAnnotations;
    }

    public InsnList getInstructions() {
        return instructions;
    }

    public void setInstructions(InsnList instructions) {
        this.instructions = instructions;
    }

    public List<TryCatchBlockNode> getTryCatchBlocks() {
        return tryCatchBlocks;
    }

    public void setTryCatchBlocks(List<TryCatchBlockNode> tryCatchBlocks) {
        this.tryCatchBlocks = tryCatchBlocks;
    }

    public int getMaxStack() {
        return maxStack;
    }

    public void setMaxStack(int maxStack) {
        this.maxStack = maxStack;
    }

    public int getMaxLocals() {
        return maxLocals;
    }

    public void setMaxLocals(int maxLocals) {
        this.maxLocals = maxLocals;
    }

    public List<LocalVariableCode> getLocalVariables() {
        return localVariables;
    }

    public void setLocalVariables(List<LocalVariableCode> localVariables) {
        this.localVariables = localVariables;
    }

    public List<LocalVariableAnnotationNode> getVisibleLocalVariableAnnotations() {
        return visibleLocalVariableAnnotations;
    }

    public void setVisibleLocalVariableAnnotations(List<LocalVariableAnnotationNode> visibleLocalVariableAnnotations) {
        this.visibleLocalVariableAnnotations = visibleLocalVariableAnnotations;
    }

    public List<LocalVariableAnnotationNode> getInvisibleLocalVariableAnnotations() {
        return invisibleLocalVariableAnnotations;
    }

    public void setInvisibleLocalVariableAnnotations(List<LocalVariableAnnotationNode> invisibleLocalVariableAnnotations) {
        this.invisibleLocalVariableAnnotations = invisibleLocalVariableAnnotations;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public VariableType getReturnVariableType() {
        return returnVariableType;
    }

    public void setReturnVariableType(VariableType returnVariableType) {
        this.returnVariableType = returnVariableType;
    }

    public List<VariableType> getArgsVariableType() {
        return argsVariableType;
    }

    public void setArgsVariableType(List<VariableType> argsVariableType) {
        this.argsVariableType = argsVariableType;
    }

    @Override
    public String toString() {
        return "MethodCode{" +
                "access=" + access +
                ", name=" + name +
                ", desc=" + desc +
                ", signature=" + signature +
                ", exceptions=" + exceptions +
                ", parameters=" + parameters +
                ", visibleAnnotations=" + visibleAnnotations +
                ", invisibleAnnotations=" + invisibleAnnotations +
                ", visibleTypeAnnotations=" + visibleTypeAnnotations +
                ", invisibleTypeAnnotations=" + invisibleTypeAnnotations +
                ", attrs=" + attrs +
                ", annotationDefault=" + annotationDefault +
                ", visibleParameterAnnotations=" + Arrays.toString(visibleParameterAnnotations) +
                ", invisibleParameterAnnotations=" + Arrays.toString(invisibleParameterAnnotations) +
                ", instructions=" + instructions +
                ", tryCatchBlocks=" + tryCatchBlocks +
                ", maxStack=" + maxStack +
                ", maxLocals=" + maxLocals +
                ", localVariables=" + localVariables +
                ", visibleLocalVariableAnnotations=" + visibleLocalVariableAnnotations +
                ", invisibleLocalVariableAnnotations=" + invisibleLocalVariableAnnotations +
                ", className=" + className +
                ", returnVariableType=" + returnVariableType +
                ", argsVariableType=" + argsVariableType +
                '}';
    }

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
