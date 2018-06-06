package io.nuls.contract.vm.program.impl;

import io.nuls.contract.sdk.*;
import io.nuls.contract.vm.util.VmUtils;

import java.math.BigInteger;
import java.util.*;

import static io.nuls.contract.vm.util.Utils.classNameReplace;

public class ProgramConstants {

    public static final String CONTRACT_INTERFACE_NAME = classNameReplace(Contract.class.getName());

    public static final Class[] SDK_CLASSES = new Class[]{
            Address.class,
            Block.class,
            Contract.class,
            Event.class,
            Msg.class,
            Utils.class,
    };

    public static final Class[] CONTRACT_USED_CLASSES = new Class[]{
            Boolean.class,
            Byte.class,
            Short.class,
            Character.class,
            Integer.class,
            Long.class,
            Float.class,
            Double.class,
            String.class,
            BigInteger.class,
            List.class,
            ArrayList.class,
            Map.class,
            HashMap.class,
    };

    public static final Class[] VM_INIT_CLASSES = new Class[]{
            StrictMath.class,
            RuntimeException.class,
            ArrayIndexOutOfBoundsException.class,
            OutOfMemoryError.class,
            Collections.class,
            HashSet.class,
            VmUtils.class,
    };

    public static final String[] SDK_CLASS_NAMES = new String[SDK_CLASSES.length];

    public static final String[] CONTRACT_USED_CLASS_NAMES = new String[CONTRACT_USED_CLASSES.length];

    public static final String[] VM_INIT_CLASS_NAMES = new String[VM_INIT_CLASSES.length + 1];

    static {
        for (int i = 0; i < SDK_CLASSES.length; i++) {
            SDK_CLASS_NAMES[i] = classNameReplace(SDK_CLASSES[i].getName());
        }
        for (int i = 0; i < CONTRACT_USED_CLASSES.length; i++) {
            CONTRACT_USED_CLASS_NAMES[i] = classNameReplace(CONTRACT_USED_CLASSES[i].getName());
        }
        int length = VM_INIT_CLASSES.length;
        for (int i = 0; i < length; i++) {
            VM_INIT_CLASS_NAMES[i] = classNameReplace(VM_INIT_CLASSES[i].getName());
        }
        VM_INIT_CLASS_NAMES[length] = "java/lang/CharacterDataLatin1";
    }

}
