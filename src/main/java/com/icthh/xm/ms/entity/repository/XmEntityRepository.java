package com.icthh.xm.ms.entity.repository;

import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.repository.entitygraph.EntityGraphRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Set;


/**
 * Spring Data JPA repository for the XmEntity entity.
 */
@SuppressWarnings("unused")
@Repository
public interface XmEntityRepository extends JpaRepository<XmEntity, Long>, EntityGraphRepository<XmEntity, Long> {

    @EntityGraph(value = "xmEntityGraph", type = EntityGraph.EntityGraphType.LOAD)
    XmEntity findOneById(Long id);

    Page<XmEntity> findAllByTypeKeyIn(Pageable pageable, Set<String> typeKeys);

}
