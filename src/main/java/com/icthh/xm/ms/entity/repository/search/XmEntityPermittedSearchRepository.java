package com.icthh.xm.ms.entity.repository.search;

import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.service.search.ElasticsearchTemplateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Slf4j
@Repository
public class XmEntityPermittedSearchRepository extends PermittedSearchRepository {

    private final ElasticsearchTemplateWrapper elasticsearchTemplateWrapper;

    public XmEntityPermittedSearchRepository(ElasticsearchTemplateWrapper elasticsearchTemplateWrapper) {
        super(elasticsearchTemplateWrapper);
        this.elasticsearchTemplateWrapper = elasticsearchTemplateWrapper;
    }

    /**
     * Search for XmEntity by type key and query.
     *
     * @param query        the query
     * @param typeKey      the type key
     * @param pageable     the page info
     * @param privilegeKey the privilege key
     * @return permitted entities
     */
    public Page<XmEntity> searchByQueryAndTypeKey(String query,
                                                  String typeKey,
                                                  Pageable pageable,
                                                  String privilegeKey) {
        return elasticsearchTemplateWrapper.searchByQueryAndTypeKey(query, typeKey, pageable, privilegeKey);
    }

    public Page<XmEntity> searchWithIdNotIn(String query, Set<Long> ids,
                                            String targetEntityTypeKey,
                                            Pageable pageable, String privilegeKey) {
        return elasticsearchTemplateWrapper.searchWithIdNotIn(query, ids, targetEntityTypeKey, pageable, privilegeKey);
    }
}
