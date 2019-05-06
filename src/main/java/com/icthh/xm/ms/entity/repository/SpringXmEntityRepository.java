package com.icthh.xm.ms.entity.repository;

import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.projection.XmEntityIdKeyTypeKey;
import com.icthh.xm.ms.entity.projection.XmEntityStateProjection;
import com.icthh.xm.ms.entity.projection.XmEntityVersion;
import com.icthh.xm.ms.entity.repository.entitygraph.EntityGraphRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the XmEntity entity.
 */
@SuppressWarnings("unused")
@Repository
public interface SpringXmEntityRepository extends
    JpaRepository<XmEntity, Long>, JpaSpecificationExecutor<XmEntity> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM XmEntity e WHERE e.id = :id")
    XmEntity findOneByIdForUpdate(@Param("id") Long id);

    @EntityGraph(value = "xmEntityGraph", type = EntityGraph.EntityGraphType.LOAD)
    XmEntity findOneById(Long id);

    Page<XmEntity> findAllByTypeKeyIn(Pageable pageable, Set<String> typeKeys);

    XmEntityIdKeyTypeKey findOneIdKeyTypeKeyById(Long id);

    XmEntityIdKeyTypeKey findOneIdKeyTypeKeyByKey(String key);

    XmEntityStateProjection findStateProjectionById(Long id);

    XmEntityStateProjection findStateProjectionByKey(String key);

    Optional<XmEntityVersion> findVersionById(Long id);

    // TODO Why we need this method? see org.springframework.data.repository.query.QueryByExampleExecutor.exists()
    boolean existsByTypeKeyAndNameIgnoreCase(String typeKey, String name);

    @Lock(LockModeType.WRITE)
    @Override
    <S extends XmEntity> List<S> saveAll(Iterable<S> entities);

    @Lock(LockModeType.WRITE)
    @Override
    <S extends XmEntity> S saveAndFlush(S entity);

    @Lock(LockModeType.WRITE)
    @Override
    <S extends XmEntity> S save(S entity);

}
