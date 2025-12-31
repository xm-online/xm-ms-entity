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
        log.debug("Elasticsearch is disabled. Skipping index for entity: {}", entity.getId());
        return entity;
    }

    @Override
    public Iterable<XmEntity> search(QueryBuilder query) {
        log.warn("Elasticsearch is disabled. Returning empty list for search");
        return Collections.emptyList();
    }

    @Override
    public Page<XmEntity> search(QueryBuilder query, Pageable pageable) {
        log.warn("Elasticsearch is disabled. Returning empty page for search");
        return new PageImpl<>(Collections.emptyList(), pageable, 0);
    }

    @Override
    public Page<XmEntity> search(SearchQuery searchQuery) {
        log.warn("Elasticsearch is disabled. Returning empty page for search");
        return new PageImpl<>(Collections.emptyList());
    }

    @Override
    public Page<XmEntity> searchSimilar(XmEntity entity, String[] fields, Pageable pageable) {
        log.warn("Elasticsearch is disabled. Returning empty page for searchSimilar");
        return new PageImpl<>(Collections.emptyList(), pageable, 0);
    }

    @Override
    public void refresh() {
        log.debug("Elasticsearch is disabled. Skipping refresh");
    }

    @Override
    public Class<XmEntity> getEntityClass() {
        return XmEntity.class;
    }

    @Override
    public Iterable<XmEntity> findAll(Sort sort) {
        log.warn("Elasticsearch is disabled. Returning empty list for findAll");
        return Collections.emptyList();
    }

    @Override
    public Page<XmEntity> findAll(Pageable pageable) {
        log.warn("Elasticsearch is disabled. Returning empty page for findAll");
        return new PageImpl<>(Collections.emptyList(), pageable, 0);
    }

    @Override
    public <S extends XmEntity> S save(S entity) {
        log.debug("Elasticsearch is disabled. Skipping save for entity: {}", entity != null ? entity.getId() : null);
        return entity;
    }

    @Override
    public <S extends XmEntity> Iterable<S> saveAll(Iterable<S> entities) {
        log.debug("Elasticsearch is disabled. Skipping saveAll");
        return entities;
    }

    @Override
    public Optional<XmEntity> findById(Long id) {
        log.debug("Elasticsearch is disabled. Returning empty Optional for findById: {}", id);
        return Optional.empty();
    }

    @Override
    public boolean existsById(Long id) {
        log.debug("Elasticsearch is disabled. Returning false for existsById: {}", id);
        return false;
    }

    @Override
    public Iterable<XmEntity> findAll() {
        log.warn("Elasticsearch is disabled. Returning empty list for findAll");
        return Collections.emptyList();
    }

    @Override
    public Iterable<XmEntity> findAllById(Iterable<Long> longs) {
        log.warn("Elasticsearch is disabled. Returning empty list for findAllById");
        return Collections.emptyList();
    }

    @Override
    public long count() {
        log.debug("Elasticsearch is disabled. Returning 0 for count");
        return 0;
    }

    @Override
    public void deleteById(Long id) {
        log.debug("Elasticsearch is disabled. Skipping deleteById: {}", id);
    }

    @Override
    public void delete(XmEntity entity) {
        log.debug("Elasticsearch is disabled. Skipping delete for entity: {}", entity != null ? entity.getId() : null);
    }

    @Override
    public void deleteAll(Iterable<? extends XmEntity> entities) {
        log.debug("Elasticsearch is disabled. Skipping deleteAll");
    }

    @Override
    public void deleteAll() {
        log.debug("Elasticsearch is disabled. Skipping deleteAll");
    }
}
