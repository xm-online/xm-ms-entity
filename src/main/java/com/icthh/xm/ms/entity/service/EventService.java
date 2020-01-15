package com.icthh.xm.ms.entity.service;

import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.commons.permission.annotation.FindWithPermission;
import com.icthh.xm.commons.permission.annotation.PrivilegeDescription;
import com.icthh.xm.commons.permission.repository.PermittedRepository;
import com.icthh.xm.ms.entity.domain.Event;
import com.icthh.xm.ms.entity.repository.EventRepository;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
import com.icthh.xm.ms.entity.repository.search.PermittedSearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service Implementation for managing Event.
 */
@Service
@Transactional
@RequiredArgsConstructor
@LepService(group = "service.event")
public class EventService {

    private final EventRepository eventRepository;

    private final PermittedRepository permittedRepository;

    private final PermittedSearchRepository permittedSearchRepository;

    private final XmEntityRepository xmEntityRepository;

    /**
     * Save a event.
     *
     * @param event the entity to save
     * @return the persisted entity
     */
    @LogicExtensionPoint("Save")
    public Event save(Event event) {
        if (event.getAssigned() != null) {
            event.setAssigned(xmEntityRepository.getOne(event.getAssigned().getId()));
        }
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

    /**
     * Search for the event corresponding to the query.
     *
     *  @param query the query of the search
     *  @return the list of entities
     */
    @Transactional(readOnly = true)
    @FindWithPermission("EVENT.SEARCH")
    @LogicExtensionPoint("Search")
    @PrivilegeDescription("Privilege to search for the event corresponding to the query")
    public List<Event> search(String query, String privilegeKey) {
        return permittedSearchRepository.search(query, Event.class, privilegeKey);
    }
}
