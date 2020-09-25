package com.icthh.xm.ms.entity.repository;

import com.icthh.xm.commons.permission.access.repository.ResourceRepository;
import com.icthh.xm.ms.entity.domain.Event;
import com.icthh.xm.ms.entity.domain.XmEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Event entity.
 */
@SuppressWarnings("unused")
@Repository
public interface EventRepository extends JpaRepository<Event, Long>, ResourceRepository {

    @Override
    Event findResourceById(Object id);

    Optional<Event> findByEventDataRef(XmEntity eventDataRef);
}
