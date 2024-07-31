package com.icthh.xm.ms.entity.service.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Time;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.ClearScrollRequest;
import co.elastic.clients.elasticsearch.core.CountRequest;
import co.elastic.clients.elasticsearch.core.CountResponse;
import co.elastic.clients.elasticsearch.core.DeleteByQueryRequest;
import co.elastic.clients.elasticsearch.core.DeleteRequest;
import co.elastic.clients.elasticsearch.core.GetRequest;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.ScrollRequest;
import co.elastic.clients.elasticsearch.core.ScrollResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.indices.GetIndexRequest;
import co.elastic.clients.elasticsearch.indices.GetIndexResponse;
import co.elastic.clients.elasticsearch.indices.PutMappingRequest;
import co.elastic.clients.elasticsearch.indices.PutMappingResponse;
import co.elastic.clients.json.JsonpDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.ms.entity.service.search.deserializer.RequestDeserializer;
import com.icthh.xm.ms.entity.service.search.mapper.SearchRequestBuilder;
import com.icthh.xm.ms.entity.service.search.mapper.SearchResultMapper;
import com.icthh.xm.ms.entity.service.search.mapper.extractor.ResultsExtractor;
import com.icthh.xm.ms.entity.service.search.page.aggregation.AggregatedPage;
import com.icthh.xm.ms.entity.service.search.query.SearchQuery;
import com.icthh.xm.ms.entity.service.search.query.dto.DeleteQuery;
import com.icthh.xm.ms.entity.service.search.query.dto.GetQuery;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchRequestService {

    private final SearchResultMapper searchResultMapper;
    private final SearchRequestBuilder searchRequestBuilder;
    private final ElasticsearchClient elasticsearchClient;
    private final ObjectMapper objectMapper;

    public <T> AggregatedPage<T> queryForPage(SearchQuery query, Class<T> clazz) {
        Pageable pageable = query.getPageable();

        SearchRequest request = searchRequestBuilder.buildSearchRequest(query);

        SearchResponse<T> search = search(request, clazz);
        return searchResultMapper.mapSearchResults(search, pageable);
    }

    public <T> List<T> queryForList(SearchQuery query, Class<T> clazz) {
        SearchRequest request = searchRequestBuilder.buildSearchRequest(query);
        SearchResponse<T> searchResponse = search(request, clazz);

        return searchResultMapper.mapListSearchResults(searchResponse.hits());
    }

    @SneakyThrows
    public <T> T queryForObject(GetQuery query, Class<T> clazz, String indexName) {
        Assert.notNull(indexName, "Index name is required");

        GetRequest getRequest = GetRequest.of(g -> g
            .index(indexName)
            .id(query.getId())
        );

        GetResponse<T> response = elasticsearchClient.get(getRequest, clazz);
        return response.found() ? response.source() : null;
    }

    public <T> List<String> queryForIds(SearchQuery query) {
        SearchRequest request = searchRequestBuilder.buildSearchRequest(query);
        SearchResponse<Map> searchResponse = search(request, Map.class);

        return searchResultMapper.mapIdsSearchResults(searchResponse);
    }

    @SneakyThrows
    public <T> long count(SearchQuery query, String indexName) {
        SearchRequest searchRequest = searchRequestBuilder.buildSearchRequest(query);
        List<String> index = CollectionUtils.isEmpty(searchRequest.index()) ? List.of(indexName) : searchRequest.index();

        CountRequest countRequest = CountRequest.of(request -> request
            .index(index)
            .query(searchRequest.query()));

        CountResponse count = elasticsearchClient.count(countRequest);
        return count.count();
    }

    public <T> T query(SearchQuery query, ResultsExtractor<T> resultsExtractor) {
        SearchRequest request = searchRequestBuilder.buildSearchRequest(query);

        SearchResponse<Map> response = search(request, Map.class);

        var searchResponse = searchResultMapper.mapSearchResponse(response);
        return resultsExtractor.extract(searchResponse);
    }

    @SneakyThrows
    public String delete(String id, String indexName) {
        Assert.notNull(indexName, "Index name is required");

        DeleteRequest deleteRequest = DeleteRequest.of(request -> request
            .index(indexName)
            .id(id));

        return elasticsearchClient.delete(deleteRequest).id();
    }

    @SneakyThrows
    public void deleteByQuery(DeleteQuery deleteQuery, String defaultIndexName) {
        Query query = searchRequestBuilder.buildSearchQuery(deleteQuery.getQuery());

        String indexName = !StringUtils.isBlank(deleteQuery.getIndex()) ? deleteQuery.getIndex()
            : defaultIndexName;
        int pageSize = deleteQuery.getPageSize() != null ? deleteQuery.getPageSize() : 1000;
        Long scrollTimeInMillis = deleteQuery.getScrollTimeInMillis() != null ? deleteQuery.getScrollTimeInMillis()
            : 10000L;

        DeleteByQueryRequest deleteByQueryRequestBuilder = DeleteByQueryRequest.of(d -> d
            .index(indexName)
            .query(query)
            .scroll(Time.of(time -> time.time(scrollTimeInMillis + "ms")))
            .maxDocs((long) pageSize)
        );

        //TODO: test maxDocs is correct mapping to page size?
        elasticsearchClient.deleteByQuery(deleteByQueryRequestBuilder);
    }

    // TODO: check if mapping is null would it override?
    @SneakyThrows
    public boolean putMapping(String indexName, Object properties) {
        Assert.notNull(indexName, "Index name is required");

        PutMappingRequest.Builder builder = new PutMappingRequest.Builder();
        builder.index(indexName);

        JsonpDeserializer<PutMappingRequest> builderDeserializer = RequestDeserializer.getPutMappingBuilderDeserializer(builder);
        PutMappingRequest deserializedRequest = RequestDeserializer.deserializeSettings(builderDeserializer, properties, objectMapper);

        PutMappingResponse response = elasticsearchClient.indices().putMapping(deserializedRequest);

        return response.acknowledged();
    }

    public <T> Page<T> startScroll(long scrollTimeInMillis, SearchQuery query, Class<T> clazz) {
        Assert.notNull(query.getIndices(), "No index defined for Query");
        Assert.notNull(query.getPageable(), "Query.pageable is required for scan & scroll");

        SearchRequest request = searchRequestBuilder.buildScrollSearchRequest(query, scrollTimeInMillis);

        SearchResponse<T> response = search(request, clazz);
        return searchResultMapper.mapSearchResults(response, query.getPageable());
    }

    @SneakyThrows
    public <T> Page<T> continueScroll(String scrollId, long scrollTimeInMillis, Class<T> clazz) {
        ScrollRequest scrollRequest = ScrollRequest.of(request -> request
            .scrollId(scrollId)
            .scroll(Time.of(time -> time.time(scrollTimeInMillis + "ms"))));

        ScrollResponse<T> scroll = elasticsearchClient.scroll(scrollRequest, clazz);
        return searchResultMapper.mapScrollSearchResults(scroll, Pageable.unpaged());
    }

    @SneakyThrows
    public void clearScroll(String scrollId) {
        ClearScrollRequest clearScrollRequest = ClearScrollRequest.of(request -> request.scrollId(scrollId));
        elasticsearchClient.clearScroll(clearScrollRequest);
    }

    private <T> SearchResponse<T> search(SearchRequest searchRequest, Class<T> clazz) {
        try {
            SearchResponse<T> response = elasticsearchClient.search(searchRequest, clazz);
            log.info("Sent elasticsearch request: {} with result size: {}", searchRequest.toString(), response.hits().hits().size());

            return response;
        } catch (Exception e) {
            log.error("Error sending elasticsearch request: {}", e.getMessage(), e);
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    @SneakyThrows
    public List<String> getAllIndexes() {
        GetIndexRequest getIndexRequest = GetIndexRequest.of(request -> request.index("*"));
        GetIndexResponse getIndexResponse = elasticsearchClient.indices().get(getIndexRequest);
        return new ArrayList<>(getIndexResponse.result().keySet());
    }
}
