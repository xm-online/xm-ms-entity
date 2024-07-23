package com.icthh.xm.ms.entity.repository.search;

import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.service.search.builder.QueryBuilder;
import com.icthh.xm.ms.entity.service.search.query.SearchQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Optional;

/**
 * Spring Data Elasticsearch repository for the XmEntity entity.
 *
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
