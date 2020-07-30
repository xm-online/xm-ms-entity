package com.icthh.xm.ms.entity.repository.search.elasticsearch.index;

public interface ElasticIndexNameResolver {

    String resolve(String typeKey);
}
