package com.icthh.xm.ms.entity.service.mapper;

import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.service.dto.XmEntityDto;
import org.hibernate.Hibernate;
import org.mapstruct.Named;

import java.util.Set;

public class LazyLoadingAwareMapper {

    protected <T> Set<T> safeSet(Set<T> set) {
        return Hibernate.isInitialized(set) ? set : null;
    }

    protected <T> T safeRef(T ref) {
        return Hibernate.isInitialized(ref) ? ref : null;
    }

    @Named("shallowXmEntityToDto")
    protected XmEntityDto shallowXmEntityToDto(XmEntity entity) {
        if (entity == null) return null;
        if (!Hibernate.isInitialized(entity)) return null;
        XmEntityDto dto = new XmEntityDto();
        dto.setId(entity.getId());
        return dto;
    }

    @Named("shallowXmEntityToEntity")
    protected XmEntity shallowXmEntityToEntity(XmEntityDto dto) {
        if (dto == null) return null;
        XmEntity entity = new XmEntity();
        entity.setId(dto.getId());
        return entity;
    }
}
