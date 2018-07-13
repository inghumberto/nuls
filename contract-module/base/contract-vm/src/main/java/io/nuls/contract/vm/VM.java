package io.nuls.contract.vm;

import io.nuls.contract.entity.BlockHeaderDto;
import io.nuls.contract.util.VMContext;
import io.nuls.contract.vm.code.MethodCode;
import io.nuls.contract.vm.code.VariableType;
import io.nuls.contract.vm.instructions.comparisons.*;
import io.nuls.contract.vm.instructions.constants.*;
import io.nuls.contract.vm.instructions.control.*;
import io.nuls.contract.vm.instructions.conversions.D2x;
import io.nuls.contract.vm.instructions.conversions.F2x;
import io.nuls.contract.vm.instructions.conversions.I2x;
import io.nuls.contract.vm.instructions.conversions.L2x;
import io.nuls.contract.vm.instructions.extended.Ifnonnull;
import io.nuls.contract.vm.instructions.extended.Ifnull;
import io.nuls.contract.vm.instructions.extended.Multianewarray;
import io.nuls.contract.vm.instructions.loads.*;
import io.nuls.contract.vm.instructions.math.*;
import io.nuls.contract.vm.instructions.references.*;
import io.nuls.contract.vm.instructions.stack.Dup;
import io.nuls.contract.vm.instructions.stack.Pop;
import io.nuls.contract.vm.instructions.stack.Swap;
import io.nuls.contract.vm.instructions.stores.*;
import io.nuls.contract.vm.natives.io.nuls.contract.sdk.NativeAddress;
import io.nuls.contract.vm.program.ProgramExecutor;
import io.nuls.contract.vm.program.ProgramTransfer;
import io.nuls.contract.vm.program.impl.ProgramContext;
import io.nuls.contract.vm.program.impl.ProgramInvoke;
import io.nuls.contract.vm.util.Log;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;

import java.util.ArrayList;
import java.util.List;

public class VM {

    private static final int VM_STACK_MAX_SIZE = 512;

    private final VMStack vmStack;

    private final Heap heap;

    private final MethodArea methodArea;

    private Result result;

    private Object resultValue;

    private VMContext vmContext;

    private ProgramInvoke programInvoke;

    private ProgramContext programContext;

    private ProgramExecutor programExecutor;

    private long gasUsed;

    private long gas;

    private long startTime;

    private long endTime;

    private long elapsedTime;

    private boolean revert;

    private boolean error;

    private String errorMessage;

    private List<ProgramTransfer> transfers = new ArrayList<>();

    private List<String> events = new ArrayList<>();

    public VM() {
        this.vmStack = new VMStack(VM_STACK_MAX_SIZE);
        this.heap = new Heap(this);
        this.methodArea = new MethodArea(this);
        this.result = new Result();
    }

    public VM(VM vm) {
        this.vmStack = new VMStack(VM_STACK_MAX_SIZE);
        this.heap = new Heap(this, vm.heap);
        this.methodArea = new MethodArea(this, vm.methodArea);
        this.result = new Result();
    }

    public boolean isEmptyFrame() {
        return this.vmStack.isEmpty();
    }

    public boolean isNotEmptyFrame() {
        return !isEmptyFrame();
    }

    public Frame lastFrame() {
        return this.vmStack.lastElement();
    }

    public void popFrame() {
        this.vmStack.pop();
    }

    public void endTime() {
        this.endTime = System.currentTimeMillis();
        this.elapsedTime = this.endTime - this.startTime;
    }

    public void initProgramContext(ProgramInvoke programInvoke) {
        this.programInvoke = programInvoke;
        ObjectRef coinbase = null;
        long timestamp = 0;
        BlockHeaderDto blockHeader = getBlockHeader(programInvoke.getNumber());
        if (blockHeader != null) {
            coinbase = this.heap.newAddress(blockHeader.getPackingAddress());
            timestamp = blockHeader.getTime();
        }

        programContext = new ProgramContext();
        programContext.setAddress(this.heap.newAddress(NativeAddress.toString(programInvoke.getAddress())));
        programContext.setSender(this.heap.newAddress(NativeAddress.toString(programInvoke.getSender())));
        programContext.setGasPrice(programInvoke.getGasPrice());
        programContext.setGas(programInvoke.getGas());
        programContext.setValue(this.heap.newBigInteger(programInvoke.getValue().toString()));
        programContext.setCoinbase(coinbase);
        programContext.setTimestamp(timestamp);
        programContext.setNumber(programInvoke.getNumber());
    }

    public void run(MethodCode methodCode, Object[] args) {
        Frame frame = new Frame(this, methodCode, args);
        this.vmStack.push(frame);
        run();
    }

    public void run(ObjectRef objectRef, MethodCode methodCode, VMContext vmContext, ProgramInvoke programInvoke) {
        this.vmContext = vmContext;
        this.gas = programInvoke.getGas();
        String[] args = programInvoke.getArgs();
        int length = args == null ? 1 : args.length + 1;
        List runArgs = new ArrayList();
        runArgs.add(objectRef);
        List<VariableType> argsVariableType = methodCode.getArgsVariableType();
        for (int i = 0; i < argsVariableType.size(); i++) {
            VariableType variableType = argsVariableType.get(i);
            Object arg = args[i];
            if (arg == null) {
                runArgs.add(arg);
            } else if (variableType.isArray()) {
                throw new RuntimeException("parameter can't be array");
            } else if (variableType.isPrimitive()) {
                arg = variableType.getPrimitiveValue(arg);
                runArgs.add(arg);
                if (variableType.isLong() || variableType.isDouble()) {
                    runArgs.add(null);
                }
            } else if (VariableType.STRING_TYPE.equals(variableType)) {
                arg = this.heap.newString(arg.toString());
                runArgs.add(arg);
            } else {
                arg = this.heap.newObject(variableType, arg.toString());
                runArgs.add(arg);
            }
        }
        initProgramContext(programInvoke);
        run(methodCode, runArgs.toArray());
    }

    public void run() {
        if (this.startTime < 1) {
            this.startTime = System.currentTimeMillis();
        }
        if (this.error || this.revert || this.result.isError()) {
            endTime();
            return;
        }
        if (!this.vmStack.isEmpty()) {
            final Frame frame = this.vmStack.lastElement();
            Log.runMethod(frame.getMethodCode());
            while (frame.getCurrentInsnNode() != null && !frame.getResult().isEnded()) {
                step(frame);
                frame.step();
                if (this.error || this.revert || this.result.isError()) {
                    endTime();
                    return;
                }
                if (this.result.isException()) {
                    endTime();
                    return;
                }
                if (frame != this.vmStack.lastElement()) {
                    endTime();
                    return;
                }
            }
            this.popFrame();
            Log.endMethod(frame.getMethodCode());
            this.resultValue = frame.getResult().getValue();
            if (!this.vmStack.isEmpty()) {
                final Frame lastFrame = this.vmStack.lastElement();
                if (frame.getResult().getVariableType().isNotVoid()) {
                    lastFrame.getOperandStack().push(frame.getResult().getValue(), frame.getResult().getVariableType());
                }
                Log.continueMethod(lastFrame.getMethodCode());
            } else {
                this.result = frame.getResult();
            }
        }
        endTime();
    }

    private void step(Frame frame) {

        OpCode opCode = frame.currentOpCode();

        if (opCode == null) {
            if (frame.getCurrentInsnNode() != null && frame.getCurrentInsnNode().getOpcode() >= 0) {
                frame.nonsupportOpCode();
            }
            return;
        }

        int gasCost = gasCost(frame, opCode);
        if (0 < this.gas && this.gas < (this.gasUsed + gasCost)) {
            error(String.format("not enough gas for '%s' cause spending: invokeGas[%d], gas[%d], usedGas[%d]", opCode, gasCost, this.gas, this.gasUsed));
            return;
        }
        this.gasUsed += gasCost;

        switch (opCode) {
            case NOP:
                Nop.nop(frame);
                break;
            case ACONST_NULL:
                Aconst.aconst_null(frame);
                break;
            case ICONST_M1:
                Iconst.iconst_m1(frame);
                break;
            case ICONST_0:
                Iconst.iconst_0(frame);
                break;
            case ICONST_1:
                Iconst.iconst_1(frame);
                break;
            case ICONST_2:
                Iconst.iconst_2(frame);
                break;
            case ICONST_3:
                Iconst.iconst_3(frame);
                break;
            case ICONST_4:
                Iconst.iconst_4(frame);
                break;
            case ICONST_5:
                Iconst.iconst_5(frame);
                break;
            case LCONST_0:
                Lconst.lconst_0(frame);
                break;
            case LCONST_1:
                Lconst.lconst_1(frame);
                break;
            case FCONST_0:
                Fconst.fconst_0(frame);
                break;
            case FCONST_1:
                Fconst.fconst_1(frame);
                break;
            case FCONST_2:
                Fconst.fconst_2(frame);
                break;
            case DCONST_0:
                Dconst.dconst_0(frame);
                break;
            case DCONST_1:
                Dconst.dconst_1(frame);
                break;
            case BIPUSH:
                Xipush.bipush(frame);
                break;
            case SIPUSH:
                Xipush.sipush(frame);
                break;
            case LDC:
                Ldc.ldc(frame);
                break;
            case ILOAD:
                Iload.iload(frame);
                break;
            case LLOAD:
                Lload.lload(frame);
                break;
            case FLOAD:
                Fload.fload(frame);
                break;
            case DLOAD:
                Dload.dload(frame);
                break;
            case ALOAD:
                Aload.aload(frame);
                break;
            case IALOAD:
                Xaload.iaload(frame);
                break;
            case LALOAD:
                Xaload.laload(frame);
                break;
            case FALOAD:
                Xaload.faload(frame);
                break;
            case DALOAD:
                Xaload.daload(frame);
                break;
            case AALOAD:
                Xaload.aaload(frame);
                break;
            case BALOAD:
                Xaload.baload(frame);
                break;
            case CALOAD:
                Xaload.caload(frame);
                break;
            case SALOAD:
                Xaload.saload(frame);
                break;
            case ISTORE:
                Istore.istore(frame);
                break;
            case LSTORE:
                Lstore.lstore(frame);
                break;
            case FSTORE:
                Fstore.fstore(frame);
                break;
            case DSTORE:
                Dstore.dstore(frame);
                break;
            case ASTORE:
                Astore.astore(frame);
                break;
            case IASTORE:
                Xastore.iastore(frame);
                break;
            case LASTORE:
                Xastore.lastore(frame);
                break;
            case FASTORE:
                Xastore.fastore(frame);
                break;
            case DASTORE:
                Xastore.dastore(frame);
                break;
            case AASTORE:
                Xastore.aastore(frame);
                break;
            case BASTORE:
                Xastore.bastore(frame);
                break;
            case CASTORE:
                Xastore.castore(frame);
                break;
            case SASTORE:
                Xastore.sastore(frame);
                break;
            case POP:
                Pop.pop(frame);
                break;
            case POP2:
                Pop.pop2(frame);
                break;
            case DUP:
                Dup.dup(frame);
                break;
            case DUP_X1:
                Dup.dup_x1(frame);
                break;
            case DUP_X2:
                Dup.dup_x2(frame);
                break;
            case DUP2:
                Dup.dup2(frame);
                break;
            case DUP2_X1:
                Dup.dup2_x1(frame);
                break;
            case DUP2_X2:
                Dup.dup2_x2(frame);
                break;
            case SWAP:
                Swap.swap(frame);
                break;
            case IADD:
                Add.iadd(frame);
                break;
            case LADD:
                Add.ladd(frame);
                break;
            case FADD:
                Add.fadd(frame);
                break;
            case DADD:
                Add.dadd(frame);
                break;
            case ISUB:
                Sub.isub(frame);
                break;
            case LSUB:
                Sub.lsub(frame);
                break;
            case FSUB:
                Sub.fsub(frame);
                break;
            case DSUB:
                Sub.dsub(frame);
                break;
            case IMUL:
                Mul.imul(frame);
                break;
            case LMUL:
                Mul.lmul(frame);
                break;
            case FMUL:
                Mul.fmul(frame);
                break;
            case DMUL:
                Mul.dmul(frame);
                break;
            case IDIV:
                Div.idiv(frame);
                break;
            case LDIV:
                Div.ldiv(frame);
                break;
            case FDIV:
                Div.fdiv(frame);
                break;
            case DDIV:
                Div.ddiv(frame);
                break;
            case IREM:
                Rem.irem(frame);
                break;
            case LREM:
                Rem.lrem(frame);
                break;
            case FREM:
                Rem.frem(frame);
                break;
            case DREM:
                Rem.drem(frame);
                break;
            case INEG:
                Neg.ineg(frame);
                break;
            case LNEG:
                Neg.lneg(frame);
                break;
            case FNEG:
                Neg.fneg(frame);
                break;
            case DNEG:
                Neg.dneg(frame);
                break;
            case ISHL:
                Shl.ishl(frame);
                break;
            case LSHL:
                Shl.lshl(frame);
                break;
            case ISHR:
                Shr.ishr(frame);
                break;
            case LSHR:
                Shr.lshr(frame);
                break;
            case IUSHR:
                Ushr.iushr(frame);
                break;
            case LUSHR:
                Ushr.lushr(frame);
                break;
            case IAND:
                And.iand(frame);
                break;
            case LAND:
                And.land(frame);
                break;
            case IOR:
                Or.ior(frame);
                break;
            case LOR:
                Or.lor(frame);
                break;
            case IXOR:
                Xor.ixor(frame);
                break;
            case LXOR:
                Xor.lxor(frame);
                break;
            case IINC:
                Iinc.iinc(frame);
                break;
            case I2L:
                I2x.i2l(frame);
                break;
            case I2F:
                I2x.i2f(frame);
                break;
            case I2D:
                I2x.i2d(frame);
                break;
            case L2I:
                L2x.l2i(frame);
                break;
            case L2F:
                L2x.l2f(frame);
                break;
            case L2D:
                L2x.l2d(frame);
                break;
            case F2I:
                F2x.f2i(frame);
                break;
            case F2L:
                F2x.f2l(frame);
                break;
            case F2D:
                F2x.f2d(frame);
                break;
            case D2I:
                D2x.d2i(frame);
                break;
            case D2L:
                D2x.d2l(frame);
                break;
            case D2F:
                D2x.d2f(frame);
                break;
            case I2B:
                I2x.i2b(frame);
                break;
            case I2C:
                I2x.i2c(frame);
                break;
            case I2S:
                I2x.i2s(frame);
                break;
            case LCMP:
                Lcmp.lcmp(frame);
                break;
            case FCMPL:
                Fcmp.fcmpl(frame);
                break;
            case FCMPG:
                Fcmp.fcmpg(frame);
                break;
            case DCMPL:
                Dcmp.dcmpl(frame);
                break;
            case DCMPG:
                Dcmp.dcmpg(frame);
                break;
            case IFEQ:
                IfCmp.ifeq(frame);
                break;
            case IFNE:
                IfCmp.ifne(frame);
                break;
            case IFLT:
                IfCmp.iflt(frame);
                break;
            case IFGE:
                IfCmp.ifge(frame);
                break;
            case IFGT:
                IfCmp.ifgt(frame);
                break;
            case IFLE:
                IfCmp.ifle(frame);
                break;
            case IF_ICMPEQ:
                IfIcmp.if_icmpeq(frame);
                break;
            case IF_ICMPNE:
                IfIcmp.if_icmpne(frame);
                break;
            case IF_ICMPLT:
                IfIcmp.if_icmplt(frame);
                break;
            case IF_ICMPGE:
                IfIcmp.if_icmpge(frame);
                break;
            case IF_ICMPGT:
                IfIcmp.if_icmpgt(frame);
                break;
            case IF_ICMPLE:
                IfIcmp.if_icmple(frame);
                break;
            case IF_ACMPEQ:
                IfAcmp.if_acmpeq(frame);
                break;
            case IF_ACMPNE:
                IfAcmp.if_acmpne(frame);
                break;
            case GOTO:
                Goto.goto_(frame);
                break;
            case JSR:
                Jsr.jsr(frame);
                break;
            case RET:
                Ret.ret(frame);
                break;
            case TABLESWITCH:
                Tableswitch.tableswitch(frame);
                break;
            case LOOKUPSWITCH:
                Lookupswitch.lookupswitch(frame);
                break;
            case IRETURN:
                Return.ireturn(frame);
                break;
            case LRETURN:
                Return.lreturn(frame);
                break;
            case FRETURN:
                Return.freturn(frame);
                break;
            case DRETURN:
                Return.dreturn(frame);
                break;
            case ARETURN:
                Return.areturn(frame);
                break;
            case RETURN:
                Return.return_(frame);
                break;
            case GETSTATIC:
                Getstatic.getstatic(frame);
                break;
            case PUTSTATIC:
                Putstatic.putstatic(frame);
                break;
            case GETFIELD:
                Getfield.getfield(frame);
                break;
            case PUTFIELD:
                Putfield.putfield(frame);
                break;
            case INVOKEVIRTUAL:
                Invokevirtual.invokevirtual(frame);
                break;
            case INVOKESPECIAL:
                Invokespecial.invokespecial(frame);
                break;
            case INVOKESTATIC:
                Invokestatic.invokestatic(frame);
                break;
            case INVOKEINTERFACE:
                Invokeinterface.invokeinterface(frame);
                break;
            case INVOKEDYNAMIC:
                Invokedynamic.invokedynamic(frame);
                break;
            case NEW:
                New.new_(frame);
                break;
            case NEWARRAY:
                Newarray.newarray(frame);
                break;
            case ANEWARRAY:
                Anewarray.anewarray(frame);
                break;
            case ARRAYLENGTH:
                Arraylength.arraylength(frame);
                break;
            case ATHROW:
                Athrow.athrow(frame);
                break;
            case CHECKCAST:
                Checkcast.checkcast(frame);
                break;
            case INSTANCEOF:
                Instanceof.instanceof_(frame);
                break;
            case MONITORENTER:
                Monitorenter.monitorenter(frame);
                break;
            case MONITOREXIT:
                Monitorexit.monitorexit(frame);
                break;
            case MULTIANEWARRAY:
                Multianewarray.multianewarray(frame);
                break;
            case IFNULL:
                Ifnull.ifnull(frame);
                break;
            case IFNONNULL:
                Ifnonnull.ifnonnull(frame);
                break;
            default:
                frame.nonsupportOpCode();
                break;
        }
    }

    public int gasCost(Frame frame, OpCode opCode) {
        int gasCost = 1;
        switch (opCode) {
            case NOP:
                break;
            case ACONST_NULL:
            case ICONST_M1:
            case ICONST_0:
            case ICONST_1:
            case ICONST_2:
            case ICONST_3:
            case ICONST_4:
            case ICONST_5:
            case LCONST_0:
            case LCONST_1:
            case FCONST_0:
            case FCONST_1:
            case FCONST_2:
            case DCONST_0:
            case DCONST_1:
            case BIPUSH:
            case SIPUSH:
                gasCost = GasCost.CONSTANT;
                break;
            case LDC:
                Object value = frame.ldcInsnNode().cst;
                if (value instanceof Number) {
                    gasCost = GasCost.LDC;
                } else {
                    gasCost = Math.max(value.toString().length(), 1) * GasCost.LDC;
                }
                break;
            case ILOAD:
            case LLOAD:
            case FLOAD:
            case DLOAD:
            case ALOAD:
                gasCost = GasCost.LOAD;
                break;
            case IALOAD:
            case LALOAD:
            case FALOAD:
            case DALOAD:
            case AALOAD:
            case BALOAD:
            case CALOAD:
            case SALOAD:
                gasCost = GasCost.ARRAYLOAD;
                break;
            case ISTORE:
            case LSTORE:
            case FSTORE:
            case DSTORE:
            case ASTORE:
                gasCost = GasCost.STORE;
                break;
            case IASTORE:
            case LASTORE:
            case FASTORE:
            case DASTORE:
            case AASTORE:
            case BASTORE:
            case CASTORE:
            case SASTORE:
                gasCost = GasCost.ARRAYSTORE;
                break;
            case POP:
            case POP2:
            case DUP:
            case DUP_X1:
            case DUP_X2:
            case DUP2:
            case DUP2_X1:
            case DUP2_X2:
            case SWAP:
                gasCost = GasCost.STACK;
                break;
            case IADD:
            case LADD:
            case FADD:
            case DADD:
            case ISUB:
            case LSUB:
            case FSUB:
            case DSUB:
            case IMUL:
            case LMUL:
            case FMUL:
            case DMUL:
            case IDIV:
            case LDIV:
            case FDIV:
            case DDIV:
            case IREM:
            case LREM:
            case FREM:
            case DREM:
            case INEG:
            case LNEG:
            case FNEG:
            case DNEG:
            case ISHL:
            case LSHL:
            case ISHR:
            case LSHR:
            case IUSHR:
            case LUSHR:
            case IAND:
            case LAND:
            case IOR:
            case LOR:
            case IXOR:
            case LXOR:
            case IINC:
                gasCost = GasCost.MATH;
                break;
            case I2L:
            case I2F:
            case I2D:
            case L2I:
            case L2F:
            case L2D:
            case F2I:
            case F2L:
            case F2D:
            case D2I:
            case D2L:
            case D2F:
            case I2B:
            case I2C:
            case I2S:
                gasCost = GasCost.CONVERSION;
                break;
            case LCMP:
            case FCMPL:
            case FCMPG:
            case DCMPL:
            case DCMPG:
            case IFEQ:
            case IFNE:
            case IFLT:
            case IFGE:
            case IFGT:
            case IFLE:
            case IF_ICMPEQ:
            case IF_ICMPNE:
            case IF_ICMPLT:
            case IF_ICMPGE:
            case IF_ICMPGT:
            case IF_ICMPLE:
            case IF_ACMPEQ:
            case IF_ACMPNE:
                gasCost = GasCost.COMPARISON;
                break;
            case GOTO:
            case JSR:
            case RET:
                gasCost = GasCost.CONTROL;
                break;
            case TABLESWITCH:
                TableSwitchInsnNode table = frame.tableSwitchInsnNode();
                gasCost = Math.max(table.max - table.min, 1) * GasCost.TABLESWITCH;
                break;
            case LOOKUPSWITCH:
                LookupSwitchInsnNode lookup = frame.lookupSwitchInsnNode();
                gasCost = Math.max(lookup.keys.size(), 1) * GasCost.LOOKUPSWITCH;
                break;
            case IRETURN:
            case LRETURN:
            case FRETURN:
            case DRETURN:
            case ARETURN:
            case RETURN:
                gasCost = GasCost.CONTROL;
                break;
            case GETSTATIC:
            case PUTSTATIC:
            case GETFIELD:
            case PUTFIELD:
            case INVOKEVIRTUAL:
            case INVOKESPECIAL:
            case INVOKESTATIC:
            case INVOKEINTERFACE:
            case INVOKEDYNAMIC:
            case NEW:
                gasCost = GasCost.REFERENCE;
                break;
            case NEWARRAY:
            case ANEWARRAY:
                int count = frame.getOperandStack().popInt();
                gasCost = Math.max(count, 1) * GasCost.NEWARRAY;
                frame.getOperandStack().pushInt(count);
                break;
            case ARRAYLENGTH:
            case ATHROW:
            case CHECKCAST:
            case INSTANCEOF:
            case MONITORENTER:
            case MONITOREXIT:
                gasCost = GasCost.REFERENCE;
                break;
            case MULTIANEWARRAY:
                MultiANewArrayInsnNode multiANewArrayInsnNode = frame.multiANewArrayInsnNode();
                int size = 1;
                int[] dimensions = new int[multiANewArrayInsnNode.dims];
                for (int i = multiANewArrayInsnNode.dims - 1; i >= 0; i--) {
                    int length = frame.getOperandStack().popInt();
                    if (length > 0) {
                        size *= length;
                    }
                    dimensions[i] = length;
                }
                for (int dimension : dimensions) {
                    frame.getOperandStack().pushInt(dimension);
                }
                gasCost = size * GasCost.MULTIANEWARRAY;
                break;
            case IFNULL:
            case IFNONNULL:
                gasCost = GasCost.EXTENDED;
                break;
            default:
                break;
        }
        return gasCost;
    }

    public BlockHeaderDto getBlockHeader(long number) {
        try {
            if (this.vmContext != null) {
                BlockHeaderDto blockHeader = this.vmContext.getBlockHeader(number);
                return blockHeader;
            } else {
                return null;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static int getVmStackMaxSize() {
        return VM_STACK_MAX_SIZE;
    }

    public VMStack getVmStack() {
        return vmStack;
    }

    public Heap getHeap() {
        return heap;
    }

    public MethodArea getMethodArea() {
        return methodArea;
    }

    public Result getResult() {
        return result;
    }

    public Object getResultValue() {
        return resultValue;
    }

    public VMContext getVmContext() {
        return vmContext;
    }

    public ProgramInvoke getProgramInvoke() {
        return programInvoke;
    }

    public ProgramContext getProgramContext() {
        return programContext;
    }

    public ProgramExecutor getProgramExecutor() {
        return programExecutor;
    }

    public long getGasUsed() {
        return gasUsed;
    }

    public long getGas() {
        return gas;
    }

    public long getGasLeft() {
        return gas - gasUsed;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public long getElapsedTime() {
        return elapsedTime;
    }

    public List<ProgramTransfer> getTransfers() {
        return transfers;
    }

    public List<String> getEvents() {
        return events;
    }

    public void setResult(Result result) {
        this.result = result;
    }

    public void setProgramExecutor(ProgramExecutor programExecutor) {
        this.programExecutor = programExecutor;
    }

    public void setGasUsed(long gasUsed) {
        this.gasUsed = gasUsed;
    }

    public void revert(String errorMessage) {
        this.revert = true;
        this.errorMessage = errorMessage;
    }

    public void error(String errorMessage) {
        this.error = true;
        this.errorMessage = errorMessage;
    }

    public boolean isRevert() {
        return revert;
    }

    public boolean isError() {
        return error;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

}
