package com.icthh.xm.ms.entity.service.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Time;
import co.elastic.clients.elasticsearch._types.mapping.*;
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
import co.elastic.clients.elasticsearch.indices.PutMappingRequest;
import co.elastic.clients.elasticsearch.indices.PutMappingResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

        Map<String, Property> mappingProperties = createMappingProperties(properties);
        PutMappingRequest request = PutMappingRequest.of(b -> b.index(indexName).properties(mappingProperties));

        PutMappingResponse response = elasticsearchClient.indices().putMapping(request);

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

    private Map<String, Property> createMappingProperties(Object properties) {
        Map<String, Object> settingsMap = objectMapper.convertValue(properties, new TypeReference<Map<String, Object>>() {});
        Map<String, Object> propertiesMap = (Map<String, Object>) settingsMap.getOrDefault("properties", new HashMap<String, Object>());

        return propertiesMap.entrySet()
            .stream()
            .collect(Collectors.toMap(Map.Entry::getKey, this::toProperty));
    }

    private Property toProperty(Object properties) {
        Map<String, Object> fieldMapping = (Map<String, Object>) properties;
        String type = (String) fieldMapping.get("type");
        PropertyVariant propertyVariant = resolvePropertyVariantByType(type);
        return new Property(propertyVariant);
    }

    private PropertyVariant resolvePropertyVariantByType(String type) {
        Property.Kind kind = Property.Kind.valueOf(type);

        switch (kind) {
            case Text:
                return new TextProperty.Builder().build();
            case Keyword:
                return new KeywordProperty.Builder().build();
            case Long:
                return new LongNumberProperty.Builder().build();
            case Integer:
                return new IntegerNumberProperty.Builder().build();
            case Double:
                return new DoubleNumberProperty.Builder().build();
            case Float:
                return new FloatNumberProperty.Builder().build();
            case Date:
                return new DateProperty.Builder().build();
            case Boolean:
                return new BooleanProperty.Builder().build();
            case Object:
                return new ObjectProperty.Builder().build();
            case Nested:
                return new NestedProperty.Builder().build();
            default:
                throw new IllegalArgumentException("Unsupported property type: " + type);
        }
    }
}
