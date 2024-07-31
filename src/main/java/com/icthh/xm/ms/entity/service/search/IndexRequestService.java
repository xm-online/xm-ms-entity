package com.icthh.xm.ms.entity.service.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.core.bulk.IndexOperation;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.IndexSettings;
import co.elastic.clients.elasticsearch.indices.RefreshRequest;
import co.elastic.clients.json.JsonData;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.ms.entity.service.search.deserializer.RequestDeserializer;
import com.icthh.xm.ms.entity.service.search.query.dto.IndexQuery;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.HashMap;
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
        Assert.notNull(indexName, "No index name defined for create operation");

        CreateIndexRequest.Builder createIndexRequestBuilder = new CreateIndexRequest.Builder()
            .index(indexName);

        if (settings != null) {
            IndexSettings indexSettings = RequestDeserializer.deserializeSettings(IndexSettings._DESERIALIZER, settings, objectMapper);
            createIndexRequestBuilder.settings(indexSettings);
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
        Assert.notNull(indexName, "No index defined for refresh operation");
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

        BulkResponse bulkResponse = elasticsearchClient.bulk(bulkRequestBuilder.build());
        checkForBulkUpdateFailure(bulkResponse);
    }

    @SneakyThrows
    public boolean deleteIndex(String indexName) {
        Assert.notNull(indexName, "No index defined for delete operation");
        if (indexExists(indexName)) {
            return elasticsearchClient.indices()
                .delete(request -> request.index(indexName))
                .acknowledged();
        }
        return false;
    }

    @SneakyThrows
    public boolean deleteIndex(List<String> indexes) {
        if (CollectionUtils.isEmpty(indexes)) {
            log.info("No indexes available to delete");
            return false;
        }

        return elasticsearchClient.indices()
            .delete(request -> request.index(indexes))
            .acknowledged();
    }

    @SneakyThrows
    public boolean indexExists(String indexName) {
        return elasticsearchClient.indices()
            .exists(request -> request.index(indexName))
            .value();
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

    private void checkForBulkUpdateFailure(BulkResponse bulkResponse) {
        if (bulkResponse.errors()) {
            Map<String, String> failedDocuments = new HashMap<>();
            for (BulkResponseItem item : bulkResponse.items()) {
                if (item.error() != null) {
                    failedDocuments.put(item.id(), item.error().reason());
                }
            }
            throw new ElasticsearchException(
                "Bulk indexing has failures. Use ElasticsearchException.getFailedDocuments() for detailed messages ["
                    + failedDocuments + "]",
                failedDocuments);
        }
    }
}
