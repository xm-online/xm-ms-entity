package com.icthh.xm.ms.entity.repository.search;

import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;
import static org.elasticsearch.index.query.QueryBuilders.prefixQuery;
import static org.elasticsearch.index.query.QueryBuilders.simpleQueryStringQuery;
import static org.elasticsearch.index.query.QueryBuilders.termsQuery;
import static org.springframework.data.elasticsearch.core.query.Query.DEFAULT_PAGE;

import com.icthh.xm.commons.permission.service.PermissionCheckService;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.repository.search.elasticsearch.index.ElasticIndexNameResolver;
import com.icthh.xm.ms.entity.repository.search.translator.SpelToElasticTranslator;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
public class XmEntityPermittedSearchRepository extends PermittedSearchRepository {

    private static final String TYPE_KEY = "typeKey";
    private final ElasticIndexNameResolver elasticIndexNameResolver;


    public XmEntityPermittedSearchRepository(PermissionCheckService permissionCheckService,
                                             SpelToElasticTranslator spelToElasticTranslator,
                                             ElasticsearchTemplate elasticsearchTemplate,
                                             ElasticIndexNameResolver elasticIndexNameResolver) {
        super(permissionCheckService, spelToElasticTranslator, elasticsearchTemplate);
        this.elasticIndexNameResolver = elasticIndexNameResolver;
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

        var esQuery = isEmpty(permittedQuery)
            ? boolQuery().must(typeKeyQuery)
            : typeKeyQuery.must(simpleQueryStringQuery(permittedQuery));

        log.debug("Executing DSL '{}'", esQuery);

        NativeSearchQuery queryBuilder = new NativeSearchQueryBuilder()
            .withQuery(esQuery)
            .withIndices(elasticIndexNameResolver.resolve(typeKey))
            .withPageable(pageable == null ? DEFAULT_PAGE : pageable)
            .build();

        return getElasticsearchTemplate().queryForPage(queryBuilder, XmEntity.class);
    }

    private BoolQueryBuilder typeKeyQuery(String typeKey) {
        var prefix = typeKey + ".";
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
            .withIndices(elasticIndexNameResolver.resolve(targetEntityTypeKey))
            .withPageable(pageable == null ? DEFAULT_PAGE : pageable)
            .build();

        return getElasticsearchTemplate().queryForPage(queryBuilder, XmEntity.class);
    }
}
