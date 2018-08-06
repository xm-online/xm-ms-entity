package com.icthh.xm.ms.entity.repository.search;

import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;
import static org.elasticsearch.index.query.QueryBuilders.prefixQuery;
import static org.elasticsearch.index.query.QueryBuilders.simpleQueryStringQuery;

import com.icthh.xm.commons.permission.service.PermissionCheckService;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.repository.search.translator.SpelToElasticTranslator;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
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

    public XmEntityPermittedSearchRepository(PermissionCheckService permissionCheckService,
                                             SpelToElasticTranslator spelToElasticTranslator,
                                             ElasticsearchTemplate elasticsearchTemplate) {
        super(permissionCheckService, spelToElasticTranslator, elasticsearchTemplate);
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

        val typeKeyQuery = boolQuery()
            .should(matchQuery(TYPE_KEY, typeKey))
            .minimumNumberShouldMatch(1);

        val esQuery = isEmpty(permittedQuery)
            ? boolQuery().must(typeKeyQuery)
            : typeKeyQuery.must(simpleQueryStringQuery(permittedQuery));

        log.debug("Executing DSL '{}'", esQuery);

        NativeSearchQuery queryBuilder = new NativeSearchQueryBuilder()
            .withQuery(esQuery)
            .withPageable(pageable)
            .build();

        return getElasticsearchTemplate().queryForPage(queryBuilder, XmEntity.class);
    }
}
