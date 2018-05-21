package io.nuls.contract.vm;

public class GasCost {

    public static final int COMPARISON = 1;
    public static final int CONSTANT = 1;
    public static final int LDC = 1;//
    public static final int CONTROL = 5;
    public static final int TABLESWITCH = 2;//
    public static final int LOOKUPSWITCH = 2;//
    public static final int CONVERSION = 1;
    public static final int EXTENDED = 1;
    public static final int MULTIANEWARRAY = 1;//
    public static final int LOAD = 1;
    public static final int ARRAYLOAD = 5;
    public static final int MATH = 1;
    public static final int REFERENCE = 10;
    public static final int NEWARRAY = 1;//
    public static final int STACK = 2;
    public static final int STORE = 1;
    public static final int ARRAYSTORE = 5;

}
