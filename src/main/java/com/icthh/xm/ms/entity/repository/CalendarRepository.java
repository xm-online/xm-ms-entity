package com.icthh.xm.ms.entity.repository;

import com.icthh.xm.commons.permission.access.repository.ResourceRepository;
import com.icthh.xm.ms.entity.domain.Calendar;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Calendar entity.
 */
@SuppressWarnings("unused")
@Repository
public interface CalendarRepository extends JpaRepository<Calendar, Long>, ResourceRepository<Calendar, Long> {

    @Override
    Calendar findResourceById(Long id);

    Set<Calendar> findByXmEntityTypeKeyAndTypeKeyAndEventsTypeKeyIn(String entityType, String typeKey, List<String> eventTypeKeys);
}
