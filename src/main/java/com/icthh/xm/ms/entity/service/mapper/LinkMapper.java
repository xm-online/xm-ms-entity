package com.icthh.xm.ms.entity.service.mapper;

import com.icthh.xm.ms.entity.domain.Link;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.service.dto.LinkDto;
import com.icthh.xm.ms.entity.service.dto.XmEntityDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring")
public abstract class LinkMapper extends LazyLoadingAwareMapper {

    @Mapping(target = "target", qualifiedByName = "targetXmEntityToDto")
    @Mapping(target = "source", qualifiedByName = "shallowXmEntityToDto")
    public abstract LinkDto toDto(Link entity);

    @Mapping(target = "target", qualifiedByName = "targetXmEntityToEntity")
    @Mapping(target = "source", qualifiedByName = "shallowXmEntityToEntity")
    public abstract Link toEntity(LinkDto dto);

    public abstract Set<LinkDto> toDtoSet(Set<Link> entities);

    public abstract Set<Link> toEntitySet(Set<LinkDto> dtos);

    public abstract List<LinkDto> toDtoList(List<Link> entities);

    @Named("targetXmEntityToDto")
    protected XmEntityDto targetXmEntityToDto(XmEntity entity) {
        if (entity == null) return null;
        if (!org.hibernate.Hibernate.isInitialized(entity)) return null;
        XmEntityDto dto = new XmEntityDto();
        dto.setId(entity.getId());
        dto.setKey(entity.getKey());
        dto.setTypeKey(entity.getTypeKey());
        dto.setStateKey(entity.getStateKey());
        dto.setName(entity.getName());
        dto.setStartDate(entity.getStartDate());
        dto.setEndDate(entity.getEndDate());
        dto.setUpdateDate(entity.getUpdateDate());
        dto.setAvatarUrl(entity.getAvatarUrl());
        dto.setDescription(entity.getDescription());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setRemoved(entity.isRemoved());
        dto.setData(entity.getData());
        return dto;
    }

    @Named("targetXmEntityToEntity")
    protected XmEntity targetXmEntityToEntity(XmEntityDto dto) {
        if (dto == null) return null;
        XmEntity entity = new XmEntity();
        entity.setId(dto.getId());
        entity.setKey(dto.getKey());
        entity.setTypeKey(dto.getTypeKey());
        entity.setStateKey(dto.getStateKey());
        entity.setName(dto.getName());
        entity.setStartDate(dto.getStartDate());
        entity.setEndDate(dto.getEndDate());
        entity.setUpdateDate(dto.getUpdateDate());
        entity.setAvatarUrl(dto.getAvatarUrl());
        entity.setDescription(dto.getDescription());
        entity.setCreatedBy(dto.getCreatedBy());
        entity.setRemoved(dto.isRemoved());
        entity.setData(dto.getData());
        return entity;
    }
}
