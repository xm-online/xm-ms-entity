package com.icthh.xm.ms.entity.repository;

import com.icthh.xm.commons.permission.access.repository.ResourceRepository;
import com.icthh.xm.ms.entity.domain.Link;
import java.util.List;

import com.icthh.xm.ms.entity.projection.LinkProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Link entity.
 */
@SuppressWarnings("unused")
@Repository
public interface LinkRepository extends JpaRepository<Link, Long>, JpaSpecificationExecutor<Link>, ResourceRepository {

    @Override
    Link findResourceById(Object id);

    @EntityGraph(attributePaths = {"target", "source"})
    List<Link> findBySourceIdAndTargetTypeKey(Long sourceId, String typeKey);

    @EntityGraph(attributePaths = {"target", "source"})
    List<Link> findByTargetIdAndTypeKey(Long targetId, String typeKey);

    @EntityGraph(attributePaths = {"target", "source"})
    List<Link> findBySourceTypeKeyAndTypeKeyIn(String sourceTypeKey, List<String> linksTypeKeys);

    @EntityGraph(attributePaths = {"target", "source"})
    List<Link> findBySourceIdAndTypeKey(Long id, String typeKey);

    List<LinkProjection> findLinkProjectionsBySourceIdAndTypeKey(Long id, String typeKey);

    @Override
    @EntityGraph(attributePaths = {"target", "source"})
    List<Link> findAll(Specification<Link> spec);

    @Override
    @EntityGraph(attributePaths = {"target", "source"})
    Page<Link> findAll(Specification<Link> spec, Pageable pageable);

}
