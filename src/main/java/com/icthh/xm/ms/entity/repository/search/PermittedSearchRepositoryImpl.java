package com.icthh.xm.ms.entity.repository.search;

import com.icthh.xm.commons.search.ElasticsearchOperations;
import com.icthh.xm.commons.search.dto.SearchDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Slf4j
@Repository("permittedSearchRepository")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "application.elastic-enabled", havingValue = "true", matchIfMissing = true)
public class PermittedSearchRepositoryImpl implements PermittedSearchRepository {

    private final ElasticsearchOperations elasticsearchOperations;

    /**
     * Search permitted entities.
     *
     * @param query        the elastic query
     * @param entityClass  the search entity class
     * @param privilegeKey the privilege key
     * @return permitted entities
     */
    public <T> List<T> search(String query, Class<T> entityClass, String privilegeKey) {
        return elasticsearchOperations.search(query, entityClass, privilegeKey);
    }

    /**
     * Search permitted entities.
     *
     * @param query       the elastic query
     * @param pageable    the page info
     * @param entityClass the search entity class
     * @return permitted entities
     * @deprecated use {@link #searchForPage(SearchDto, String)} instead
     */
    @Deprecated
    public <T> Page<T> search(String query, Pageable pageable, Class<T> entityClass, String privilegeKey) {
        return searchForPage(SearchDto.builder()
            .entityClass(entityClass)
            .pageable(pageable)
            .query(query)
            .build(), privilegeKey);
    }

    /**
     * Search permitted entities with scroll
     *
     * @param scrollTimeInMillis The time in millisecond for scroll feature
     * @param query              the elastic query
     * @param pageable           the page info
     * @param entityClass        the search entity class
     * @param privilegeKey       the privilege key
     * @return permitted entities
     */
    public <T> Page<T> search(Long scrollTimeInMillis,
                              String query,
                              Pageable pageable,
                              Class<T> entityClass,
                              String privilegeKey) {
        return elasticsearchOperations.search(scrollTimeInMillis, query, pageable, entityClass, privilegeKey);
    }

    // do not renamed! called from lep for not simple string query
    public ElasticsearchOperations getElasticsearchTemplate() {
        return elasticsearchOperations;
    }

    public <T> Page<T> searchForPage(SearchDto searchDto, String privilegeKey) {
        return elasticsearchOperations.searchForPage(searchDto, privilegeKey);
    }
}
