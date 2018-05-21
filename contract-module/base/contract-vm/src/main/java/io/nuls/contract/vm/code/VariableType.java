package io.nuls.contract.vm.code;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Getter
@ToString
@EqualsAndHashCode
public class VariableType {

    private static LoadingCache<String, VariableType> CACHE;

    static {
        CACHE = CacheBuilder.newBuilder()
                .expireAfterAccess(10, TimeUnit.MINUTES)
                .build(new CacheLoader<String, VariableType>() {
                    @Override
                    public VariableType load(String desc) {
                        return new VariableType(desc);
                    }
                });
    }

    public static final VariableType INT_TYPE = valueOf("I");
    public static final VariableType LONG_TYPE = valueOf("J");
    public static final VariableType FLOAT_TYPE = valueOf("F");
    public static final VariableType DOUBLE_TYPE = valueOf("D");
    public static final VariableType BOOLEAN_TYPE = valueOf("Z");
    public static final VariableType BYTE_TYPE = valueOf("B");
    public static final VariableType CHAR_TYPE = valueOf("C");
    public static final VariableType SHORT_TYPE = valueOf("S");
    public static final VariableType STRING_TYPE = valueOf("Ljava/lang/String;");
    public static final VariableType BIGINTEGER_TYPE = valueOf("Ljava/math/BigInteger;");
    public static final VariableType ADDRESS_TYPE = valueOf("Lio/nuls/contract/sdk/Address;");
    public static final VariableType INT_ARRAY_TYPE = valueOf("[I");
    public static final VariableType LONG_ARRAY_TYPE = valueOf("[J");
    public static final VariableType FLOAT_ARRAY_TYPE = valueOf("[F");
    public static final VariableType DOUBLE_ARRAY_TYPE = valueOf("[D");
    public static final VariableType BOOLEAN_ARRAY_TYPE = valueOf("[Z");
    public static final VariableType BYTE_ARRAY_TYPE = valueOf("[B");
    public static final VariableType CHAR_ARRAY_TYPE = valueOf("[C");
    public static final VariableType SHORT_ARRAY_TYPE = valueOf("[S");
    public static final VariableType STRING_ARRAY_TYPE = valueOf("[Ljava/lang/String;");

    private String desc;

    private String type;

    private VariableType componentType;

    private boolean primitiveType;

    private boolean primitive;

    private boolean array;

    private int dimensions;

    private Object defaultValue;

    private VariableType(String desc) {
        if (Descriptors.DESCRIPTORS.containsKey(desc)) {
            this.desc = Descriptors.DESCRIPTORS.get(desc);
        } else {
            this.desc = desc;
        }
        this.type = this.desc;
        if (this.type.contains("[")) {
            this.array = true;
            this.dimensions = this.type.lastIndexOf("[") + 1;
            this.type = this.desc.replace("[", "");
            this.componentType = valueOf(this.desc.replaceFirst("\\[", ""));
        }
        if (Descriptors.DESCRIPTORS.inverse().containsKey(this.type)) {
            this.type = Descriptors.DESCRIPTORS.inverse().get(this.type);
            this.primitiveType = isNotVoid();
        } else if (this.type.startsWith("L") && this.type.endsWith(";")) {
            this.type = this.type.substring(1, this.type.length() - 1);
        } else {
            this.desc = "L" + this.type + ";";
        }
        if (!this.array && this.primitiveType) {
            this.primitive = true;
            this.defaultValue = this.defaultValue();
        }
    }

    public static VariableType valueOf(String desc) {
        try {
            VariableType variableType = CACHE.get(desc);
            if (!variableType.getDesc().equals(desc)) {
                variableType = CACHE.get(variableType.getDesc());
                CACHE.put(desc, variableType);
            }
            return variableType;
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public static VariableType parseReturn(String desc) {
        List<String> list = Descriptors.parse(desc, true);
        if (list.size() < 1) {
            throw new IllegalArgumentException();
        }
        String type = list.get(list.size() - 1);
        return valueOf(type);
    }

    public static List<VariableType> parseArgs(String desc) {
        List<VariableType> args = new ArrayList<>();
        List<String> list = Descriptors.parse(desc);
        for (String type : list) {
            args.add(valueOf(type));
        }
        return args;
    }

    public static List<VariableType> parseAll(String desc) {
        List<VariableType> args = new ArrayList<>();
        List<String> list = Descriptors.parse(desc, true);
        for (String type : list) {
            args.add(valueOf(type));
        }
        return args;
    }

    public boolean isVoid() {
        return "void".equals(this.type);
    }

    public boolean isNotVoid() {
        return !isVoid();
    }

    public boolean isByte() {
        return primitive && "byte".equals(this.type);
    }

    public boolean isChar() {
        return primitive && "char".equals(this.type);
    }

    public boolean isDouble() {
        return primitive && "double".equals(this.type);
    }

    public boolean isFloat() {
        return primitive && "float".equals(this.type);
    }

    public boolean isInt() {
        return primitive && "int".equals(this.type);
    }

    public boolean isLong() {
        return primitive && "long".equals(this.type);
    }

    public boolean isShort() {
        return primitive && "short".equals(this.type);
    }

    public boolean isBoolean() {
        return primitive && "boolean".equals(this.type);
    }

    public Object defaultValue() {
        Object defaultValue = null;
        switch (this.type) {
            case "int":
                defaultValue = 0;
                break;
            case "long":
                defaultValue = 0L;
                break;
            case "float":
                defaultValue = 0.0F;
                break;
            case "double":
                defaultValue = 0.0D;
                break;
            case "boolean":
                defaultValue = false;
                break;
            case "byte":
                defaultValue = (byte) 0;
                break;
            case "char":
                defaultValue = '\u0000';
                break;
            case "short":
                defaultValue = (short) 0;
                break;
            default:
                break;
        }
        return defaultValue;
    }

    public Class getPrimitiveTypeClass() {
        Class clazz = null;
        if (!this.primitiveType) {
            return clazz;
        }
        switch (this.type) {
            case "int":
                clazz = Integer.TYPE;
                break;
            case "long":
                clazz = Long.TYPE;
                break;
            case "float":
                clazz = Float.TYPE;
                break;
            case "double":
                clazz = Double.TYPE;
                break;
            case "boolean":
                clazz = Boolean.TYPE;
                break;
            case "byte":
                clazz = Byte.TYPE;
                break;
            case "char":
                clazz = Character.TYPE;
                break;
            case "short":
                clazz = Short.TYPE;
                break;
            default:
                break;
        }
        return clazz;
    }

    public Object getPrimitiveValue(Object value) {
        if (this.primitive) {
            if (value == null) {
                value = defaultValue();
            } else {
                String s = value.toString();
                switch (this.type) {
                    case "int":
                        value = Integer.valueOf(s).intValue();
                        break;
                    case "long":
                        value = Long.valueOf(s).longValue();
                        break;
                    case "float":
                        value = Float.valueOf(s).floatValue();
                        break;
                    case "double":
                        value = Double.valueOf(s).doubleValue();
                        break;
                    case "boolean":
                        if ("true".equalsIgnoreCase(s) || "1".equals(s)) {
                            value = true;
                        } else {
                            value = false;
                        }
                        break;
                    case "byte":
                        value = Byte.valueOf(s).byteValue();
                        break;
                    case "char":
                        value = s.charAt(0);
                        break;
                    case "short":
                        value = Short.valueOf(s).shortValue();
                        break;
                    default:
                        break;
                }
            }
        }
        return value;
    }

    public String getNormalDesc() {
        if (Descriptors.DESCRIPTORS.inverse().containsKey(this.desc)) {
            return Descriptors.DESCRIPTORS.inverse().get(this.desc);
        } else {
            return this.desc;
        }
    }

}
