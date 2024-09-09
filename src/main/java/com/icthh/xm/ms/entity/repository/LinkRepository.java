package com.icthh.xm.ms.entity.repository;

import com.icthh.xm.commons.permission.access.repository.ResourceRepository;
import com.icthh.xm.ms.entity.domain.Link;
import java.util.List;

import com.icthh.xm.ms.entity.projection.LinkProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Link entity.
 */
@SuppressWarnings("unused")
@Repository
public interface LinkRepository extends JpaRepository<Link, Long>, JpaSpecificationExecutor<Link>,
    ResourceRepository<Link, Long> {

    @Override
    Link findResourceById(Long id);

    List<Link> findBySourceIdAndTargetTypeKey(Long sourceId, String typeKey);

    List<Link> findByTargetIdAndTypeKey(Long targetId, String typeKey);

    List<Link> findBySourceTypeKeyAndTypeKeyIn(String sourceTypeKey, List<String> linksTypeKeys);

    List<Link> findBySourceIdAndTypeKey(Long id, String typeKey);

    List<LinkProjection> findLinkProjectionsBySourceIdAndTypeKey(Long id, String typeKey);

}
