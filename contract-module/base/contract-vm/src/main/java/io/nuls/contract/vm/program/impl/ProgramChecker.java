package io.nuls.contract.vm.program.impl;

import io.nuls.contract.vm.OpCode;
import io.nuls.contract.vm.code.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InnerClassNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class ProgramChecker {

    private static final Logger log = LoggerFactory.getLogger(ProgramExecutorImpl.class);

    public static void check(List<ClassCode> classCodes) {
        checkJdkVersion(classCodes);
        checkContractNum(classCodes);
        //checkStaticField(classCodes);
        checkClass(classCodes);
        checkMethod(classCodes);
        checkOpCode(classCodes);
    }

    public static void checkJdkVersion(List<ClassCode> classCodes) {
        for (ClassCode classCode : classCodes) {
            if (!classCode.isV1_6() && !classCode.isV1_8()) {
                throw new RuntimeException("class version must be 1.6 or 1.8");
            }
        }
    }

    public static void checkContractNum(List<ClassCode> classCodes) {
        List<ClassCode> contractClassCodes = classCodes.stream()
                .filter(classCode -> classCode.getInterfaces().contains(ProgramConstants.CONTRACT_INTERFACE_NAME)
                        || ProgramConstants.CONTRACT_INTERFACE_NAME.equals(classCode.getSuperName()))
                .collect(Collectors.toList());
        int contractCount = contractClassCodes.size();
        if (contractCount != 1) {
            throw new RuntimeException(String.format("find %s contracts", contractCount));
        }
    }

    public static void checkStaticField(List<ClassCode> classCodes) {
        List<FieldCode> fieldCodes = new ArrayList<>();
        classCodes.stream().forEach(classCode -> {
            List<FieldCode> list = classCode.getFields().stream().filter(FieldCode::isStatic).collect(Collectors.toList());
            fieldCodes.addAll(list);
        });
        if (fieldCodes.size() > 0) {
            throw new RuntimeException(String.format("find %s static fields", fieldCodes.size()));
        }
    }

    public static void checkClass(List<ClassCode> classCodes) {
        Set<String> allClass = allClass(classCodes);
        Set<String> classCodeNames = classCodes.stream().map(ClassCode::getName).collect(Collectors.toSet());
        Collection<String> classes = CollectionUtils.removeAll(allClass, classCodeNames);
        Collection<String> classes1 = CollectionUtils.removeAll(classes, Arrays.asList(ProgramConstants.SDK_CLASS_NAMES));
        Collection<String> classes2 = CollectionUtils.removeAll(classes1, Arrays.asList(ProgramConstants.CONTRACT_USED_CLASS_NAMES));
        System.out.println();
    }

    public static void checkMethod(List<ClassCode> classCodes) {
        Map<String, ClassCode> classCodeMap = new LinkedHashMap<>();
        classCodeMap.putAll(ClassCodeLoader.RESOURCE_CLASS_CODES);
        for (ClassCode classCode : classCodes) {
            classCodeMap.put(classCode.getName(), classCode);
        }
        for (ClassCode classCode : classCodes) {
            for (MethodCode methodCode : classCode.getMethods()) {
                Set<MethodCode> methodCodes = new LinkedHashSet<>();
                Object o = isSupportMethod(methodCode, methodCodes, classCodeMap);
                if (!Boolean.TRUE.equals(o)) {
                    MethodInsnNode methodInsnNode = (MethodInsnNode) o;
                    throw new RuntimeException(String.format("can't use method: %s.%s%s", methodInsnNode.owner, methodInsnNode.name, methodInsnNode.desc));
                }
            }
        }
    }

    public static void checkOpCode(List<ClassCode> classCodes) {
        for (ClassCode classCode : classCodes) {
            for (MethodCode methodCode : classCode.getMethods()) {
                checkOpCode(methodCode);
            }
        }
    }

    public static void checkOpCode(MethodCode methodCode) {
        ListIterator<AbstractInsnNode> listIterator = methodCode.getInstructions().iterator();
        while (listIterator.hasNext()) {
            AbstractInsnNode abstractInsnNode = listIterator.next();
            if (abstractInsnNode != null && abstractInsnNode.getOpcode() > 0) {
                OpCode opCode = OpCode.valueOf(abstractInsnNode.getOpcode());
                boolean nonsupport = false;
                if (opCode == null) {
                    nonsupport = true;
                } else {
                    switch (opCode) {
                        case JSR:
                        case RET:
                        case INVOKEDYNAMIC:
                        case MONITORENTER:
                        case MONITOREXIT:
                            nonsupport = true;
                            break;
                        default:
                            break;
                    }
                }
                if (nonsupport) {
                    int line = getLine(abstractInsnNode);
                    throw new RuntimeException(String.format("nonsupport opcode: class(%s), line(%d)", methodCode.getClassCode().getName(), line));
                }
            }
        }
    }

    public static Set<String> allClass(Collection<ClassCode> classCodes) {
        Set<String> set = new LinkedHashSet<>();
        for (ClassCode classCode : classCodes) {
            set.add(classCode.getName());
            set.add(classCode.getSuperName());
            set.addAll(classCode.getInterfaces());
            for (InnerClassNode innerClassNode : classCode.getInnerClasses()) {
                set.add(innerClassNode.name);
            }
            for (FieldCode fieldCode : classCode.getFields()) {
                set.add(fieldCode.getDesc());
            }
            for (MethodCode methodCode : classCode.getMethods()) {
                set.addAll(allClass(methodCode));
            }
        }

        Set<String> classes = new LinkedHashSet<>();
        for (String s : set) {
            if (s == null) {
                continue;
            }
            if (s.contains("$")) {
                //continue;
            }
            if (s.contains("(")) {
                List<VariableType> list = VariableType.parseAll(s);
                for (VariableType variableType : list) {
                    if (!variableType.isPrimitiveType() && variableType.isNotVoid()) {
                        classes.add(variableType.getType());
                    }
                }
            } else {
                VariableType variableType = VariableType.valueOf(s);
                if (!variableType.isPrimitiveType() && variableType.isNotVoid()) {
                    classes.add(variableType.getType());
                }
            }
        }
        return classes;
    }

    public static Set<String> allClass(MethodCode methodCode) {
        Set<String> set = new LinkedHashSet<>();
        ListIterator<AbstractInsnNode> listIterator = methodCode.getInstructions().iterator();
        while (listIterator.hasNext()) {
            AbstractInsnNode abstractInsnNode = listIterator.next();
            String desc = null;
            try {
                desc = (String) FieldUtils.readField(abstractInsnNode, "desc");
            } catch (Exception e) {
                //ignore
            }
            set.add(desc);
            String owner = null;
            try {
                owner = (String) FieldUtils.readField(abstractInsnNode, "owner");
            } catch (Exception e) {
                //ignore
            }
            set.add(owner);
        }
        return set;
    }

    public static Object isSupportMethod(MethodCode methodCode, Set<MethodCode> methodCodes, Map<String, ClassCode> classCodeMap) {
        if (!methodCodes.contains(methodCode)) {
            methodCodes.add(methodCode);
            ListIterator<AbstractInsnNode> listIterator = methodCode.getInstructions().iterator();
            while (listIterator.hasNext()) {
                AbstractInsnNode abstractInsnNode = listIterator.next();
                if (!(abstractInsnNode instanceof MethodInsnNode)) {
                    continue;
                }
                MethodInsnNode methodInsnNode = (MethodInsnNode) abstractInsnNode;
                VariableType variableType = VariableType.valueOf(methodInsnNode.owner);
                ClassCode classCode = classCodeMap.get(variableType.getType());
                if (classCode == null) {
                    log.warn("can't find " + methodInsnNode.owner);
                    return methodInsnNode;
                }
                MethodCode methodCode1 = getMethodCode(classCode, methodInsnNode.name, methodInsnNode.desc, classCodeMap);
                if (methodCode1 == null) {
                    log.warn(String.format("can't find %s.%s%s", methodInsnNode.owner, methodInsnNode.name, methodInsnNode.desc));
                    return methodInsnNode;
                }
                Object o = isSupportMethod(methodCode1, methodCodes, classCodeMap);
                if (!Boolean.TRUE.equals(o)) {
                    log.warn(String.format("not support %s.%s%s", methodInsnNode.owner, methodInsnNode.name, methodInsnNode.desc));
                    return methodInsnNode;
                }
            }
        }
        return Boolean.TRUE;
    }

    public static MethodCode getMethodCode(ClassCode classCode, String methodName, String methodDesc, Map<String, ClassCode> classCodeMap) {
        MethodCode methodCode = classCode.getMethodCode(methodName, methodDesc);
        if (methodCode == null && classCode.getSuperName() != null) {
            ClassCode superClassCode = classCodeMap.get(classCode.getSuperName());
            if (superClassCode != null) {
                methodCode = getMethodCode(superClassCode, methodName, methodDesc, classCodeMap);
            }
        }
        if (methodCode == null) {
            for (String interfaceName : classCode.getInterfaces()) {
                ClassCode interfaceClassCode = classCodeMap.get(interfaceName);
                methodCode = getMethodCode(interfaceClassCode, methodName, methodDesc, classCodeMap);
                if (methodCode != null) {
                    break;
                }
            }
        }
        return methodCode;
    }

    public static int getLine(AbstractInsnNode abstractInsnNode) {
        while (!(abstractInsnNode instanceof LineNumberNode)) {
            abstractInsnNode = abstractInsnNode.getPrevious();
        }
        return ((LineNumberNode) abstractInsnNode).line;
    }

}
