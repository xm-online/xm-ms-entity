package com.icthh.xm.ms.entity.repository.search;

import com.icthh.xm.ms.entity.domain.XmEntity;
import org.elasticsearch.index.query.QueryBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.query.SearchQuery;

import java.util.Optional;

/**
 * Spring Data Elasticsearch repository for the XmEntity entity.
 *
 * @Deprecation - repository methods should be rewritten to avoid usage of org.springframework.data.elasticsearch
 */
public interface XmEntitySearchRepository {

    public <S extends XmEntity> S index(S entity) ;


    public Iterable<XmEntity> search(QueryBuilder query) ;


    public Page<XmEntity> search(QueryBuilder query, Pageable pageable) ;


    public Page<XmEntity> search(SearchQuery searchQuery) ;


    public Page<XmEntity> searchSimilar(XmEntity entity, String[] fields, Pageable pageable) ;


    public void refresh() ;


    public Class<XmEntity> getEntityClass() ;


    public Iterable<XmEntity> findAll(Sort sort) ;


    public Page<XmEntity> findAll(Pageable pageable) ;


    public <S extends XmEntity> S save(S entity) ;


    public <S extends XmEntity> Iterable<S> saveAll(Iterable<S> entities);


    public Optional<XmEntity> findById(Long aLong) ;


    public boolean existsById(Long aLong) ;


    public Iterable<XmEntity> findAll() ;


    public Iterable<XmEntity> findAllById(Iterable<Long> longs) ;


    public long count() ;


    public void deleteById(Long aLong) ;


    public void delete(XmEntity entity) ;

    public void deleteAll(Iterable<? extends XmEntity> entities);

    public void deleteAll();
}
