package com.icthh.xm.ms.entity.web.rest.facade;

import com.icthh.xm.ms.entity.domain.Event;
import com.icthh.xm.ms.entity.service.EventService;
import com.icthh.xm.ms.entity.service.dto.EventDto;
import com.icthh.xm.ms.entity.service.mapper.EventMapper;
import com.icthh.xm.ms.entity.service.query.filter.EventFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EventFacade {

    private final EventService eventService;
    private final EventMapper eventMapper;

    public EventDto save(EventDto dto) {
        Event entity = eventMapper.toEntity(dto);
        Event saved = eventService.save(entity);
        return eventMapper.toDto(saved);
    }

    public List<EventDto> findAll(String privilegeKey) {
        return eventService.findAll(privilegeKey).stream()
            .map(eventMapper::toDto)
            .toList();
    }

    public List<EventDto> findAllByFilter(EventFilter filter) {
        return eventService.findAllByFilter(filter).stream()
            .map(eventMapper::toDto)
            .toList();
    }

    // TODO: EventService.findAll(EventFilter, Pageable, String) does not exist yet
    // public Page<EventDto> findAll(EventFilter filter, Pageable pageable, String privilegeKey) {
    //     return eventService.findAll(filter, pageable, privilegeKey).map(eventMapper::toDto);
    // }

    public EventDto findOne(Long id) {
        return eventMapper.toDto(eventService.findOne(id));
    }

    public void delete(Long id) {
        eventService.delete(id);
    }
}
