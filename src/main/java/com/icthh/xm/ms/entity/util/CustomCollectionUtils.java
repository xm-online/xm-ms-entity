package com.icthh.xm.ms.entity.util;

import lombok.experimental.UtilityClass;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

@UtilityClass
public final class CustomCollectionUtils {

    public static <T> Iterable<T> nullSafe(Iterable<T> iterable) {
        return iterable == null ? Collections.emptyList() : iterable;
    }

    public static <T> List<T> nullSafe(List<T> collection) {
        return collection == null ? Collections.emptyList() : collection;
    }

    public static <T> Set<T> nullSafe(Set<T> collection) {
        return collection == null ? Collections.emptySet() : collection;
    }

    public static <K, V> Map<K, V> nullSafe(Map<K, V> map) {
        return map == null ? Collections.emptyMap() : map;
    }

}
