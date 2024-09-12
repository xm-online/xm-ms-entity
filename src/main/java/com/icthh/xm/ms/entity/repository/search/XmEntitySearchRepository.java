package com.icthh.xm.ms.entity.repository.search;

import com.icthh.xm.commons.search.builder.QueryBuilder;
import com.icthh.xm.commons.search.query.SearchQuery;
import com.icthh.xm.ms.entity.domain.XmEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Optional;

/**
 * Spring Data Elasticsearch repository for the XmEntity entity.
 */
public interface XmEntitySearchRepository {

    <S extends XmEntity> S index(S entity);

    Iterable<XmEntity> search(QueryBuilder query);

    Page<XmEntity> search(QueryBuilder query, Pageable pageable);

    Page<XmEntity> search(SearchQuery searchQuery);


    Page<XmEntity> searchSimilar(XmEntity entity, String[] fields, Pageable pageable);

    void refresh();

    Class<XmEntity> getEntityClass();

    Iterable<XmEntity> findAll(Sort sort);

    Page<XmEntity> findAll(Pageable pageable);

    <S extends XmEntity> S save(S entity);

    <S extends XmEntity> Iterable<S> saveAll(Iterable<S> entities);

    Optional<XmEntity> findById(Long aLong);

    boolean existsById(Long aLong);

    Iterable<XmEntity> findAll();

    Iterable<XmEntity> findAllById(Iterable<Long> longs);

    long count();

    void deleteById(Long aLong);

    void delete(XmEntity entity);

    void deleteAll(Iterable<? extends XmEntity> entities);

    void deleteAll();
}
