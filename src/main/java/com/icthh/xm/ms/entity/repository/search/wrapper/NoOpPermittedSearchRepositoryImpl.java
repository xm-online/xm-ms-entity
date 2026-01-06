package com.icthh.xm.ms.entity.repository.search.wrapper;

import com.icthh.xm.commons.search.dto.SearchDto;
import com.icthh.xm.ms.entity.repository.search.PermittedSearchRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Slf4j
@Repository("permittedSearchRepository")
@ConditionalOnProperty(name = "application.elastic-enabled", havingValue = "false")
public class NoOpPermittedSearchRepositoryImpl implements PermittedSearchRepository {

    /**
     * Search permitted entities (stub implementation).
     */
    public <T> List<T> search(String query, Class<T> entityClass, String privilegeKey) {
        throw new UnsupportedOperationException("Elasticsearch is disabled");
    }

    /**
     * Search permitted entities (stub implementation).
     */
    public <T> Page<T> search(String query, Pageable pageable, Class<T> entityClass, String privilegeKey) {
        throw new UnsupportedOperationException("Elasticsearch is disabled");
    }

    /**
     * Search permitted entities with scroll (stub implementation).
     */
    public <T> Page<T> search(Long scrollTimeInMillis,
                              String query,
                              Pageable pageable,
                              Class<T> entityClass,
                              String privilegeKey) {
        throw new UnsupportedOperationException("Elasticsearch is disabled");
    }

    public <T> Page<T> searchForPage(SearchDto searchDto, String privilegeKey) {
        throw new UnsupportedOperationException("Elasticsearch is disabled");
    }
}
