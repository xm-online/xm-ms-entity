package com.icthh.xm.ms.entity.util;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;
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

    /**
     * Union for nullable lists with exclude duplicates by identity.
     *
     * @param list1 first list
     * @param list2 second list
     * @return concatenated list
     */
    public static <E> List<E> uniqUnion(final List<E> list1, final List<E> list2) {
        List<E> union = union(list1, list2);
        List<E> uniqUnion = new ArrayList<>();
        for (int currentIndex = 0; currentIndex < union.size(); currentIndex++) {
            if (!containsByIdentity(union, currentIndex)) {
                uniqUnion.add(union.get(currentIndex));
            }
        }
        return uniqUnion;
    }

    private static <E> boolean containsByIdentity(List<E> union, int current) {
        for (int i = current + 1; i < union.size(); i++) {
            if (union.get(current) == union.get(i)) {
                return true;
            }
        }
        return false;
    }

}
