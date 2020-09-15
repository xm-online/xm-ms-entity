package com.icthh.xm.ms.entity.repository.search.elasticsearch.index;

import static org.apache.commons.lang3.StringUtils.lowerCase;

import com.icthh.xm.ms.entity.config.elasticsearch.ElasticsearchConfiguration;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(value = "application.use-elasticsearch-multiple-indices")
@RequiredArgsConstructor
public class ElasticMultipleIndexNameResolver implements ElasticIndexNameResolver {

    private final ElasticsearchConfiguration.IndexName indexName;

    @Override
    public String resolve(String typeKey) {
        String rootIndex = getRootIndex(typeKey);
        return indexName.getPrefix() +
            lowerCase(rootIndex).replaceAll("-", "_");
    }
}
