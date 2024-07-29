package com.icthh.xm.ms.entity.service.search;

import com.icthh.xm.commons.permission.service.PermissionCheckService;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.repository.search.translator.SpelToElasticTranslator;
import com.icthh.xm.ms.entity.service.dto.SearchDto;
import com.icthh.xm.ms.entity.service.search.builder.BoolQueryBuilder;
import com.icthh.xm.ms.entity.service.search.builder.NativeSearchQueryBuilder;
import com.icthh.xm.ms.entity.service.search.filter.FetchSourceFilter;
import com.icthh.xm.ms.entity.service.search.mapper.extractor.ResultsExtractor;
import com.icthh.xm.ms.entity.service.search.page.ScrolledPage;
import com.icthh.xm.ms.entity.service.search.page.aggregation.AggregatedPage;
import com.icthh.xm.ms.entity.service.search.query.SearchQuery;
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
    private final PermissionCheckService permissionCheckService;
    private final SpelToElasticTranslator spelToElasticTranslator;
    private final IndexRequestService indexRequestService;
    private final SearchRequestService searchRequestService;

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
            .withTypes(ElasticsearchTemplateWrapper.INDEX_QUERY_TYPE)
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
            .withTypes(ElasticsearchTemplateWrapper.INDEX_QUERY_TYPE)
            .withPageable(pageable == null ? DEFAULT_PAGE : pageable)
            .build();

        return queryForPage(queryBuilder, XmEntity.class);
    }

    @Override
    public <T> boolean createIndex(Class<T> clazz) {
        return createIndex(getIndexName());
    }

    @Override
    public boolean createIndex(String indexName) {
        return indexRequestService.createIndex(indexName);
    }

    @Override
    public boolean createIndex(String indexName, Object settings) {
        return indexRequestService.createIndex(indexName, settings);
    }

    @Override
    public <T> boolean putMapping(Class<T> clazz) {
        return searchRequestService.putMapping(getIndexName(), getDefaultMapping());
    }

    @Override
    public boolean putMapping(String indexName, String type, Object mappings) {
        return searchRequestService.putMapping(indexName, mappings);
    }

    @Override
    public <T> boolean putMapping(Class<T> clazz, Object mappings) {
        return searchRequestService.putMapping(getIndexName(), mappings);
    }

    @Override
    public <T> T queryForObject(GetQuery query, Class<T> clazz) {
        return searchRequestService.queryForObject(query, clazz, getIndexName());
    }

    @Override
    public <T> AggregatedPage<T> queryForPage(SearchQuery query, Class<T> clazz) {
        return searchRequestService.queryForPage(query, clazz);
    }

    @Override
    public <T> List<T> queryForList(SearchQuery query, Class<T> clazz) {
        return searchRequestService.queryForList(query, clazz);
    }

    @Override
    public <T> List<String> queryForIds(SearchQuery query) {
        return searchRequestService.queryForIds(query);
    }

    @Override
    public <T> long count(SearchQuery query, Class<T> clazz) {
        return searchRequestService.count(query, getIndexName());
    }

    @Override
    public <T> long count(SearchQuery query) {
        return count(query, null);
    }

    @Override
    public String index(IndexQuery query) {
        return indexRequestService.index(query);
    }

    @Override
    public void bulkIndex(List<IndexQuery> queries) {
        indexRequestService.bulkIndex(queries);
    }

    @Override
    public String delete(String indexName, String type, String id) {
        return searchRequestService.delete(id, indexName);
    }

    @Override
    public <T> String delete(Class<T> clazz, String id) {
        return searchRequestService.delete(id, getIndexName());
    }

    @Override
    public <T> void delete(DeleteQuery query, Class<T> clazz) {
        searchRequestService.deleteByQuery(query, getIndexName());
    }

    @Override
    public void delete(DeleteQuery query) {
        searchRequestService.deleteByQuery(query, getIndexName());
    }

    @Override
    public <T> boolean deleteIndex(Class<T> clazz) {
        return deleteIndex(getIndexName());
    }

    @Override
    public boolean deleteIndex(String indexName) {
        return indexRequestService.deleteIndex(indexName);
    }

    @Override
    public void refresh(String indexName) {
        indexRequestService.refresh(indexName);
    }

    @Override
    public <T> void refresh(Class<T> clazz) {
        refresh(getIndexName());
    }

    @Override
    public <T> Page<T> startScroll(long scrollTimeInMillis, SearchQuery query, Class<T> clazz) {
        return searchRequestService.startScroll(scrollTimeInMillis, query, clazz);
    }

    @Override
    public <T> Page<T> continueScroll(String scrollId, long scrollTimeInMillis, Class<T> clazz) {
        return searchRequestService.continueScroll(scrollId, scrollTimeInMillis, clazz);
    }

    @Override
    public <T> void clearScroll(String scrollId) {
        searchRequestService.clearScroll(scrollId);
    }

    @Override
    public <T> T query(SearchQuery query, ResultsExtractor<T> resultsExtractor) {
        return searchRequestService.query(query, resultsExtractor);
    }

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
            .withTypes(ElasticsearchTemplateWrapper.INDEX_QUERY_TYPE)
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

}
