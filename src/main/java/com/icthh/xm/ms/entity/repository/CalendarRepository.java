package com.icthh.xm.ms.entity.repository;

// TODO: 14-Jan-19 Use commons ResourceRepository after fix
//import com.icthh.xm.commons.permission.access.repository.ResourceRepository;
import com.icthh.xm.ms.entity.domain.Calendar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


/**
 * Spring Data JPA repository for the Calendar entity.
 */
@SuppressWarnings("unused")
@Repository
public interface CalendarRepository extends JpaRepository<Calendar,Long>, ResourceRepository {

    @Override
    Calendar findResourceById(Object id);
}
