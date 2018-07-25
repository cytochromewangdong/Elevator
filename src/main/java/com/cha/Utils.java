package com.cha;

import java.lang.reflect.Array;

public abstract class Utils {
    public static <T> T[] newArray(Class<T[]> type, int size) {
        return type.cast(Array.newInstance(type.getComponentType(), size));
    }
}
