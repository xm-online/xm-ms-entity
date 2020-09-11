package com.icthh.xm.ms.entity.repository.search.elasticsearch.index;

import static org.apache.commons.lang3.StringUtils.INDEX_NOT_FOUND;
import static org.apache.commons.lang3.StringUtils.lowerCase;
import static org.apache.commons.lang3.StringUtils.substring;

import com.icthh.xm.ms.entity.config.elasticsearch.ElasticsearchConfiguration;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(value = "application.use-elasticsearch-multiple-indices")
@RequiredArgsConstructor
public class ElasticMultipleIndexNameResolver implements ElasticIndexNameResolver {

    private final ElasticsearchConfiguration.IndexName indexName;

    @Override
    public String resolve(String typeKey) {
        if (typeKey == null) return null;
        int dotIndex = StringUtils.indexOf(typeKey, ".");
        int end = dotIndex == INDEX_NOT_FOUND ? typeKey.length() : dotIndex;
        return indexName.getPrefix() + lowerCase(substring(typeKey, 0, end));
    }
}
