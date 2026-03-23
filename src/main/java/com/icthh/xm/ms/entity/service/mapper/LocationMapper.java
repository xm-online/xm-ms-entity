package com.icthh.xm.ms.entity.service.mapper;

import com.icthh.xm.ms.entity.domain.Location;
import com.icthh.xm.ms.entity.service.dto.LocationDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Set;

@Mapper(componentModel = "spring", uses = {XmEntityRefMapper.class})
public abstract class LocationMapper extends LazyLoadingAwareMapper {

    @Mapping(target = "xmEntity", qualifiedByName = "shallowXmEntityToDto")
    public abstract LocationDto toDto(Location entity);

    @Mapping(target = "xmEntity", qualifiedByName = "shallowXmEntityToEntity")
    public abstract Location toEntity(LocationDto dto);

    public abstract Set<LocationDto> toDtoSet(Set<Location> entities);

    public abstract Set<Location> toEntitySet(Set<LocationDto> dtos);
}
