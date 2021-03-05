package com.icthh.xm.ms.entity.repository.search;

import static java.util.Objects.nonNull;
import static org.elasticsearch.index.query.QueryBuilders.queryStringQuery;
import static org.springframework.data.elasticsearch.core.query.Query.DEFAULT_PAGE;

import com.icthh.xm.commons.permission.service.PermissionCheckService;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.repository.search.translator.SpelToElasticTranslator;
import com.icthh.xm.ms.entity.service.dto.SearchDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.ScrolledPage;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class PermittedSearchRepository {

    private static final String AND = " AND ";

    private final PermissionCheckService permissionCheckService;
    private final SpelToElasticTranslator spelToElasticTranslator;
    private final ElasticsearchTemplate elasticsearchTemplate;

    /**
     * Search permitted entities.
     * @param query the elastic query
     * @param entityClass the search entity class
     * @param privilegeKey the privilege key
     * @return permitted entities
     */
    public <T> List<T> search(String query, Class<T> entityClass, String privilegeKey) {
        return getElasticsearchTemplate().queryForList(buildQuery(query, null, privilegeKey, null), entityClass);
    }

    /**
     * Search permitted entities.
     * @param query the elastic query
     * @param pageable the page info
     * @param entityClass the search entity class
     * @param privilegeKey the privilege key
     * @return permitted entities
     * @deprecated use {@link #searchForPage(SearchDto)} instead
     */
    @Deprecated
    public <T> Page<T> search(String query, Pageable pageable, Class<T> entityClass, String privilegeKey) {
        return searchForPage(SearchDto.builder()
            .entityClass(entityClass)
            .pageable(pageable)
            .privilegeKey(privilegeKey)
            .query(query)
            .build());
    }

    /**
     * Search permitted entities with scroll
     * @param scrollTimeInMillis The time in millisecond for scroll feature
     * @param query the elastic query
     * @param pageable the page info
     * @param entityClass the search entity class
     * @param privilegeKey the privilege key
     * @return permitted entities
     */
    public <T> Page<T> search(Long scrollTimeInMillis,
                              String query,
                              Pageable pageable,
                              Class<T> entityClass,
                              String privilegeKey) {

        String scrollId = null;
        List<T> resultList = new ArrayList<>();
        try {
            ScrolledPage<T> scrollResult = (ScrolledPage<T>) getElasticsearchTemplate()
                .startScroll(scrollTimeInMillis, buildQuery(query, pageable, privilegeKey, null), entityClass);

            scrollId = scrollResult.getScrollId();

            while (scrollResult.hasContent()) {
                resultList.addAll(scrollResult.getContent());
                scrollId = scrollResult.getScrollId();

                scrollResult = (ScrolledPage<T>) getElasticsearchTemplate()
                    .continueScroll(scrollId, scrollTimeInMillis, entityClass);
            }
        } finally {
            if (nonNull(scrollId)) {
                getElasticsearchTemplate().clearScroll(scrollId);
            }
        }
        return new PageImpl<>(resultList, pageable, resultList.size());
    }

    private SearchQuery buildQuery(String query, Pageable pageable, String privilegeKey, FetchSourceFilter fetchSourceFilter) {
        String permittedQuery = buildPermittedQuery(query, privilegeKey);

        log.debug("Executing DSL '{}'", permittedQuery);

        return new NativeSearchQueryBuilder()
            .withQuery(queryStringQuery(permittedQuery))
            .withSourceFilter(fetchSourceFilter)
            .withPageable(pageable == null ? DEFAULT_PAGE : pageable)
            .build();
    }

    String buildPermittedQuery(String query, String privilegeKey) {
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

    // do not renamed! called from lep for not simple string query
    public ElasticsearchTemplate getElasticsearchTemplate() {
        return elasticsearchTemplate;
    }

    public <T> Page<T> searchForPage(SearchDto searchDto) {
        SearchQuery query = buildQuery(searchDto.getQuery(), searchDto.getPageable(), searchDto.getPrivilegeKey(), searchDto.getFetchSourceFilter());
        return getElasticsearchTemplate().queryForPage(query, searchDto.getEntityClass());
    }
}
