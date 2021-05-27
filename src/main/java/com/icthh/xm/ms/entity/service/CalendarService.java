package com.icthh.xm.ms.entity.service;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.commons.permission.annotation.FindWithPermission;
import com.icthh.xm.commons.permission.annotation.PrivilegeDescription;
import com.icthh.xm.commons.permission.repository.PermittedRepository;
import com.icthh.xm.ms.entity.domain.Calendar;
import com.icthh.xm.ms.entity.domain.Event;
import com.icthh.xm.ms.entity.domain.spec.CalendarSpec;
import com.icthh.xm.ms.entity.repository.CalendarRepository;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
import com.icthh.xm.ms.entity.repository.search.PermittedSearchRepository;
import com.icthh.xm.ms.entity.service.impl.StartUpdateDateGenerationStrategy;
import com.icthh.xm.ms.entity.service.query.EventQueryService;
import com.icthh.xm.ms.entity.service.query.filter.EventFilter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing Calendar.
 */
@Service
@Transactional
@RequiredArgsConstructor
@LepService(group = "service.calendar")
public class CalendarService {

    private final CalendarRepository calendarRepository;
    private final PermittedRepository permittedRepository;
    private final PermittedSearchRepository permittedSearchRepository;
    private final StartUpdateDateGenerationStrategy startUpdateDateGenerationStrategy;
    private final XmEntityRepository xmEntityRepository;
    private final EventQueryService eventQueryService;
    private final XmEntitySpecService specService;

    /**
     * Save a calendar.
     *
     * @param calendar the entity to save
     * @return the persisted entity
     */
    @LogicExtensionPoint("Save")
    public Calendar save(Calendar calendar) {

        startUpdateDateGenerationStrategy.preProcessStartDate(calendar,
                                                              calendar.getId(),
                                                              calendarRepository,
                                                              Calendar::setStartDate,
                                                              Calendar::getStartDate);
        calendar.setXmEntity(xmEntityRepository.getOne(calendar.getXmEntity().getId()));
        assertReadOnlyCalendar(calendar);
        return calendarRepository.save(calendar);
    }

    public void assertReadOnlyCalendar(Calendar calendar) {
        if (calendar == null || calendar.getId() == null) {
            return;
        }

        specService
            .findCalendar(calendar.getXmEntity().getTypeKey(), calendar.getTypeKey())
            .map(CalendarSpec::getReadonly)
            .filter(isReadonly -> isReadonly)
            .ifPresent(isReadonly -> {
                throw new BusinessException("error.read.only.calendar", "Cannot update read only calendar");
            });
    }

    /**
     *  Get all the calendars.
     *
     *  @return the list of entities
     */
    @Transactional(readOnly = true)
    @FindWithPermission("CALENDAR.GET_LIST")
    @LogicExtensionPoint("FindAll")
    @PrivilegeDescription("Privilege to get all the calendars")
    public List<Calendar> findAll(String privilegeKey) {
        return permittedRepository.findAll(Calendar.class, privilegeKey);
    }

    /**
     *  Get one calendar by id.
     *
     *  @param id the id of the entity
     *  @return the entity
     */
    @Transactional(readOnly = true)
    @LogicExtensionPoint("FindOne")
    public Calendar findOne(Long id) {
        return calendarRepository.findById(id).orElse(null);
    }

    /**
     *  Delete the  calendar by id.
     *
     *  @param id the id of the entity
     */
    @LogicExtensionPoint("Delete")
    public void delete(Long id) {
        calendarRepository.deleteById(id);
    }

    /**
     * Search for the calendar corresponding to the query.
     *
     *  @param query the query of the search
     *  @return the list of entities
     */
    @Deprecated
    @Transactional(readOnly = true)
    @FindWithPermission("CALENDAR.SEARCH")
    @LogicExtensionPoint("Search")
    @PrivilegeDescription("Privilege to search for the calendar corresponding to the query")
    public List<Calendar> search(String query, String privilegeKey) {
        return permittedSearchRepository.search(query, Calendar.class, privilegeKey);
    }

    @Transactional(readOnly = true)
    @LogicExtensionPoint("FindEvents")
    public Page<Event> findEvents(Long calendarId, EventFilter filter, Pageable pageable) {
        return eventQueryService.findAllByCalendarId(calendarId, filter, pageable);
    }
}
