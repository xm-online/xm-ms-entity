package com.icthh.xm.ms.entity.repository.search.wrapper;

import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.repository.search.IXmEntityPermittedSearchRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.Set;

@Slf4j
@Repository
@ConditionalOnProperty(name = "application.elastic-enabled", havingValue = "false")
public class NoOpXmEntityPermittedSearchRepository extends NoOpPermittedSearchRepository implements IXmEntityPermittedSearchRepository {

    /**
     * Search for XmEntity by type key and query (stub implementation).
     */
    public Page<XmEntity> searchByQueryAndTypeKey(String query,
                                                  String typeKey,
                                                  Pageable pageable,
                                                  String privilegeKey) {
        log.error("Elasticsearch is disabled. Returning empty page for searchByQueryAndTypeKey. TypeKey: {}", typeKey);
        return new PageImpl<>(Collections.emptyList(), pageable, 0);
    }

    /**
     * Search with ID exclusion (stub implementation).
     */
    public Page<XmEntity> searchWithIdNotIn(String query, Set<Long> ids,
                                            String targetEntityTypeKey,
                                            Pageable pageable, String privilegeKey) {
        log.error("Elasticsearch is disabled. Returning empty page for searchWithIdNotIn. TypeKey: {}", targetEntityTypeKey);
        return new PageImpl<>(Collections.emptyList(), pageable, 0);
    }
}
