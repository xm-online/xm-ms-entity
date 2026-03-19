package com.icthh.xm.ms.entity.service.mapper;

import com.icthh.xm.ms.entity.domain.Calendar;
import com.icthh.xm.ms.entity.service.dto.CalendarDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Set;

@Mapper(componentModel = "spring", uses = {EventMapper.class})
public abstract class CalendarMapper extends LazyLoadingAwareMapper {

    @Mapping(target = "xmEntity", qualifiedByName = "shallowXmEntityToDto")
    @Mapping(target = "events", conditionExpression = "java(org.hibernate.Hibernate.isInitialized(entity.getEvents()))")
    public abstract CalendarDto toDto(Calendar entity);

    @Mapping(target = "xmEntity", qualifiedByName = "shallowXmEntityToEntity")
    public abstract Calendar toEntity(CalendarDto dto);

    public abstract Set<CalendarDto> toDtoSet(Set<Calendar> entities);

    public abstract Set<Calendar> toEntitySet(Set<CalendarDto> dtos);
}
