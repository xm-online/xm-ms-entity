package com.icthh.xm.ms.entity.repository.search;

import com.icthh.xm.ms.entity.service.dto.SearchDto;
import com.icthh.xm.ms.entity.service.search.ElasticsearchTemplateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class PermittedSearchRepository {

    private final ElasticsearchTemplateWrapper elasticsearchTemplateWrapper;

    /**
     * Search permitted entities.
     *
     * @param query        the elastic query
     * @param entityClass  the search entity class
     * @param privilegeKey the privilege key
     * @return permitted entities
     */
    public <T> List<T> search(String query, Class<T> entityClass, String privilegeKey) {
        return elasticsearchTemplateWrapper.search(query, entityClass, privilegeKey);
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
        return elasticsearchTemplateWrapper.search(scrollTimeInMillis, query, pageable, entityClass, privilegeKey);
    }

    // do not renamed! called from lep for not simple string query
    public ElasticsearchTemplateWrapper getElasticsearchTemplate() {
        return elasticsearchTemplateWrapper;
    }

    public <T> Page<T> searchForPage(SearchDto searchDto, String privilegeKey) {
        return elasticsearchTemplateWrapper.searchForPage(searchDto, privilegeKey);
    }
}
