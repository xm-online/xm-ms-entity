package com.icthh.xm.ms.entity.config.elasticsearch;

import com.icthh.xm.ms.entity.config.elasticsearch.ElasticsearchConfiguration.IndexName;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.data.elasticsearch.repository.support.ElasticsearchEntityInformation;
import org.springframework.data.elasticsearch.repository.support.MappingElasticsearchEntityInformation;

public class CustomMappingElasticsearchEntityInformation<T, ID> extends
    MappingElasticsearchEntityInformation<T, ID> implements ElasticsearchEntityInformation<T, ID> {

    public static final String XMENTITY_SUFFIX = "xmentity";

    private final IndexName indexName;

    public CustomMappingElasticsearchEntityInformation(ElasticsearchPersistentEntity<T> entity,
        IndexName indexName) {
        super(entity);
        this.indexName = indexName;
    }

    @Override
    public String getIndexName() {
        String name = super.getIndexName();
        if (name.endsWith("_" + XMENTITY_SUFFIX)) {
            name = indexName.getPrefix() + XMENTITY_SUFFIX;
        }
        return name;
    }
}
