package com.icthh.xm.ms.entity.service.mapper;

import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.service.dto.XmEntityDto;
import org.hibernate.Hibernate;
import org.mapstruct.Mapper;
import org.mapstruct.Named;

/**
 * Dedicated mapper for shallow XmEntity references (ID only).
 * Extracted from LazyLoadingAwareMapper to avoid MapStruct ambiguity
 * when multiple mappers inherit the same base class and use each other.
 */
@Mapper(componentModel = "spring")
public class XmEntityRefMapper {

    @Named("shallowXmEntityToDto")
    public XmEntityDto shallowXmEntityToDto(XmEntity entity) {
        if (entity == null) return null;
        if (!Hibernate.isInitialized(entity)) return null;
        XmEntityDto dto = new XmEntityDto();
        dto.setId(entity.getId());
        return dto;
    }

    @Named("shallowXmEntityToEntity")
    public XmEntity shallowXmEntityToEntity(XmEntityDto dto) {
        if (dto == null) return null;
        XmEntity entity = new XmEntity();
        entity.setId(dto.getId());
        return entity;
    }
}
