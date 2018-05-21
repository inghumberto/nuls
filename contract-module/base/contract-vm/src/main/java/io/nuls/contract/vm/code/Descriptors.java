package io.nuls.contract.vm.code;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import java.util.ArrayList;
import java.util.List;

public class Descriptors {

    public static final BiMap<String, String> DESCRIPTORS;

    static {
        DESCRIPTORS = HashBiMap.create();
        DESCRIPTORS.put("void", "V");
        DESCRIPTORS.put("byte", "B");
        DESCRIPTORS.put("char", "C");
        DESCRIPTORS.put("double", "D");
        DESCRIPTORS.put("float", "F");
        DESCRIPTORS.put("int", "I");
        DESCRIPTORS.put("long", "J");
        DESCRIPTORS.put("short", "S");
        DESCRIPTORS.put("boolean", "Z");
    }

    public static List<String> parse(String desc) {
        return parse(desc, false);
    }

    public static List<String> parse(String desc, boolean includeReturn) {
        boolean isL = false;
        boolean isEnd = false;
        StringBuilder sb = new StringBuilder();
        List<String> descList = new ArrayList<>();

        for (int i = 0; i < desc.length(); i++) {
            String s = desc.substring(i, i + 1);

            if ("[".equals(s)) {
                sb.append(s);
            } else if ("(".equals(s)) {
                //
            } else if (")".equals(s)) {
                isEnd = true;
            } else if (";".equals(s)) {
                isEnd = true;
                sb.append(s);
            } else if (isL) {
                sb.append(s);
            } else if ("L".equals(s)) {
                isL = true;
                sb.append(s);
            } else if (DESCRIPTORS.inverse().containsKey(s)) {
                isEnd = true;
                sb.append(s);
            } else {
                throw new IllegalArgumentException("unknown desc");
            }

            if (isEnd) {
                if (sb.length() > 0) {
                    descList.add(sb.toString());
                    sb = new StringBuilder();
                }
                isL = false;
                isEnd = false;
            }

            if (")".equals(s)) {
                if (includeReturn) {
                    //
                } else {
                    break;
                }
            }

        }

        return descList;
    }

}
