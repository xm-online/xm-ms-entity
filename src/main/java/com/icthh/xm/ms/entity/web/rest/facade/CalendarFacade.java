package com.icthh.xm.ms.entity.web.rest.facade;

import com.icthh.xm.ms.entity.domain.Calendar;
import com.icthh.xm.ms.entity.service.CalendarService;
import com.icthh.xm.ms.entity.service.dto.CalendarDto;
import com.icthh.xm.ms.entity.service.dto.EventDto;
import com.icthh.xm.ms.entity.service.mapper.CalendarMapper;
import com.icthh.xm.ms.entity.service.mapper.EventMapper;
import com.icthh.xm.ms.entity.service.query.filter.EventFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CalendarFacade {

    private final CalendarService calendarService;
    private final CalendarMapper calendarMapper;
    private final EventMapper eventMapper;

    public CalendarDto save(CalendarDto dto) {
        Calendar entity = calendarMapper.toEntity(dto);
        Calendar saved = calendarService.save(entity);
        return calendarMapper.toDto(saved);
    }

    public List<CalendarDto> findAll(String privilegeKey) {
        return calendarService.findAll(privilegeKey).stream()
            .map(calendarMapper::toDto)
            .toList();
    }

    public CalendarDto findOne(Long id) {
        return calendarMapper.toDto(calendarService.findOne(id));
    }

    public void delete(Long id) {
        calendarService.delete(id);
    }

    public Page<EventDto> findEvents(Long calendarId, EventFilter filter, Pageable pageable) {
        return calendarService.findEvents(calendarId, filter, pageable).map(eventMapper::toDto);
    }
}
