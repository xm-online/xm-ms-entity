package com.icthh.xm.ms.entity.repository;

import com.icthh.xm.ms.entity.domain.Location;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.*;


/**
 * Spring Data JPA repository for the Location entity.
 */
@SuppressWarnings("unused")
@Repository
public interface LocationRepository extends JpaRepository<Location,Long> {
    
}
