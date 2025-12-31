package com.icthh.xm.ms.entity.repository.search;

import com.icthh.xm.commons.search.ElasticsearchOperations;
import com.icthh.xm.ms.entity.domain.XmEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Slf4j
@Repository
@ConditionalOnProperty(name = "application.elastic-enabled", havingValue = "true", matchIfMissing = true)
public class XmEntityPermittedSearchRepository extends PermittedSearchRepository implements IXmEntityPermittedSearchRepository {

    private final ElasticsearchOperations elasticsearchOperations;

    public XmEntityPermittedSearchRepository(ElasticsearchOperations elasticsearchOperations) {
        super(elasticsearchOperations);
        this.elasticsearchOperations = elasticsearchOperations;
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
        return elasticsearchOperations.searchByQueryAndTypeKey(query, typeKey, pageable, XmEntity.class, privilegeKey);
    }

    public Page<XmEntity> searchWithIdNotIn(String query, Set<Long> ids,
                                            String targetEntityTypeKey,
                                            Pageable pageable, String privilegeKey) {
        return elasticsearchOperations.searchWithIdNotIn(query, ids, targetEntityTypeKey, pageable, XmEntity.class, privilegeKey);
    }
}
