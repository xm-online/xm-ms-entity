package com.icthh.xm.ms.entity.repository.search.wrapper;

import com.icthh.xm.commons.search.builder.QueryBuilder;
import com.icthh.xm.commons.search.query.SearchQuery;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.repository.search.XmEntitySearchRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;

@Slf4j
@Service
@ConditionalOnProperty(name = "application.elastic-enabled", havingValue = "false")
public class NoOpXmEntitySearchRepository implements XmEntitySearchRepository {

    @Override
    public <S extends XmEntity> S index(S entity) {
        throw new UnsupportedOperationException("Elasticsearch is disabled");
    }

    @Override
    public Iterable<XmEntity> search(QueryBuilder query) {
        throw new UnsupportedOperationException("Elasticsearch is disabled");
    }

    @Override
    public Page<XmEntity> search(QueryBuilder query, Pageable pageable) {
        throw new UnsupportedOperationException("Elasticsearch is disabled");
    }

    @Override
    public Page<XmEntity> search(SearchQuery searchQuery) {
        throw new UnsupportedOperationException("Elasticsearch is disabled");
    }

    @Override
    public Page<XmEntity> searchSimilar(XmEntity entity, String[] fields, Pageable pageable) {
        throw new UnsupportedOperationException("Elasticsearch is disabled");
    }

    @Override
    public void refresh() {
        throw new UnsupportedOperationException("Elasticsearch is disabled");
    }

    @Override
    public Class<XmEntity> getEntityClass() {
        return XmEntity.class;
    }

    @Override
    public Iterable<XmEntity> findAll(Sort sort) {
        throw new UnsupportedOperationException("Elasticsearch is disabled");
    }

    @Override
    public Page<XmEntity> findAll(Pageable pageable) {
        throw new UnsupportedOperationException("Elasticsearch is disabled");
    }

    @Override
    public <S extends XmEntity> S save(S entity) {
        throw new UnsupportedOperationException("Elasticsearch is disabled");
    }

    @Override
    public <S extends XmEntity> Iterable<S> saveAll(Iterable<S> entities) {
        throw new UnsupportedOperationException("Elasticsearch is disabled");
    }

    @Override
    public Optional<XmEntity> findById(Long id) {
        throw new UnsupportedOperationException("Elasticsearch is disabled");
    }

    @Override
    public boolean existsById(Long id) {
        throw new UnsupportedOperationException("Elasticsearch is disabled");
    }

    @Override
    public Iterable<XmEntity> findAll() {
        throw new UnsupportedOperationException("Elasticsearch is disabled");
    }

    @Override
    public Iterable<XmEntity> findAllById(Iterable<Long> longs) {
        throw new UnsupportedOperationException("Elasticsearch is disabled");
    }

    @Override
    public long count() {
        throw new UnsupportedOperationException("Elasticsearch is disabled");
    }

    @Override
    public void deleteById(Long id) {
        throw new UnsupportedOperationException("Elasticsearch is disabled");
    }

    @Override
    public void delete(XmEntity entity) {
        throw new UnsupportedOperationException("Elasticsearch is disabled");
    }

    @Override
    public void deleteAll(Iterable<? extends XmEntity> entities) {
        throw new UnsupportedOperationException("Elasticsearch is disabled");
    }

    @Override
    public void deleteAll() {
        throw new UnsupportedOperationException("Elasticsearch is disabled");
    }
}
