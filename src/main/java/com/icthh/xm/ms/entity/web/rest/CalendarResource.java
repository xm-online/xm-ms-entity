package com.icthh.xm.ms.entity.web.rest;

import com.codahale.metrics.annotation.Timed;
import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.exceptions.ErrorConstants;
import com.icthh.xm.commons.permission.annotation.PrivilegeDescription;
import com.icthh.xm.ms.entity.service.dto.CalendarDto;
import com.icthh.xm.ms.entity.service.dto.EventDto;
import com.icthh.xm.ms.entity.web.rest.facade.CalendarFacade;
import com.icthh.xm.ms.entity.service.query.filter.EventFilter;
import com.icthh.xm.ms.entity.web.rest.util.HeaderUtil;
import com.icthh.xm.ms.entity.web.rest.util.PaginationUtil;
import com.icthh.xm.ms.entity.web.rest.util.RespContentUtil;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

import jakarta.validation.Valid;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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

/**
 * REST controller for managing Calendar.
 */
@RestController
@RequestMapping("/api")
public class CalendarResource {

    private static final String ENTITY_NAME = "calendar";

    private final CalendarFacade calendarFacade;
    private final CalendarResource calendarResource;

    public CalendarResource(
                    CalendarFacade calendarFacade,
                    @Lazy CalendarResource calendarResource) {
        this.calendarFacade = calendarFacade;
        this.calendarResource = calendarResource;
    }

    /**
     * POST  /calendars : Create a new calendar.
     *
     * @param calendar the calendar to create
     * @return the ResponseEntity with status 201 (Created) and with body the new calendar, or with status 400
     *      (Bad Request) if the calendar has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/calendars")
    @Timed
    @PreAuthorize("hasPermission({'calendar': #calendar}, 'CALENDAR.CREATE')")
    @PrivilegeDescription("Privilege to create a new calendar")
    public ResponseEntity<CalendarDto> createCalendar(@Valid @RequestBody CalendarDto calendar) throws URISyntaxException {
        if (calendar.getId() != null) {
            throw new BusinessException(ErrorConstants.ERR_BUSINESS_IDEXISTS,
                                        "A new calendar cannot already have an ID");
        }
        CalendarDto result = calendarFacade.save(calendar);
        return ResponseEntity.created(new URI("/api/calendars/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * PUT  /calendars : Updates an existing calendar.
     *
     * @param calendar the calendar to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated calendar,
     *      or with status 400 (Bad Request) if the calendar is not valid, or with status 500 (Internal Server Error)
     *      if the calendar couldn't be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PutMapping("/calendars")
    @Timed
    @PreAuthorize("hasPermission({'id': #calendar.id, 'newCalendar': #calendar}, 'calendar', 'CALENDAR.UPDATE')")
    @PrivilegeDescription("Privilege to updates an existing calendar")
    public ResponseEntity<CalendarDto> updateCalendar(@Valid @RequestBody CalendarDto calendar) throws URISyntaxException {
        if (calendar.getId() == null) {
            //in order to call method with permissions check
            return this.calendarResource.createCalendar(calendar);
        }
        CalendarDto result = calendarFacade.save(calendar);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(ENTITY_NAME, calendar.getId().toString()))
            .body(result);
    }

    /**
     * GET  /calendars : get all the calendars.
     *
     * @return the ResponseEntity with status 200 (OK) and the list of calendars in body
     */
    @GetMapping("/calendars")
    @Timed
    public List<CalendarDto> getAllCalendars() {
        return calendarFacade.findAll(null);
    }

    /**
     * GET  /calendars/:id : get the "id" calendar.
     *
     * @param id the id of the calendar to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the calendar, or with status 404 (Not Found)
     */
    @GetMapping("/calendars/{id}")
    @Timed
    @PostAuthorize("hasPermission({'returnObject': returnObject.body}, 'CALENDAR.GET_LIST.ITEM')")
    @PrivilegeDescription("Privilege to get the calendar by id")
    public ResponseEntity<CalendarDto> getCalendar(@PathVariable Long id) {
        CalendarDto calendar = calendarFacade.findOne(id);
        return RespContentUtil.wrapOrNotFound(Optional.ofNullable(calendar));
    }

    /**
     * GET  /calendars/{id}/events : get events for specific calendar.
     *
     * @param calendarId the id of the calendar to retrieve events
     * @return the ResponseEntity with status 200 (OK) and with body events
     */
    @GetMapping("/calendars/{calendarId}/events")
    @Timed
    @PreAuthorize("hasPermission({'calendarId': #calendarId}, 'calendar', 'CALENDAR.GET_LIST.ITEM.EVENTS')")
    @PrivilegeDescription("Privilege to get events for specific calendar")
    public ResponseEntity<List<EventDto>> getCalendarEvents(@PathVariable Long calendarId,
                                                         EventFilter filter,
                                                         Pageable pageable) {
        Page<EventDto> page = calendarFacade.findEvents(calendarId, filter, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/calendars/{id}/events");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    /**
     * DELETE  /calendars/:id : delete the "id" calendar.
     *
     * @param id the id of the calendar to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/calendars/{id}")
    @Timed
    @PreAuthorize("hasPermission({'id': #id}, 'calendar', 'CALENDAR.DELETE')")
    @PrivilegeDescription("Privilege to delete the calendar by id")
    public ResponseEntity<Void> deleteCalendar(@PathVariable Long id) {
        calendarFacade.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(ENTITY_NAME, id.toString())).build();
    }
}
