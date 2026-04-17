package com.icthh.xm.ms.entity.web.rest;

import com.codahale.metrics.annotation.Timed;
import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.exceptions.ErrorConstants;
import com.icthh.xm.commons.permission.annotation.PrivilegeDescription;
import com.icthh.xm.ms.entity.service.dto.EventDto;
import com.icthh.xm.ms.entity.web.rest.facade.EventFacade;
import com.icthh.xm.ms.entity.service.query.filter.EventFilter;
import com.icthh.xm.ms.entity.web.rest.util.HeaderUtil;
import com.icthh.xm.ms.entity.web.rest.util.RespContentUtil;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

/**
 * REST controller for managing Event.
 */
@RestController
@RequestMapping("/api")
public class EventResource {

    private static final String ENTITY_NAME = "event";

    private final EventFacade eventFacade;
    private final EventResource eventResource;

    public EventResource(
                    EventFacade eventFacade,
                    @Lazy EventResource eventResource) {
        this.eventFacade = eventFacade;
        this.eventResource = eventResource;
    }

    /**
     * POST  /events : Create a new event.
     *
     * @param event the event to create
     * @return the ResponseEntity with status 201 (Created) and with body the new event, or with status 400 (Bad Request) if the event has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/events")
    @Timed
    @PreAuthorize("hasPermission({'event': #event}, 'EVENT.CREATE')")
    @PrivilegeDescription("Privilege to create a new event")
    public ResponseEntity<EventDto> createEvent(@Valid @RequestBody EventDto event) throws URISyntaxException {
        if (event.getId() != null) {
            throw new BusinessException(ErrorConstants.ERR_BUSINESS_IDEXISTS,
                                        "A new event cannot already have an ID");
        }
        EventDto result = eventFacade.save(event);
        return ResponseEntity.created(new URI("/api/events/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * PUT  /events : Updates an existing event.
     *
     * @param event the event to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated event,
     * or with status 400 (Bad Request) if the event is not valid,
     * or with status 500 (Internal Server Error) if the event couldn't be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PutMapping("/events")
    @Timed
    @PreAuthorize("hasPermission({'id': #event.id, 'newEvent': #event}, 'event', 'EVENT.UPDATE')")
    @PrivilegeDescription("Privilege to updates an existing event")
    public ResponseEntity<EventDto> updateEvent(@Valid @RequestBody EventDto event) throws URISyntaxException {
        if (event.getId() == null) {
            //in order to call method with permissions check
            return this.eventResource.createEvent(event);
        }
        EventDto result = eventFacade.save(event);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(ENTITY_NAME, event.getId().toString()))
            .body(result);
    }

    /**
     * GET  /events : get all the events.
     *
     * @return the ResponseEntity with status 200 (OK) and the list of events in body
     */
    @GetMapping("/events")
    @Timed
    public List<EventDto> getAllEvents(EventFilter eventFilter) {
        return eventFilter == null ? eventFacade.findAll(null) : eventFacade.findAllByFilter(eventFilter);
    }

    /**
     * GET  /events/:id : get the "id" event.
     *
     * @param id the id of the event to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the event, or with status 404 (Not Found)
     */
    @GetMapping("/events/{id}")
    @Timed
    @PostAuthorize("hasPermission({'returnObject': returnObject.body}, 'EVENT.GET_LIST.ITEM')")
    @PrivilegeDescription("Privilege to get the event by id")
    public ResponseEntity<EventDto> getEvent(@PathVariable Long id) {
        EventDto event = eventFacade.findOne(id);
        return RespContentUtil.wrapOrNotFound(Optional.ofNullable(event));
    }

    /**
     * DELETE  /events/:id : delete the "id" event.
     *
     * @param id the id of the event to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/events/{id}")
    @Timed
    @PreAuthorize("hasPermission({'id': #id}, 'event', 'EVENT.DELETE')")
    @PrivilegeDescription("Privilege to delete the event by id")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long id) {
        eventFacade.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(ENTITY_NAME, id.toString())).build();
    }
}
