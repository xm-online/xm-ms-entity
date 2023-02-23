package com.icthh.xm.ms.entity.repository.search;

import com.icthh.xm.ms.entity.domain.XmEntity;
import org.elasticsearch.index.query.QueryBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.stereotype.Service;

import java.util.Optional;
@Service
public class XmEntitySearchRepositoryImpl implements XmEntitySearchRepository {

    @Override
    public <S extends XmEntity> S index(S entity) {
        return null;
    }

    @Override
    public Iterable<XmEntity> search(QueryBuilder query) {
        return null;
    }

    @Override
    public Page<XmEntity> search(QueryBuilder query, Pageable pageable) {
        return null;
    }

    @Override
    public Page<XmEntity> search(SearchQuery searchQuery) {
        return null;
    }

    @Override
    public Page<XmEntity> searchSimilar(XmEntity entity, String[] fields, Pageable pageable) {
        return null;
    }

    @Override
    public void refresh() {

    }

    @Override
    public Class<XmEntity> getEntityClass() {
        return null;
    }

    @Override
    public Iterable<XmEntity> findAll(Sort sort) {
        return null;
    }

    @Override
    public Page<XmEntity> findAll(Pageable pageable) {
        return null;
    }

    @Override
    public <S extends XmEntity> S save(S entity) {
        return null;
    }

    @Override
    public <S extends XmEntity> Iterable<S> saveAll(Iterable<S> entities) {
        return null;
    }

    @Override
    public Optional<XmEntity> findById(Long aLong) {
        return Optional.empty();
    }

    @Override
    public boolean existsById(Long aLong) {
        return false;
    }

    @Override
    public Iterable<XmEntity> findAll() {
        return null;
    }

    @Override
    public Iterable<XmEntity> findAllById(Iterable<Long> longs) {
        return null;
    }

    @Override
    public long count() {
        return 0;
    }

    @Override
    public void deleteById(Long aLong) {

    }

    @Override
    public void delete(XmEntity entity) {

    }

    @Override
    public void deleteAll(Iterable<? extends XmEntity> entities) {

    }

    @Override
    public void deleteAll() {

    }
}
