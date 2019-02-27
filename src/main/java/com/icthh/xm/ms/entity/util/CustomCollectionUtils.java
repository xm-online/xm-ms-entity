package com.icthh.xm.ms.entity.util;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.experimental.UtilityClass;
import java.util.*;

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

    /**
     * Return immutable empty map, if map is empty
     * @param map map
     * @param <K> key
     * @param <V> value
     * @return original map or EmptyMap
     */
    public static <K, V> Map<K, V> nullSafe(Map<K, V> map) {
        return map == null ? Collections.emptyMap() : map;
    }

    /**
     * Returns new mutable HashMap instance if input is null
     * @param map input map
     * @param <K> key
     * @param <V> value
     * @return original map or new HashMap (mutable)
     */
    public static <K, V> Map<K, V> emptyIfNull (Map<K, V> map) {
        return map == null ? Maps.newHashMap() : map;
    }

    /**
     * Union for nullable lists.
     *
     * @param list1 first list
     * @param list2 second list
     * @return concatenated list
     */
    public static <E> List<E> union(final List<E> list1, final List<E> list2) {
        final List<E> result = Lists.newArrayList();
        if (list1 != null) {
            result.addAll(list1);
        }
        if (list2 != null) {
            result.addAll(list2);
        }
        return result;
    }

}
