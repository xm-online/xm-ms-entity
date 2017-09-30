package com.icthh.xm.ms.entity.repository;

import com.icthh.xm.ms.entity.domain.Calendar;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.*;


/**
 * Spring Data JPA repository for the Calendar entity.
 */
@SuppressWarnings("unused")
@Repository
public interface CalendarRepository extends JpaRepository<Calendar,Long> {
    
}
