package com.icthh.xm.ms.entity.util;

import com.google.common.collect.Maps;
import lombok.experimental.UtilityClass;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

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
     * @param map
     * @param <K>
     * @param <V>
     * @return
     */
    public static <K, V> Map<K, V> nullSafe(Map<K, V> map) {
        return map == null ? Collections.emptyMap() : map;
    }

    /**
     * Returns new mutable HashMap instance if input is null
     * @param map input map
     * @param <K>
     * @param <V>
     * @return
     */
    public static <K, V> Map<K, V> emptyIfNull (Map<K, V> map) {
        return map == null ? Maps.newHashMap() : map;
    }

}
