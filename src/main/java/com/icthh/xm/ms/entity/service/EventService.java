package com.icthh.xm.ms.entity.service;

import static java.util.Optional.ofNullable;

import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.commons.permission.annotation.FindWithPermission;
import com.icthh.xm.commons.permission.annotation.PrivilegeDescription;
import com.icthh.xm.commons.permission.repository.PermittedRepository;
import com.icthh.xm.ms.entity.domain.Event;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.lep.keyresolver.EventTypeKeyResolver;
import com.icthh.xm.ms.entity.repository.EventRepository;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
import com.icthh.xm.ms.entity.repository.search.PermittedSearchRepository;
import com.icthh.xm.ms.entity.service.query.EventQueryService;
import com.icthh.xm.ms.entity.service.query.filter.EventFilter;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing Event.
 */
@Service
@Transactional
@RequiredArgsConstructor
@LepService(group = "service.event")
public class EventService {

    private final XmEntityService xmEntityService;
    private final EventQueryService eventQueryService;
    private final EventRepository eventRepository;
    private final PermittedRepository permittedRepository;
    private final PermittedSearchRepository permittedSearchRepository;
    private final XmEntityRepository xmEntityRepository;
    private final CalendarService calendarService;

    @Setter(onMethod_ = {@Autowired})
    private EventService self;

    /**
     * Save an event.
     *
     * <p>NOTE: Method triggers LEP method which resolved by {@link Event#getTypeKey()} value.
     * @param event the entity to save
     * @return the persisted entity
     */
    @LogicExtensionPoint(value = "Save", resolver = EventTypeKeyResolver.class)
    public Event save(Event event) {
        return self.saveEvent(event);
    }

    /**
     * Save an event.
     *
     * @param event the entity to save
     * @return the persisted entity
     */
    @LogicExtensionPoint(value = "Save")
    public Event saveEvent(Event event) {
        calendarService.assertReadOnlyCalendar(event.getCalendar());
        if (event.getId() != null) {
            Optional<Event> oldEvent = eventRepository.findById(event.getId());
            oldEvent.map(Event::getCalendar).ifPresent(calendarService::assertReadOnlyCalendar);
        }

        ofNullable(event.getAssigned())
            .map(XmEntity::getId)
            .ifPresent(assignedId -> event.setAssigned(xmEntityRepository.getOne(assignedId)));
        XmEntity eventDataRef = ofNullable(event.getEventDataRef())
            .map(xmEntityService::save)
            .orElse(null);
        event.setEventDataRef(eventDataRef);
        return eventRepository.save(event);
    }

    /**
     *  Get all the events.
     *
     *  @return the list of entities
     */
    @Transactional(readOnly = true)
    @FindWithPermission("EVENT.GET_LIST")
    @LogicExtensionPoint("FindAll")
    @PrivilegeDescription("Privilege to get all the events")
    public List<Event> findAll(String privilegeKey) {
        return permittedRepository.findAll(Event.class, privilegeKey);
    }

    /**
     * Get all the events by filter.
     * Method is used in LEP's
     *
     * @return the list of entities
     */
    @Transactional(readOnly = true)
    @SuppressWarnings("unused")
    @LogicExtensionPoint("FindAllByFilter")
    public List<Event> findAllByFilter(EventFilter filter) {
        return eventQueryService.findAll(filter);
    }

    @Transactional(readOnly = true)
    public Page<Event> findAll(Specification<Event> spec, Pageable pageable) {
        return eventRepository.findAll(spec, pageable);
    }

    /**
     *  Get one event by id.
     *
     *  @param id the id of the entity
     *  @return the entity
     */
    @Transactional(readOnly = true)
    @LogicExtensionPoint("FindOne")
    public Event findOne(Long id) {
        return eventRepository.findById(id).orElse(null);
    }

    /**
     *  Delete the  event by id.
     *
     *  @param id the id of the entity
     */
    @LogicExtensionPoint("Delete")
    public void delete(Long id) {
        eventRepository.deleteById(id);
    }

    @LogicExtensionPoint("DeleteAll")
    public void deleteAll(List<Event> events) {
        eventRepository.deleteAll(events);
    }

    /**
     * Search for the event corresponding to the query.
     *
     *  @param query the query of the search
     *  @return the list of entities
     */
    @Deprecated
    @Transactional(readOnly = true)
    @FindWithPermission("EVENT.SEARCH")
    @LogicExtensionPoint("Search")
    @PrivilegeDescription("Privilege to search for the event corresponding to the query")
    public List<Event> search(String query, String privilegeKey) {
        return permittedSearchRepository.search(query, Event.class, privilegeKey);
    }
}
