package com.icthh.xm.ms.entity.service.query;

import com.icthh.xm.ms.entity.domain.Calendar_;
import com.icthh.xm.ms.entity.domain.Event;
import com.icthh.xm.ms.entity.domain.Event_;
import com.icthh.xm.ms.entity.repository.EventRepository;
import com.icthh.xm.ms.entity.service.query.filter.EventFilter;
import io.github.jhipster.service.QueryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for executing complex queries for {@link Event} entities in the database.
 * The main input is a {@link EventFilter} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link List} of {@link Event} or a {@link Page} of {@link Event} which fulfills the criteria.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventQueryService extends QueryService<Event> {

    private final EventRepository eventRepository;

    /**
     * Return a {@link List} of {@link Event} which matches the criteria from the database.
     * @param calendarId The calendar identifier which relates with events to search.
     * @param filer The object which holds all the filters, which the entities should match.
     * @param pageable the pagination information.
     * @return the matching entities.
     */
    public Page<Event> findAllByCalendarId(Long calendarId, EventFilter filer, Pageable pageable) {
        Specification<Event> specification = createSpecification(calendarId, filer);
        return eventRepository.findAll(specification, pageable);
    }

    /**
     * Return a {@link List} of {@link Event} which matches the criteria from the database.
     * @param filter The object which holds all the filters, which the entities should match.
     * @return the matching entities.
     */
    public List<Event> findAll(EventFilter filter) {
        Specification<Event> specification = createSpecification(filter);
        return eventRepository.findAll(specification);
    }

    private Specification<Event> createSpecification(Long calendarId, EventFilter filter) {
        Specification<Event> specification =
            (root, query, cb) -> cb.equal(root.get(Event_.calendar).get(Calendar_.id), calendarId);
        specification = getEventSpecification(filter, specification);

        return specification;
    }

    private Specification<Event> createSpecification(EventFilter filter) {
        //criteriaBuilder.conjunction() is used to avoid returning null from methods.
        //It generates always true Predicate
        Specification<Event> specification = (root, query, cb) -> cb.conjunction();
        specification = getEventSpecification(filter, specification);

        return specification;
    }

    private Specification<Event> getEventSpecification(EventFilter filter, Specification<Event> specification) {
        if (filter != null) {
            if (filter.getId() != null) {
                specification = specification.and(buildRangeSpecification(filter.getId(), Event_.id));
            }
            if (filter.getTypeKey() != null) {
                specification = specification.and(buildStringSpecification(filter.getTypeKey(), Event_.typeKey));
            }
            if (filter.getStartDate() != null) {
                specification = specification.and(buildRangeSpecification(filter.getStartDate(), Event_.startDate));
            }
            if (filter.getEndDate() != null) {
                specification = specification.and(buildRangeSpecification(filter.getEndDate(), Event_.endDate));
            }
        }
        return specification;
    }
}
