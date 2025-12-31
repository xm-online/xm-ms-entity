package com.icthh.xm.ms.entity.repository.search;

import com.icthh.xm.ms.entity.domain.XmEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Set;

public interface IXmEntityPermittedSearchRepository extends IPermittedSearchRepository {

    /**
     * Search for XmEntity by type key and query.
     *
     * @param query        the query
     * @param typeKey      the type key
     * @param pageable     the page info
     * @param privilegeKey the privilege key
     * @return permitted entities
     */
    Page<XmEntity> searchByQueryAndTypeKey(String query,
                                          String typeKey,
                                          Pageable pageable,
                                          String privilegeKey);

    /**
     * Search with ID exclusion.
     *
     * @param query                the query
     * @param ids                  the IDs to exclude
     * @param targetEntityTypeKey  the target entity type key
     * @param pageable             the page info
     * @param privilegeKey         the privilege key
     * @return permitted entities
     */
    Page<XmEntity> searchWithIdNotIn(String query,
                                     Set<Long> ids,
                                     String targetEntityTypeKey,
                                     Pageable pageable,
                                     String privilegeKey);
}
