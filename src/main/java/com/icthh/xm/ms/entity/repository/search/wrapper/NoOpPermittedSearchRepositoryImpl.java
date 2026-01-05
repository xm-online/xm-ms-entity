package com.icthh.xm.ms.entity.repository.search.wrapper;

import com.icthh.xm.commons.search.dto.SearchDto;
import com.icthh.xm.ms.entity.repository.search.IPermittedSearchRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

@Slf4j
@Repository("permittedSearchRepository")
@ConditionalOnProperty(name = "application.elastic-enabled", havingValue = "false")
public class NoOpPermittedSearchRepositoryImpl implements IPermittedSearchRepository {

    /**
     * Search permitted entities (stub implementation).
     */
    public <T> List<T> search(String query, Class<T> entityClass, String privilegeKey) {
        log.error("Elasticsearch is disabled. Returning empty list for search query: {}", query);
        return Collections.emptyList();
    }

    /**
     * Search permitted entities (stub implementation).
     */
    public <T> Page<T> search(String query, Pageable pageable, Class<T> entityClass, String privilegeKey) {
        log.error("Elasticsearch is disabled. Returning empty page for search query: {}", query);
        return new PageImpl<>(Collections.emptyList(), pageable, 0);
    }

    /**
     * Search permitted entities with scroll (stub implementation).
     */
    public <T> Page<T> search(Long scrollTimeInMillis,
                              String query,
                              Pageable pageable,
                              Class<T> entityClass,
                              String privilegeKey) {
        log.error("Elasticsearch is disabled. Returning empty page for scroll search query: {}", query);
        return new PageImpl<>(Collections.emptyList(), pageable, 0);
    }

    public <T> Page<T> searchForPage(SearchDto searchDto, String privilegeKey) {
        log.error("Elasticsearch is disabled. Returning empty page for searchDto: {}", searchDto);
        return new PageImpl<>(Collections.emptyList(), searchDto.getPageable(), 0);
    }
}
