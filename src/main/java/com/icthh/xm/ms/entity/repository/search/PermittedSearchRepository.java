package com.icthh.xm.ms.entity.repository.search;

import static org.elasticsearch.index.query.QueryBuilders.queryStringQuery;
import static org.springframework.data.elasticsearch.core.query.Query.DEFAULT_PAGE;

import com.icthh.xm.commons.permission.service.PermissionCheckService;
import com.icthh.xm.ms.entity.repository.search.translator.SpelToElasticTranslator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Repository;

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
        return getElasticsearchTemplate().queryForList(buildQuery(query, null, privilegeKey), entityClass);
    }

    /**
     * Search permitted entities.
     * @param query the elastic query
     * @param pageable the page info
     * @param entityClass the search entity class
     * @param privilegeKey the privilege key
     * @return permitted entities
     */
    public <T> Page<T> search(String query, Pageable pageable, Class<T> entityClass, String privilegeKey) {
        return getElasticsearchTemplate().queryForPage(buildQuery(query, pageable, privilegeKey), entityClass);
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
        return getElasticsearchTemplate().startScroll(scrollTimeInMillis,
                                                      buildQuery(query, pageable, privilegeKey),
                                                      entityClass);
    }

    private SearchQuery buildQuery(String query, Pageable pageable, String privilegeKey) {
        String permittedQuery = buildPermittedQuery(query, privilegeKey);

        log.debug("Executing DSL '{}'", permittedQuery);

        return new NativeSearchQueryBuilder()
            .withQuery(queryStringQuery(permittedQuery))
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
}
