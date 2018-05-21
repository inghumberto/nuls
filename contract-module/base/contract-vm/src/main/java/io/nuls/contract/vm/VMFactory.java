package io.nuls.contract.vm;

import io.nuls.contract.vm.code.ClassCode;
import io.nuls.contract.vm.code.ClassCodeLoader;
import io.nuls.contract.vm.program.impl.ProgramConstants;
import org.apache.commons.lang3.ArrayUtils;

import java.util.LinkedHashMap;
import java.util.Map;

public class VMFactory {

    public static final Map<String, ClassCode> VM_INIT_CLASS_CODES = new LinkedHashMap();

    public static final VM VM;

    static {
        String[] classes = ArrayUtils.addAll(ProgramConstants.VM_INIT_CLASS_NAMES, ProgramConstants.CONTRACT_USED_CLASS_NAMES);
        classes = ArrayUtils.addAll(classes, ProgramConstants.SDK_CLASS_NAMES);
        for (int i = 0; i < classes.length; i++) {
            ClassCodeLoader.load(VM_INIT_CLASS_CODES, classes[i], ClassCodeLoader::loadFromResource);
        }
        VM = newVM();
    }

    public static VM createVM() {
        return new VM(VM);
    }

    public static VM newVM() {
        VM vm = new VM();
        vm.getMethodArea().loadClassCodes(VM_INIT_CLASS_CODES.values());
        return vm;
    }

}
