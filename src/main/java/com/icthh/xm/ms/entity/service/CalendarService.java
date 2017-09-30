package com.icthh.xm.ms.entity.service;

import com.icthh.xm.ms.entity.domain.Calendar;
import com.icthh.xm.ms.entity.repository.CalendarRepository;
import com.icthh.xm.ms.entity.repository.search.CalendarSearchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.elasticsearch.index.query.QueryBuilders.*;

/**
 * Service Implementation for managing Calendar.
 */
@Service
@Transactional
public class CalendarService {

    private final Logger log = LoggerFactory.getLogger(CalendarService.class);

    private final CalendarRepository calendarRepository;

    private final CalendarSearchRepository calendarSearchRepository;

    public CalendarService(CalendarRepository calendarRepository, CalendarSearchRepository calendarSearchRepository) {
        this.calendarRepository = calendarRepository;
        this.calendarSearchRepository = calendarSearchRepository;
    }

    /**
     * Save a calendar.
     *
     * @param calendar the entity to save
     * @return the persisted entity
     */
    public Calendar save(Calendar calendar) {
        log.debug("Request to save Calendar : {}", calendar);
        Calendar result = calendarRepository.save(calendar);
        calendarSearchRepository.save(result);
        return result;
    }

    /**
     *  Get all the calendars.
     *
     *  @return the list of entities
     */
    @Transactional(readOnly = true)
    public List<Calendar> findAll() {
        log.debug("Request to get all Calendars");
        return calendarRepository.findAll();
    }

    /**
     *  Get one calendar by id.
     *
     *  @param id the id of the entity
     *  @return the entity
     */
    @Transactional(readOnly = true)
    public Calendar findOne(Long id) {
        log.debug("Request to get Calendar : {}", id);
        return calendarRepository.findOne(id);
    }

    /**
     *  Delete the  calendar by id.
     *
     *  @param id the id of the entity
     */
    public void delete(Long id) {
        log.debug("Request to delete Calendar : {}", id);
        calendarRepository.delete(id);
        calendarSearchRepository.delete(id);
    }

    /**
     * Search for the calendar corresponding to the query.
     *
     *  @param query the query of the search
     *  @return the list of entities
     */
    @Transactional(readOnly = true)
    public List<Calendar> search(String query) {
        log.debug("Request to search Calendars for query {}", query);
        return StreamSupport
            .stream(calendarSearchRepository.search(queryStringQuery(query)).spliterator(), false)
            .collect(Collectors.toList());
    }
}
