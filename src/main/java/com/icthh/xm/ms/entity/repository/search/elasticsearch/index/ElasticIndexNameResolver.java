package com.icthh.xm.ms.entity.repository.search.elasticsearch.index;

import static org.apache.commons.lang3.StringUtils.INDEX_NOT_FOUND;
import static org.apache.commons.lang3.StringUtils.substring;

import org.apache.commons.lang3.StringUtils;

public interface ElasticIndexNameResolver {

    String resolve(String typeKey);

    default String getRootIndex(String typeKey){
        if (typeKey == null) return null;
        int dotIndex = StringUtils.indexOf(typeKey, ".");
        int end = dotIndex == INDEX_NOT_FOUND ? typeKey.length() : dotIndex;
        return substring(typeKey, 0, end);
    }

    default boolean isOnlyRoot(String typeKey){
        return getRootIndex(typeKey).equals(typeKey);
    }
}
