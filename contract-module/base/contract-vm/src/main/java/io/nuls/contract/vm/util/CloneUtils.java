package io.nuls.contract.vm.util;

import io.nuls.contract.vm.ObjectRef;

import java.lang.reflect.Array;
import java.util.LinkedHashMap;
import java.util.Map;

public class CloneUtils {

    public static void clone(Map<String, Object> source, Map<String, Object> target) {
        for (String key : source.keySet()) {
            Object object = source.get(key);
            if (object == null) {
                target.put(key, null);
            } else if (object instanceof Integer) {
                target.put(key, ((Integer) object).intValue());
            } else if (object instanceof Long) {
                target.put(key, ((Long) object).longValue());
            } else if (object instanceof Float) {
                target.put(key, ((Float) object).floatValue());
            } else if (object instanceof Double) {
                target.put(key, ((Double) object).doubleValue());
            } else if (object instanceof Boolean) {
                target.put(key, ((Boolean) object).booleanValue());
            } else if (object instanceof Byte) {
                target.put(key, ((Byte) object).byteValue());
            } else if (object instanceof Character) {
                target.put(key, ((Character) object).charValue());
            } else if (object instanceof Short) {
                target.put(key, ((Short) object).shortValue());
            } else if (object instanceof String) {
                target.put(key, object);
            } else if (object instanceof ObjectRef) {
                target.put(key, object);
            } else if (object.getClass().isArray()) {
                int length = Array.getLength(object);
                Object array = Array.newInstance(object.getClass().getComponentType(), length);
                System.arraycopy(object, 0, array, 0, length);
                target.put(key, array);
            } else {
                target.put(key, object);
            }
        }
    }

    public static Map<String, Object> clone(Map<String, Object> source) {
        Map<String, Object> target = new LinkedHashMap<>();
        clone(source, target);
        return target;
    }

}
