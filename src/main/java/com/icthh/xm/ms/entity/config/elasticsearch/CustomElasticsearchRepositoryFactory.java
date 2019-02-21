package com.icthh.xm.ms.entity.config.elasticsearch;

import com.icthh.xm.ms.entity.config.elasticsearch.ElasticsearchConfiguration.IndexName;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.repository.support.ElasticsearchEntityInformation;
import org.springframework.data.elasticsearch.repository.support.ElasticsearchEntityInformationCreator;
import org.springframework.data.elasticsearch.repository.support.ElasticsearchRepositoryFactory;

public class CustomElasticsearchRepositoryFactory extends ElasticsearchRepositoryFactory {

    private final ElasticsearchEntityInformationCreator entityInformationCreator;

    public CustomElasticsearchRepositoryFactory(ElasticsearchOperations elasticsearchOperations,
        IndexName indexName) {
        super(elasticsearchOperations);
        this.entityInformationCreator = new CustomElasticsearchEntityInformationCreator(
            elasticsearchOperations.getElasticsearchConverter().getMappingContext(), indexName);
    }

    @Override
    public <T, ID> ElasticsearchEntityInformation<T, ID> getEntityInformation(
        Class<T> domainClass) {
        return entityInformationCreator.getEntityInformation(domainClass);
    }
}
