package com.icthh.xm.ms.entity.config.elasticsearch;

import com.icthh.xm.ms.entity.config.elasticsearch.ElasticsearchConfiguration.IndexName;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentProperty;
import org.springframework.data.elasticsearch.repository.support.ElasticsearchEntityInformation;
import org.springframework.data.elasticsearch.repository.support.ElasticsearchEntityInformationCreatorImpl;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.util.Assert;

public class CustomElasticsearchEntityInformationCreator extends
    ElasticsearchEntityInformationCreatorImpl {

    private final MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> mappingContext;
    private final IndexName indexName;

    public CustomElasticsearchEntityInformationCreator(
        MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> mappingContext,
        IndexName indexName) {
        super(mappingContext);
        this.mappingContext = mappingContext;
        this.indexName = indexName;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T, ID> ElasticsearchEntityInformation<T, ID> getEntityInformation(
        Class<T> domainClass) {
        ElasticsearchPersistentEntity<T> persistentEntity = (ElasticsearchPersistentEntity<T>) mappingContext
            .getRequiredPersistentEntity(domainClass);

        Assert.notNull(persistentEntity,
            String.format("Unable to obtain mapping metadata for %s!", domainClass));
        Assert.notNull(persistentEntity.getIdProperty(),
            String.format("No id property found for %s!", domainClass));

        return new CustomMappingElasticsearchEntityInformation<>(persistentEntity, indexName);
    }
}
