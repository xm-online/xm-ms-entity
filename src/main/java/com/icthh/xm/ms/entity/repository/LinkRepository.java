package com.icthh.xm.ms.entity.repository;

// TODO: 14-Jan-19 Use commons ResourceRepository after fix
//import com.icthh.xm.commons.permission.access.repository.ResourceRepository;
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
}
