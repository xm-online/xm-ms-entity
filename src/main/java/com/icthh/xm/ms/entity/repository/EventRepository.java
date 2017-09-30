package com.icthh.xm.ms.entity.repository;

import com.icthh.xm.ms.entity.domain.Event;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.*;


/**
 * Spring Data JPA repository for the Event entity.
 */
@SuppressWarnings("unused")
@Repository
public interface EventRepository extends JpaRepository<Event,Long> {
    
}
