package com.icthh.xm.ms.entity.repository;

import com.icthh.xm.commons.permission.access.repository.ResourceRepository;
import com.icthh.xm.ms.entity.domain.Event;
import com.icthh.xm.ms.entity.domain.XmEntity;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Event entity.
 */
@Repository
public interface EventRepository extends JpaRepository<Event, Long>, ResourceRepository,
    JpaSpecificationExecutor<Event> {

    @Override
    Event findResourceById(Object id);

    Optional<Event> findByEventDataRef(XmEntity eventDataRef);

    @Override
    @EntityGraph(attributePaths = {"calendar", "assigned", "eventDataRef"})
    Page<Event> findAll(Specification<Event> spec, Pageable pageable);
}
