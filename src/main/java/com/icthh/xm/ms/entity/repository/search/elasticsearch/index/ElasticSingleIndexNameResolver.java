package com.icthh.xm.ms.entity.repository.search.elasticsearch.index;

import static com.icthh.xm.ms.entity.config.elasticsearch.CustomMappingElasticsearchEntityInformation.XMENTITY_SUFFIX;

import com.icthh.xm.ms.entity.config.elasticsearch.ElasticsearchConfiguration;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(value = "application.use-elasticsearch-multiple-indices",
    havingValue = "false", matchIfMissing = true)
@RequiredArgsConstructor
public class ElasticSingleIndexNameResolver implements ElasticIndexNameResolver {

    private final ElasticsearchConfiguration.IndexName indexName;

    @Override
    public String resolve(String typeKey) {
        return indexName.getPrefix() + XMENTITY_SUFFIX;
    }
}
