package com.icthh.xm.ms.entity.repository;

import com.icthh.xm.commons.permission.access.repository.ResourceRepository;
import com.icthh.xm.ms.entity.domain.Location;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Location entity.
 */
@SuppressWarnings("unused")
@Repository
public interface LocationRepository extends JpaRepository<Location, Long>, ResourceRepository<Location, Long> {

    @Override
    Location findResourceById(Long id);

    List<Location> findAllByXmEntityTypeKeyAndTypeKeyIn(String entityTypeKey, List<String> typeKeys);

    List<Location> findAllByXmEntityIdIn(List<Long> xmEntityIds);

    List<Location> findAllByIdIn(List<Long> locationIds);
}
