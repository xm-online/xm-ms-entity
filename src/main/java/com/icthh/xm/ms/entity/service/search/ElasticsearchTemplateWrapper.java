package com.icthh.xm.ms.entity.service.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.icthh.xm.commons.permission.service.PermissionCheckService;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.repository.search.translator.SpelToElasticTranslator;
import com.icthh.xm.ms.entity.service.dto.SearchDto;
import com.icthh.xm.ms.entity.service.search.builder.BoolQueryBuilder;
import com.icthh.xm.ms.entity.service.search.builder.NativeSearchQueryBuilder;
import com.icthh.xm.ms.entity.service.search.filter.FetchSourceFilter;
import com.icthh.xm.ms.entity.service.search.mapper.GetResultMapper;
import com.icthh.xm.ms.entity.service.search.mapper.SearchRequestBuilder;
import com.icthh.xm.ms.entity.service.search.mapper.SearchResultMapper;
import com.icthh.xm.ms.entity.service.search.mapper.extractor.ResultsExtractor;
import com.icthh.xm.ms.entity.service.search.page.ScrolledPage;
import com.icthh.xm.ms.entity.service.search.page.aggregation.AggregatedPage;
import com.icthh.xm.ms.entity.service.search.query.CriteriaQuery;
import com.icthh.xm.ms.entity.service.search.query.SearchQuery;
import com.icthh.xm.ms.entity.service.search.query.StringQuery;
import com.icthh.xm.ms.entity.service.search.query.dto.DeleteQuery;
import com.icthh.xm.ms.entity.service.search.query.dto.GetQuery;
import com.icthh.xm.ms.entity.service.search.query.dto.IndexQuery;
import com.icthh.xm.ms.entity.service.search.query.dto.NativeSearchQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.icthh.xm.ms.entity.service.search.builder.QueryBuilders.boolQuery;
import static com.icthh.xm.ms.entity.service.search.builder.QueryBuilders.matchQuery;
import static com.icthh.xm.ms.entity.service.search.builder.QueryBuilders.prefixQuery;
import static com.icthh.xm.ms.entity.service.search.builder.QueryBuilders.queryStringQuery;
import static com.icthh.xm.ms.entity.service.search.builder.QueryBuilders.simpleQueryStringQuery;
import static com.icthh.xm.ms.entity.service.search.builder.QueryBuilders.termsQuery;
import static com.icthh.xm.ms.entity.service.search.query.AbstractQuery.DEFAULT_PAGE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang.StringUtils.isEmpty;

@Slf4j
@Service
@RequiredArgsConstructor
public class ElasticsearchTemplateWrapper implements ElasticsearchOperations {

    public static final String INDEX_QUERY_TYPE = "xmentity";

    private static final String AND = " AND ";
    private static final String TYPE_KEY = "typeKey";

    private final TenantContextHolder tenantContextHolder;
    private final ElasticsearchClient elasticsearchClient;
    private final PermissionCheckService permissionCheckService;
    private final SpelToElasticTranslator spelToElasticTranslator;
    private final SearchResultMapper searchResultMapper;
    private final SearchRequestBuilder searchRequestBuilder;

    public static String composeIndexName(String tenantCode) {
        return tenantCode.toLowerCase() + "_" + INDEX_QUERY_TYPE;
    }

    public String getIndexName() {
        String tenantKey = tenantContextHolder.getTenantKey();
        return composeIndexName(tenantKey);
    }

    public <T> List<T> search(String query, Class<T> entityClass, String privilegeKey) {
        return queryForList(buildQuery(query, null, privilegeKey, null), entityClass);
    }

    public <T> Page<T> searchForPage(SearchDto searchDto, String privilegeKey) {
        FetchSourceFilter fetchSourceFilter = null;
        if (searchDto.getFetchSourceFilter() != null) {
            fetchSourceFilter = new FetchSourceFilter(searchDto.getFetchSourceFilter().getIncludes(), searchDto.getFetchSourceFilter().getExcludes());
        }
        SearchQuery query = buildQuery(searchDto.getQuery(), searchDto.getPageable(), privilegeKey, fetchSourceFilter);
        return queryForPage(query, searchDto.getEntityClass());
    }

    public <T> Page<T> search(Long scrollTimeInMillis,
                              String query,
                              Pageable pageable,
                              Class<T> entityClass,
                              String privilegeKey) {

        String scrollId = null;
        List<T> resultList = new ArrayList<>();
        try {
            ScrolledPage<T> scrollResult = (ScrolledPage<T>) startScroll(scrollTimeInMillis,
                buildQuery(query, pageable, privilegeKey, null), entityClass);

            scrollId = scrollResult.getScrollId();

            while (scrollResult.hasContent()) {
                resultList.addAll(scrollResult.getContent());
                scrollId = scrollResult.getScrollId();

                scrollResult = (ScrolledPage<T>) continueScroll(scrollId, scrollTimeInMillis, entityClass);
            }
        } finally {
            if (nonNull(scrollId)) {
                clearScroll(scrollId);
            }
        }
        return new PageImpl<>(resultList, pageable, resultList.size());
    }

    public Page<XmEntity> searchByQueryAndTypeKey(String query,
                                                  String typeKey,
                                                  Pageable pageable,
                                                  String privilegeKey) {
        String permittedQuery = buildPermittedQuery(query, privilegeKey);

        BoolQueryBuilder typeKeyQuery = typeKeyQuery(typeKey);

        val esQuery = isEmpty(permittedQuery)
            ? boolQuery().must(typeKeyQuery)
            : typeKeyQuery.must(simpleQueryStringQuery(permittedQuery));

        log.debug("Executing DSL '{}'", esQuery);

        String indexName = ElasticsearchTemplateWrapper.composeIndexName(tenantContextHolder.getTenantKey());
        NativeSearchQuery queryBuilder = new NativeSearchQueryBuilder()
            .withIndices(indexName)
//            .withTypes(ElasticsearchTemplateWrapper.INDEX_QUERY_TYPE) TODO-IMPL: Removed in 8.14v
            .withQuery(esQuery)
            .withPageable(pageable == null ? DEFAULT_PAGE : pageable)
            .build();

        return queryForPage(queryBuilder, XmEntity.class);
    }

    public Page<XmEntity> searchWithIdNotIn(String query, Set<Long> ids,
                                            String targetEntityTypeKey,
                                            Pageable pageable, String privilegeKey) {
        String permittedQuery = buildPermittedQuery(query, privilegeKey);

        BoolQueryBuilder typeKeyQuery = typeKeyQuery(targetEntityTypeKey);

        BoolQueryBuilder idNotIn = boolQuery()
            .mustNot(termsQuery("id", ids));
        var esQuery = isEmpty(permittedQuery)
            ? idNotIn.must(typeKeyQuery)
            : idNotIn.must(simpleQueryStringQuery(permittedQuery)).must(typeKeyQuery);

        log.info("Executing DSL '{}'", esQuery);

        NativeSearchQuery queryBuilder = new NativeSearchQueryBuilder()
            .withQuery(esQuery)
            .withIndices(getIndexName())
//            .withTypes(ElasticsearchTemplateWrapper.INDEX_QUERY_TYPE) TODO-IMPL: Removed in 8.14v
            .withPageable(pageable == null ? DEFAULT_PAGE : pageable)
            .build();

        return queryForPage(queryBuilder, XmEntity.class);
    }

    // TODO-REMOVED: Unused method
//    @Override
//    public ElasticsearchConverter getElasticsearchConverter() {
//        return elasticsearchTemplate.getElasticsearchConverter();
//    }

    // TODO-REMOVED: Unused method (check leps for usage otherwise remove)
//    @Override
//    public ElasticsearchClient getClient() {
//        return elasticsearchClient;
//    }

    @Override
    public <T> boolean createIndex(Class<T> clazz) {
        return createIndex(getIndexName());
    }

    @Override
    public boolean createIndex(String indexName) {
//        return elasticsearchTemplate.createIndex(indexName);
        return false;
    }

    @Override
    public boolean createIndex(String indexName, Object settings) {
//        return elasticsearchTemplate.createIndex(indexName, settings);
        return false;
    }

    // TODO-REMOVE: Unused methods
//    @Override
//    public <T> boolean createIndex(Class<T> clazz, Object settings) {
//        throw new UnsupportedOperationException("Not implemented");
//    }

    @Override
    public <T> boolean putMapping(Class<T> clazz) {
//        return elasticsearchTemplate.putMapping(getIndexName(), ElasticsearchTemplateWrapper.INDEX_QUERY_TYPE, getDefaultMapping());
        return false;
    }

    @Override
    public boolean putMapping(String indexName, String type, Object mappings) {
//        return elasticsearchTemplate.putMapping(indexName, type, mappings);
        return false;
    }

    @Override
    public <T> boolean putMapping(Class<T> clazz, Object mappings) {
//        return elasticsearchTemplate.putMapping(getIndexName(), ElasticsearchTemplateWrapper.INDEX_QUERY_TYPE, mappings);
        return false;
    }

    // TODO-REMOVE: Check usage otherwise remove unused methods
//    @Override
//    public <T> Map getMapping(Class<T> clazz) {
//        throw new UnsupportedOperationException("Not implemented");
//    }
//
//    @Override
//    public Map getMapping(String indexName, String type) {
//        throw new UnsupportedOperationException("Not implemented");
//    }
//
//    @Override
//    public Map getSetting(String indexName) {
//        throw new UnsupportedOperationException("Not implemented");
//    }
//
//    @Override
//    public <T> Map getSetting(Class<T> clazz) {
//        throw new UnsupportedOperationException("Not implemented");
//    }

    @Override
    public <T> T queryForObject(GetQuery query, Class<T> clazz) {
//        TODO-IMPL
//        return queryForObject(query, clazz, resultsMapper);
        return null;
    }

    @Override
    public <T> T queryForObject(GetQuery query, Class<T> clazz, GetResultMapper mapper) {
//        TODO-IMPL
//
//        GetResponse response = elasticsearchTemplate.getClient()
//            .prepareGet(getIndexName(), ElasticsearchTemplateWrapper.INDEX_QUERY_TYPE, query.getId()).execute()
//            .actionGet();
//
//        T entity = mapper.mapResult(response, clazz);
//        return entity;
        return null;
    }

    @Override
    public <T> T queryForObject(CriteriaQuery query, Class<T> clazz) {
//        TODO-IMPL
//        return elasticsearchTemplate.queryForObject(query, clazz);
        return null;
    }

    @Override
    public <T> T queryForObject(StringQuery query, Class<T> clazz) {
//        TODO-IMPL
//        return elasticsearchTemplate.queryForObject(query, clazz);
        return null;
    }

    @Override
    public <T> AggregatedPage<T> queryForPage(SearchQuery query, Class<T> clazz) {
        Pageable pageable = query.getPageable();

        SearchRequest request = searchRequestBuilder.buildSearchRequest(query);

        SearchResponse<T> search = search(request, clazz);
        return searchResultMapper.mapSearchResults(search, pageable);
    }

    @Override
    public <T> Page<T> queryForPage(SearchQuery query, Class<T> clazz, SearchResultMapper mapper) {
//        TODO-IMPL
//        return elasticsearchTemplate.queryForPage(query, clazz, mapper);
        return null;
    }

    @Override
    public <T> Page<T> queryForPage(CriteriaQuery query, Class<T> clazz) {
//        TODO-IMPL
//        return elasticsearchTemplate.queryForPage(query, clazz);
        return null;
    }

    @Override
    public <T> Page<T> queryForPage(StringQuery query, Class<T> clazz) {
//        TODO-IMPL
//        return elasticsearchTemplate.queryForPage(query, clazz);
        return null;
    }

    @Override
    public <T> Page<T> queryForPage(StringQuery query, Class<T> clazz, SearchResultMapper mapper) {
//        TODO-IMPL
//        return elasticsearchTemplate.queryForPage(query, clazz, mapper);
        return null;
    }

    // TODO-REMOVE: Unused methods
//    @Override
//    public <T> CloseableIterator<T> stream(CriteriaQuery query, Class<T> clazz) {
//        throw new UnsupportedOperationException("Not implemented");
//    }
//
//    @Override
//    public <T> CloseableIterator<T> stream(SearchQuery query, Class<T> clazz) {
//        throw new UnsupportedOperationException("Not implemented");
//    }
//
//    @Override
//    public <T> CloseableIterator<T> stream(SearchQuery query, Class<T> clazz, SearchResultMapper mapper) {
//        throw new UnsupportedOperationException("Not implemented");
//    }

    @Override
    public <T> List<T> queryForList(CriteriaQuery query, Class<T> clazz) {
//        TODO-IMPL
//        return elasticsearchTemplate.queryForList(query, clazz);
        return null;
    }

    @Override
    public <T> List<T> queryForList(StringQuery query, Class<T> clazz) {
//        TODO-IMPL
//        return elasticsearchTemplate.queryForList(query, clazz);
        return null;
    }

    @Override
    public <T> List<T> queryForList(SearchQuery query, Class<T> clazz) {
        SearchRequest request = searchRequestBuilder.buildSearchRequest(query);
        SearchResponse<T> searchResponse = search(request, clazz);

        return searchResultMapper.mapListSearchResults(searchResponse);
    }

    @Override
    public <T> List<String> queryForIds(SearchQuery query) {
//        TODO-IMPL
//        return elasticsearchTemplate.queryForIds(query);
        return null;
    }

    @Override
    public <T> long count(CriteriaQuery query, Class<T> clazz) {
//        TODO-IMPL
//        return elasticsearchTemplate.count(query, clazz);
        return 0;
    }

    @Override
    public <T> long count(CriteriaQuery query) {
//        TODO-IMPL
//        return elasticsearchTemplate.count(query);
        return 0;
    }

    @Override
    public <T> long count(SearchQuery query, Class<T> clazz) {
//        TODO-IMPL
//        return elasticsearchTemplate.count(query, clazz);
        return 0;
    }

    @Override
    public <T> long count(SearchQuery query) {
//        TODO-IMPL
//        return elasticsearchTemplate.count(query);
        return 0;
    }

    // TODO-REMOVE: Unused methods
//    @Override
//    public <T> LinkedList<T> multiGet(SearchQuery searchQuery, Class<T> clazz) {
//        throw new UnsupportedOperationException("Not implemented");
//    }
//
//    @Override
//    public <T> LinkedList<T> multiGet(SearchQuery searchQuery, Class<T> clazz, MultiGetResultMapper multiGetResultMapper) {
//        throw new UnsupportedOperationException("Not implemented");
//    }

    @Override
    public String index(IndexQuery query) {
//        TODO-IMPL
//        return prepareIndex(query).execute().actionGet().getId();
        return null;
    }

    // TODO-REMOVE: Unused methods
//    @Override
//    public UpdateResponse update(UpdateQuery updateQuery) {
//        throw new UnsupportedOperationException("Not implemented");
//    }

    @Override
    public void bulkIndex(List<IndexQuery> queries) {
//        TODO-IMPL
//        elasticsearchTemplate.bulkIndex(queries);
    }

    // TODO-REMOVE: Unused methods
    //    @Override
//    public void bulkUpdate(List<UpdateQuery> queries) {
//        throw new UnsupportedOperationException("Not implemented");
//    }

    @Override
    public String delete(String indexName, String type, String id) {
//        TODO-IMPL
//        return elasticsearchTemplate.delete(indexName, type, id);
        return null;
    }

    @Override
    public <T> void delete(CriteriaQuery criteriaQuery, Class<T> clazz) {
//        TODO-IMPL
//        elasticsearchTemplate.delete(criteriaQuery, clazz);
    }

    @Override
    public <T> String delete(Class<T> clazz, String id) {
//        TODO-IMPL
//        return elasticsearchTemplate.delete(clazz, id);
        return null;
    }

    @Override
    public <T> void delete(DeleteQuery query, Class<T> clazz) {
//        TODO-IMPL
//        elasticsearchTemplate.delete(query, clazz);
    }

    @Override
    public void delete(DeleteQuery query) {
//        TODO-IMPL
//        elasticsearchTemplate.delete(query);
    }

    @Override
    public <T> boolean deleteIndex(Class<T> clazz) {
        return deleteIndex(getIndexName());
    }

    @Override
    public boolean deleteIndex(String indexName) {
//        TODO-IMPL
//        return elasticsearchTemplate.deleteIndex(indexName);
        return false;
    }

    @Override
    public <T> boolean indexExists(Class<T> clazz) {
//        TODO-IMPL
//        return indexExists(getIndexName());
        return false;
    }

    @Override
    public boolean indexExists(String indexName) {
//        TODO-IMPL
//        return elasticsearchTemplate.indexExists(indexName);
        return false;
    }

    @Override
    public boolean typeExists(String index, String type) {
//        TODO-IMPL
//        return elasticsearchTemplate.typeExists(index, type);
        return false;
    }

    @Override
    public void refresh(String indexName) {
//        TODO-IMPL
//        elasticsearchTemplate.refresh(indexName);
    }

    @Override
    public <T> void refresh(Class<T> clazz) {
        refresh(getIndexName());
    }

    @Override
    public <T> Page<T> startScroll(long scrollTimeInMillis, SearchQuery query, Class<T> clazz) {
//        TODO-IMPL
//        return elasticsearchTemplate.startScroll(scrollTimeInMillis, query, clazz);
        return null;
    }

    @Override
    public <T> Page<T> startScroll(long scrollTimeInMillis, SearchQuery query, Class<T> clazz, SearchResultMapper mapper) {
//        TODO-IMPL
//        return elasticsearchTemplate.startScroll(scrollTimeInMillis, query, clazz, mapper);
        return null;
    }

    @Override
    public <T> Page<T> startScroll(long scrollTimeInMillis, CriteriaQuery criteriaQuery, Class<T> clazz) {
//        TODO-IMPL
//        return elasticsearchTemplate.startScroll(scrollTimeInMillis, criteriaQuery, clazz);
        return null;
    }

    @Override
    public <T> Page<T> startScroll(long scrollTimeInMillis, CriteriaQuery criteriaQuery, Class<T> clazz, SearchResultMapper mapper) {
//        TODO-IMPL
//        return elasticsearchTemplate.startScroll(scrollTimeInMillis, criteriaQuery, clazz, mapper);
        return null;
    }

    @Override
    public <T> Page<T> continueScroll(String scrollId, long scrollTimeInMillis, Class<T> clazz) {
//        TODO-IMPL
//        return elasticsearchTemplate.continueScroll(scrollId, scrollTimeInMillis, clazz);
        return null;
    }

    @Override
    public <T> Page<T> continueScroll(String scrollId, long scrollTimeInMillis, Class<T> clazz, SearchResultMapper mapper) {
//        TODO-IMPL
//        return elasticsearchTemplate.continueScroll(scrollId, scrollTimeInMillis, clazz, mapper);
        return null;
    }

    @Override
    public <T> void clearScroll(String scrollId) {
//        TODO-IMPL
//        elasticsearchTemplate.clearScroll(scrollId);
    }

    // TODO-REMOVE: Unused methods
//    @Override
//    public <T> Page<T> moreLikeThis(MoreLikeThisQuery query, Class<T> clazz) {
//        throw new UnsupportedOperationException("Not implemented");
//    }
//
//    @Override
//    public Boolean addAlias(AliasQuery query) {
//        throw new UnsupportedOperationException("Not implemented");
//    }
//
//    @Override
//    public Boolean removeAlias(AliasQuery query) {
//        throw new UnsupportedOperationException("Not implemented");
//    }
//
//    @Override
//    public List<AliasMetaData> queryForAlias(String indexName) {
//        throw new UnsupportedOperationException("Not implemented");
//    }

    @Override
    public <T> T query(SearchQuery query, ResultsExtractor<T> resultsExtractor) {
        SearchRequest request = searchRequestBuilder.buildSearchRequest(query);

        SearchResponse<Map> response = search(request, Map.class);

        var searchResponse = searchResultMapper.mapSearchResponse(response);
        return resultsExtractor.extract(searchResponse);
    }

    // TODO-REMOVE: Unused method
//    @Override
//    public ElasticsearchPersistentEntity getPersistentEntityFor(Class clazz) {
//        throw new UnsupportedOperationException("Not implemented");
//    }

//    TODO-IMPL
//    private IndexRequestBuilder prepareIndex(IndexQuery query) {
//        String indexName = query.getIndexName();
//        String type = query.getType();
//
//        IndexRequestBuilder indexRequestBuilder = null;
//        try {
//            Client client = getClient();
//            if (query.getObject() != null) {
//                String id = query.getId();
//                if (id != null) {
//                    indexRequestBuilder = client.prepareIndex(indexName, type, id);
//                } else {
//                    indexRequestBuilder = client.prepareIndex(indexName, type);
//                }
//                indexRequestBuilder.setSource(objectMapper.writeValueAsString(query.getObject()),
//                    Requests.INDEX_CONTENT_TYPE);
//            } else if (query.getSource() != null) {
//                indexRequestBuilder = client.prepareIndex(indexName, type, query.getId()).setSource(query.getSource(),
//                    Requests.INDEX_CONTENT_TYPE);
//            } else {
//                throw new ElasticsearchException(
//                    "object or source is null, failed to index the document [id: " + query.getId() + "]");
//            }
//
//            if (query.getParentId() != null) {
//                indexRequestBuilder.setParent(query.getParentId());
//            }
//
//            return indexRequestBuilder;
//        } catch (IOException e) {
//            throw new ElasticsearchException("failed to index the document [id: " + query.getId() + "]", e);
//        }
//    }

    private String getDefaultMapping() {
        String location = "/config/elastic/default-mapping.json";
        try {
            return IOUtils.toString(new ClassPathResource(location).getInputStream(), UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private SearchQuery buildQuery(String query, Pageable pageable, String privilegeKey, FetchSourceFilter fetchSourceFilter) {
        String permittedQuery = buildPermittedQuery(query, privilegeKey);

        log.debug("Executing DSL '{}'", permittedQuery);

        String indexName = ElasticsearchTemplateWrapper.composeIndexName(tenantContextHolder.getTenantKey());
        return new NativeSearchQueryBuilder()
            .withIndices(indexName)
//            .withTypes(ElasticsearchTemplateWrapper.INDEX_QUERY_TYPE) TODO-IMPL: Removed in 8.0
            .withQuery(queryStringQuery(permittedQuery))
            .withSourceFilter(fetchSourceFilter)
            .withPageable(pageable == null ? DEFAULT_PAGE : pageable)
            .build();
    }


    private String buildPermittedQuery(String query, String privilegeKey) {
        String permittedQuery = query;

        String permittedCondition = createPermissionCondition(privilegeKey);
        if (StringUtils.isNotBlank(permittedCondition)) {
            if (StringUtils.isBlank(query)) {
                permittedQuery = permittedCondition;
            } else {
                permittedQuery += AND + "(" + permittedCondition + ")";
            }
        }

        return permittedQuery;
    }

    private String createPermissionCondition(String privilegeKey) {
        return permissionCheckService.createCondition(
            SecurityContextHolder.getContext().getAuthentication(), privilegeKey,
            spelToElasticTranslator);
    }

    private BoolQueryBuilder typeKeyQuery(String typeKey) {
        val prefix = typeKey + ".";
        return boolQuery()
            .should(matchQuery(TYPE_KEY, typeKey))
            .should(prefixQuery(TYPE_KEY, prefix))
            .minimumShouldMatch(1);
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
}
