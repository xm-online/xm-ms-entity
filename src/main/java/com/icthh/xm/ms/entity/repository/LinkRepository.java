package com.icthh.xm.ms.entity.repository;

import com.icthh.xm.commons.permission.access.repository.ResourceRepository;
import com.icthh.xm.ms.entity.domain.Link;

import java.util.List;

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

    List<Link> findBySourceIdAndTargetTypeKey(Long sourceId, String typeKey);

    List<Link> findByTargetIdAndTypeKey(Long targetId, String typeKey);

    Integer countBySourceIdAndTypeKeyAndIdNot(Long sourceId, String typeKey, Long notId);

    Integer countBySourceIdAndTypeKeyAndIdNotIn(Long sourceId, String typeKey, List<Long> notId);

    Integer countBySourceIdAndTypeKey(Long sourceId, String typeKey);

}
