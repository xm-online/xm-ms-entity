package com.icthh.xm.ms.entity.config.elasticsearch;

import com.icthh.xm.ms.entity.config.elasticsearch.ElasticsearchConfiguration.IndexName;
import java.io.Serializable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.repository.support.ElasticsearchRepositoryFactoryBean;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;

/**
 * This custom factory is used for temporary fix of the issue:
 * {@see} <a href="https://jira.spring.io/projects/DATAES/issues/DATAES-387">https://jira.spring.io/projects/DATAES/issues/DATAES-387</a>.
 * <p>The fix in the {@link CustomMappingElasticsearchEntityInformation}.
 */
public class CustomElasticsearchRepositoryFactoryBean<T extends Repository<S, ID>, S, ID extends Serializable>
    extends ElasticsearchRepositoryFactoryBean<T, S, ID> {

    private ElasticsearchOperations operations;
    private IndexName indexName;

    public CustomElasticsearchRepositoryFactoryBean(Class<? extends T> repositoryInterface) {
        super(repositoryInterface);
    }

    @Autowired
    public void setIndexName(IndexName indexName) {
        this.indexName = indexName;
    }

    @Override
    public void setElasticsearchOperations(ElasticsearchOperations operations) {
        super.setElasticsearchOperations(operations);
        this.operations = operations;
    }

    @Override
    protected RepositoryFactorySupport createRepositoryFactory() {
        return new CustomElasticsearchRepositoryFactory(operations, indexName);
    }
}
