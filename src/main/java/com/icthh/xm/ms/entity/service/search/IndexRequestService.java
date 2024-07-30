package com.icthh.xm.ms.entity.service.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.IndexOperation;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.elasticsearch.indices.IndexSettings;
import co.elastic.clients.elasticsearch.indices.RefreshRequest;
import co.elastic.clients.json.JsonData;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.ms.entity.service.search.query.dto.IndexQuery;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndexRequestService {

    private final ElasticsearchClient elasticsearchClient;
    private final ObjectMapper objectMapper;

    @SneakyThrows
    public boolean createIndex(String indexName) {
        return createIndex(indexName, null);
    }

    @SneakyThrows
    public boolean createIndex(String indexName, Object settings) {
        assertIndexNameNotBlank(indexName);

        CreateIndexRequest.Builder createIndexRequestBuilder = new CreateIndexRequest.Builder()
            .index(indexName);

        if (settings != null) {
            if (settings instanceof String || settings instanceof Map) {
                Map<String, Object> settingsMap = objectMapper.convertValue(settings, new TypeReference<Map<String, Object>>() {});
                createIndexRequestBuilder.settings(IndexSettings.of(b -> b.otherSettings(createOtherSettingsMap(settingsMap))));
            }
        }

        CreateIndexRequest request = createIndexRequestBuilder.build();
        return elasticsearchClient.indices()
            .create(request)
            .acknowledged();
    }

    @SneakyThrows
    public String index(IndexQuery indexQuery) {
        IndexRequest<Object> indexRequest = buildIndexRequest(indexQuery);
        return elasticsearchClient.index(indexRequest).id();
    }

    @SneakyThrows
    public void refresh(String indexName) {
        assertIndexNameNotBlank(indexName);
        RefreshRequest request = RefreshRequest.of(refreshRequest -> refreshRequest.index(indexName));
        elasticsearchClient.indices().refresh(request);
    }

    @SneakyThrows
    public void bulkIndex(List<IndexQuery> indexQueries) {
        BulkRequest.Builder bulkRequestBuilder = new BulkRequest.Builder();

        for (IndexQuery indexQuery : indexQueries) {
            IndexOperation<Object> indexOperation = new IndexOperation.Builder<>()
                .index(indexQuery.getIndexName())
                .id(indexQuery.getId())
                .document(indexQuery.getObject())
                .build();
            bulkRequestBuilder.operations(BulkOperation.of(op -> op.index(indexOperation)));
        }

        elasticsearchClient.bulk(bulkRequestBuilder.build());
    }

    @SneakyThrows
    public boolean deleteIndex(String indexName) {
        assertIndexNameNotBlank(indexName);
        return elasticsearchClient.indices()
            .delete(request -> request.index(indexName))
            .acknowledged();
    }

    public boolean existIndex(String indexName) {
        try {
            return elasticsearchClient.indices().exists(ExistsRequest.of(e -> e.index(indexName))).value();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private IndexRequest<Object> buildIndexRequest(IndexQuery indexQuery) {
        return IndexRequest.of(indexRequest -> {
            if (StringUtils.isNotBlank(indexQuery.getIndexName())) {
                indexRequest.index(indexQuery.getIndexName());
            }

            if (indexQuery.getObject() != null) {
                indexRequest.document(indexQuery.getObject());
            }

            if (StringUtils.isNotBlank(indexQuery.getId())) {
                indexRequest.id(indexQuery.getId());
            }

            return indexRequest;
        });
    }

    private Map<String, JsonData> createOtherSettingsMap(Map<String, Object> settingsMap) {
        return settingsMap.entrySet()
            .stream()
            .collect(Collectors.toMap(Map.Entry::getKey, this::toJsonData));
    }

    private JsonData toJsonData(Object value) {
        return JsonData.of(value);
    }

    private void assertIndexNameNotBlank(String indexName) {
        if (StringUtils.isBlank(indexName)) {
            throw new IllegalArgumentException("Index name cannot be empty");
        }
    }
}
