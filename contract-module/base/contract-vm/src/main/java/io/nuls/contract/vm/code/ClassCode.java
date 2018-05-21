package io.nuls.contract.vm.code;

import lombok.Builder;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.InnerClassNode;
import org.objectweb.asm.tree.ModuleNode;
import org.objectweb.asm.tree.TypeAnnotationNode;

import java.util.List;
import java.util.Objects;

@Getter
@Builder
public class ClassCode {

    private int version;

    private int access;

    private String name;

    private String signature;

    private String superName;

    private List<String> interfaces;

    private String sourceFile;

    private String sourceDebug;

    private ModuleNode module;

    private String outerClass;

    private String outerMethod;

    private String outerMethodDesc;

    private List<AnnotationNode> visibleAnnotations;

    private List<AnnotationNode> invisibleAnnotations;

    private List<TypeAnnotationNode> visibleTypeAnnotations;

    private List<TypeAnnotationNode> invisibleTypeAnnotations;

    private List<Attribute> attrs;

    private List<InnerClassNode> innerClasses;

    private List<FieldCode> fields;

    private List<MethodCode> methods;

    private VariableType variableType;

    public MethodCode getMethodCode(String methodName, String methodDesc) {
        if (StringUtils.isEmpty(methodDesc)) {
            return getMethodCode(methodName);
        }
        return this.methods.stream().filter(methodCode ->
                Objects.equals(methodCode.getName(), methodName) &&
                        Objects.equals(methodCode.getDesc(), methodDesc)
        ).findFirst().orElse(null);
    }

    public MethodCode getMethodCode(String methodName) {
        return this.methods.stream().filter(methodCode ->
                Objects.equals(methodCode.getName(), methodName)
        ).findFirst().orElse(null);
    }

    public boolean isInterface() {
        return (access & Opcodes.ACC_INTERFACE) != 0;
    }

}
