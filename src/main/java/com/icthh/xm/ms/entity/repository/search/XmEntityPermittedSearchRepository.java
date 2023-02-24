package com.icthh.xm.ms.entity.repository.search;

import com.icthh.xm.commons.permission.service.PermissionCheckService;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.repository.search.translator.SpelToElasticTranslator;
import com.icthh.xm.ms.entity.service.search.ElasticsearchTemplateWrapper;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import static org.apache.commons.lang.StringUtils.isEmpty;
import org.elasticsearch.index.query.BoolQueryBuilder;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;
import static org.elasticsearch.index.query.QueryBuilders.prefixQuery;
import static org.elasticsearch.index.query.QueryBuilders.simpleQueryStringQuery;
import static org.elasticsearch.index.query.QueryBuilders.termsQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import static org.springframework.data.elasticsearch.core.query.Query.DEFAULT_PAGE;

import org.springframework.stereotype.Repository;

import java.util.Set;

@Slf4j
@Repository
public class XmEntityPermittedSearchRepository extends PermittedSearchRepository {

    private static final String TYPE_KEY = "typeKey";

    private final TenantContextHolder tenantContextHolder;
    private final ElasticsearchTemplateWrapper elasticsearchTemplateWrapper;

    public XmEntityPermittedSearchRepository(PermissionCheckService permissionCheckService,
                                             SpelToElasticTranslator spelToElasticTranslator,
                                             ElasticsearchTemplate elasticsearchTemplate,
                                             TenantContextHolder tenantContextHolder,
                                             ElasticsearchTemplateWrapper elasticsearchTemplateWrapper) {
        super(permissionCheckService, spelToElasticTranslator, elasticsearchTemplate,
            tenantContextHolder, elasticsearchTemplateWrapper);
        this.tenantContextHolder = tenantContextHolder;
        this.elasticsearchTemplateWrapper = elasticsearchTemplateWrapper;
    }

    /**
     * Search for XmEntity by type key and query.
     * @param query the query
     * @param typeKey the type key
     * @param pageable the page info
     * @param privilegeKey the privilege key
     * @return permitted entities
     */
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

        return elasticsearchTemplateWrapper.queryForPage(queryBuilder, XmEntity.class);
    }

    private BoolQueryBuilder typeKeyQuery(String typeKey) {
        val prefix = typeKey + ".";
        return boolQuery()
            .should(matchQuery(TYPE_KEY, typeKey))
            .should(prefixQuery(TYPE_KEY, prefix))
            .minimumShouldMatch(1);
    }

    public Page<XmEntity> searchWithIdNotIn(String query, Set<Long> ids,
                                            String targetEntityTypeKey,
                                            Pageable pageable, String privilegeKey) {
        String permittedQuery = buildPermittedQuery(query, privilegeKey);

        BoolQueryBuilder typeKeyQuery = typeKeyQuery(targetEntityTypeKey);

        BoolQueryBuilder idNotIn = boolQuery().mustNot(termsQuery("id", ids));
        var esQuery = isEmpty(permittedQuery)
            ? idNotIn.must(typeKeyQuery)
            : idNotIn.must(simpleQueryStringQuery(permittedQuery)).must(typeKeyQuery);

        log.info("Executing DSL '{}'", esQuery);

        NativeSearchQuery queryBuilder = new NativeSearchQueryBuilder()
            .withQuery(esQuery)
            .withIndices(elasticsearchTemplateWrapper.getIndexName())
            .withTypes(ElasticsearchTemplateWrapper.INDEX_QUERY_TYPE)
            .withPageable(pageable == null ? DEFAULT_PAGE : pageable)
            .build();

        return elasticsearchTemplateWrapper.queryForPage(queryBuilder, XmEntity.class);
    }
}
