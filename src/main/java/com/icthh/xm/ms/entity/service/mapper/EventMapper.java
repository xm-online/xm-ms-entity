package com.icthh.xm.ms.entity.service.mapper;

import com.icthh.xm.ms.entity.domain.Calendar;
import com.icthh.xm.ms.entity.domain.Event;
import com.icthh.xm.ms.entity.service.dto.CalendarDto;
import com.icthh.xm.ms.entity.service.dto.EventDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Set;

@Mapper(componentModel = "spring", uses = {XmEntityRefMapper.class})
public abstract class EventMapper extends LazyLoadingAwareMapper {

    @Mapping(target = "assigned", qualifiedByName = "shallowXmEntityToDto")
    @Mapping(target = "calendar", qualifiedByName = "shallowCalendarToDto")
    @Mapping(target = "eventDataRef", qualifiedByName = "fullXmEntityToDto")
    public abstract EventDto toDto(Event entity);

    @Mapping(target = "assigned", qualifiedByName = "shallowXmEntityToEntity")
    @Mapping(target = "calendar", qualifiedByName = "shallowCalendarToEntity")
    @Mapping(target = "eventDataRef", qualifiedByName = "fullXmEntityToEntity")
    public abstract Event toEntity(EventDto dto);

    public abstract Set<EventDto> toDtoSet(Set<Event> entities);

    public abstract Set<Event> toEntitySet(Set<EventDto> dtos);

    @Named("shallowCalendarToDto")
    protected CalendarDto shallowCalendarToDto(Calendar entity) {
        if (entity == null) {
            return null;
        }
        CalendarDto dto = new CalendarDto();
        dto.setId(entity.getId());
        return dto;
    }

    @Named("shallowCalendarToEntity")
    protected Calendar shallowCalendarToEntity(CalendarDto dto) {
        if (dto == null) {
            return null;
        }
        Calendar entity = new Calendar();
        entity.setId(dto.getId());
        entity.setTypeKey(dto.getTypeKey());
        return entity;
    }
}
