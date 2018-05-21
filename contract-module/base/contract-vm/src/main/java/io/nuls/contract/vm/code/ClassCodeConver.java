package io.nuls.contract.vm.code;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ClassCodeConver {

    public static ClassCode toClassCode(ClassNode classNode) {
        return ClassCode.builder()
                .version(classNode.version)
                .access(classNode.access)
                .name(classNode.name)
                .signature(classNode.signature)
                .superName(classNode.superName)
                .interfaces(classNode.interfaces != null ? classNode.interfaces : new ArrayList<>())
                .sourceFile(classNode.sourceFile)
                .sourceDebug(classNode.sourceDebug)
                .module(classNode.module)
                .outerClass(classNode.outerClass)
                .outerMethod(classNode.outerMethod)
                .outerMethodDesc(classNode.outerMethodDesc)
                .visibleAnnotations(classNode.visibleAnnotations)
                .invisibleAnnotations(classNode.invisibleAnnotations)
                .visibleTypeAnnotations(classNode.visibleTypeAnnotations)
                .invisibleTypeAnnotations(classNode.invisibleTypeAnnotations)
                .attrs(classNode.attrs)
                .innerClasses(classNode.innerClasses != null ? classNode.innerClasses : new ArrayList<>())
                .fields(toFieldCodes(classNode.fields))
                .methods(toMethodCodes(classNode.name, classNode.methods))
                .variableType(VariableType.valueOf(classNode.name))
                .build();
    }

    public static FieldCode toFieldCode(FieldNode fieldNode) {
        return FieldCode.builder()
                .access(fieldNode.access)
                .name(fieldNode.name)
                .desc(fieldNode.desc)
                .signature(fieldNode.signature)
                .value(fieldNode.value)
                .visibleAnnotations(fieldNode.visibleAnnotations)
                .invisibleAnnotations(fieldNode.invisibleAnnotations)
                .visibleTypeAnnotations(fieldNode.visibleTypeAnnotations)
                .invisibleTypeAnnotations(fieldNode.invisibleTypeAnnotations)
                .attrs(fieldNode.attrs)
                .variableType(VariableType.valueOf(fieldNode.desc))
                .build();
    }

    public static List<FieldCode> toFieldCodes(List<FieldNode> fieldNodes) {
        return fieldNodes == null ? new ArrayList<>() : fieldNodes.stream().map(ClassCodeConver::toFieldCode).collect(Collectors.toList());
    }

    public static MethodCode toMethodCode(String className, MethodNode methodNode) {
        return MethodCode.builder()
                .access(methodNode.access)
                .name(methodNode.name)
                .desc(methodNode.desc)
                .signature(methodNode.signature)
                .exceptions(methodNode.exceptions)
                .parameters(methodNode.parameters)
                .visibleAnnotations(methodNode.visibleAnnotations)
                .invisibleAnnotations(methodNode.invisibleAnnotations)
                .visibleTypeAnnotations(methodNode.visibleTypeAnnotations)
                .invisibleTypeAnnotations(methodNode.invisibleTypeAnnotations)
                .attrs(methodNode.attrs)
                .annotationDefault(methodNode.annotationDefault)
                .visibleParameterAnnotations(methodNode.visibleParameterAnnotations)
                .invisibleParameterAnnotations(methodNode.invisibleParameterAnnotations)
                .instructions(methodNode.instructions)
                .tryCatchBlocks(methodNode.tryCatchBlocks != null ? methodNode.tryCatchBlocks : new ArrayList<>())
                .maxStack(methodNode.maxStack)
                .maxLocals(methodNode.maxLocals)
                .localVariables(toLocalVariableCodes(methodNode.localVariables))
                .visibleLocalVariableAnnotations(methodNode.visibleLocalVariableAnnotations)
                .invisibleLocalVariableAnnotations(methodNode.invisibleLocalVariableAnnotations)
                //.visited(methodNode.visited)
                .className(className)
                .returnVariableType(VariableType.parseReturn(methodNode.desc))
                .argsVariableType(VariableType.parseArgs(methodNode.desc))
                .build();
    }

    public static List<MethodCode> toMethodCodes(String className, List<MethodNode> methodNodes) {
        return methodNodes == null ? new ArrayList<>() : methodNodes.stream().map(methodNode -> toMethodCode(className, methodNode)).collect(Collectors.toList());
    }

    public static LocalVariableCode toLocalVariableCode(LocalVariableNode localVariableNode) {
        return LocalVariableCode.builder()
                .name(localVariableNode.name)
                .desc(localVariableNode.desc)
                .signature(localVariableNode.signature)
                .start(localVariableNode.start)
                .end(localVariableNode.end)
                .index(localVariableNode.index)
                .variableType(VariableType.valueOf(localVariableNode.desc))
                .build();
    }

    public static List<LocalVariableCode> toLocalVariableCodes(List<LocalVariableNode> localVariableNodes) {
        return localVariableNodes == null ? new ArrayList<>() : localVariableNodes.stream().map(ClassCodeConver::toLocalVariableCode).collect(Collectors.toList());
    }

}
