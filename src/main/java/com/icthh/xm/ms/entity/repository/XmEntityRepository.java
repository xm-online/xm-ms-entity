package com.icthh.xm.ms.entity.repository;

import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.projection.XmEntityStateProjection;
import com.icthh.xm.ms.entity.repository.entitygraph.EntityGraphRepository;
import com.icthh.xm.ms.entity.projection.XmEntityIdKeyTypeKey;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Set;


/**
 * Spring Data JPA repository for the XmEntity entity.
 */
@SuppressWarnings("unused")
@Repository
public interface XmEntityRepository extends JpaRepository<XmEntity, Long>, JpaSpecificationExecutor<XmEntity>, EntityGraphRepository<XmEntity, Long> {

    @EntityGraph(value = "xmEntityGraph", type = EntityGraph.EntityGraphType.LOAD)
    XmEntity findOneById(Long id);

    Page<XmEntity> findAllByTypeKeyIn(Pageable pageable, Set<String> typeKeys);

    XmEntityIdKeyTypeKey findOneIdKeyTypeKeyById(Long id);

    XmEntityIdKeyTypeKey findOneIdKeyTypeKeyByKey(String key);

    XmEntityStateProjection findStateProjectionById(Long id);

    XmEntityStateProjection findStateProjectionByKey(String key);

    // TODO Why we need this method? see org.springframework.data.repository.query.QueryByExampleExecutor.exists()
    boolean existsByTypeKeyAndNameIgnoreCase(String typeKey, String name);

}
