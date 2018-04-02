package com.icthh.xm.ms.entity.web.rest;

import com.codahale.metrics.annotation.Timed;
import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.exceptions.ErrorConstants;
import com.icthh.xm.ms.entity.domain.Calendar;
import com.icthh.xm.ms.entity.service.CalendarService;
import com.icthh.xm.ms.entity.web.rest.util.HeaderUtil;
import com.icthh.xm.ms.entity.web.rest.util.RespContentUtil;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import javax.validation.Valid;

/**
 * REST controller for managing Calendar.
 */
@RestController
@RequestMapping("/api")
public class CalendarResource {

    private static final String ENTITY_NAME = "calendar";

    private final CalendarService calendarService;
    private final CalendarResource calendarResource;

    public CalendarResource(
                    CalendarService calendarService,
                    @Lazy CalendarResource calendarResource) {
        this.calendarService = calendarService;
        this.calendarResource = calendarResource;
    }

    /**
     * POST  /calendars : Create a new calendar.
     *
     * @param calendar the calendar to create
     * @return the ResponseEntity with status 201 (Created) and with body the new calendar, or with status 400 (Bad Request) if the calendar has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/calendars")
    @Timed
    @PreAuthorize("hasPermission({'calendar': #calendar}, 'CALENDAR.CREATE')")
    public ResponseEntity<Calendar> createCalendar(@Valid @RequestBody Calendar calendar) throws URISyntaxException {
        if (calendar.getId() != null) {
            throw new BusinessException(ErrorConstants.ERR_BUSINESS_IDEXISTS,
                                        "A new calendar cannot already have an ID");
        }
        Calendar result = calendarService.save(calendar);
        return ResponseEntity.created(new URI("/api/calendars/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * PUT  /calendars : Updates an existing calendar.
     *
     * @param calendar the calendar to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated calendar,
     * or with status 400 (Bad Request) if the calendar is not valid,
     * or with status 500 (Internal Server Error) if the calendar couldn't be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PutMapping("/calendars")
    @Timed
    @PreAuthorize("hasPermission({'id': #calendar.id, 'newCalendar': #calendar}, 'calendar', 'CALENDAR.UPDATE')")
    public ResponseEntity<Calendar> updateCalendar(@Valid @RequestBody Calendar calendar) throws URISyntaxException {
        if (calendar.getId() == null) {
            //in order to call method with permissions check
            return this.calendarResource.createCalendar(calendar);
        }
        Calendar result = calendarService.save(calendar);
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
    public List<Calendar> getAllCalendars() {
        return calendarService.findAll(null);
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
    public ResponseEntity<Calendar> getCalendar(@PathVariable Long id) {
        Calendar calendar = calendarService.findOne(id);
        return RespContentUtil.wrapOrNotFound(Optional.ofNullable(calendar));
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
    public ResponseEntity<Void> deleteCalendar(@PathVariable Long id) {
        calendarService.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(ENTITY_NAME, id.toString())).build();
    }

    /**
     * SEARCH  /_search/calendars?query=:query : search for the calendar corresponding
     * to the query.
     *
     * @param query the query of the calendar search
     * @return the result of the search
     */
    @GetMapping("/_search/calendars")
    @Timed
    public List<Calendar> searchCalendars(@RequestParam String query) {
        return calendarService.search(query, null);
    }

}
