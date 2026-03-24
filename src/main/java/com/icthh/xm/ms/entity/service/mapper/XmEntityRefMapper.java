package com.icthh.xm.ms.entity.service.mapper;

import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.service.dto.XmEntityDto;
import org.hibernate.Hibernate;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

/**
 * Dedicated mapper for XmEntity references.
 * Extracted from LazyLoadingAwareMapper to avoid MapStruct ambiguity
 * when multiple mappers inherit the same base class and use each other.
 */
@Mapper(componentModel = "spring")
public abstract class XmEntityRefMapper {

    @Named("shallowXmEntityToDto")
    public XmEntityDto shallowXmEntityToDto(XmEntity entity) {
        if (entity == null) return null;
        if (!Hibernate.isInitialized(entity)) return null;
        XmEntityDto dto = new XmEntityDto();
        dto.setId(entity.getId());
        dto.setTypeKey(entity.getTypeKey());
        return dto;
    }

    @Named("shallowXmEntityToEntity")
    public XmEntity shallowXmEntityToEntity(XmEntityDto dto) {
        if (dto == null) return null;
        XmEntity entity = new XmEntity();
        entity.setId(dto.getId());
        entity.setTypeKey(dto.getTypeKey());
        return entity;
    }

    @Named("fullXmEntityToDto")
    @Mapping(target = "avatarUrlRelative", source = "avatarUrlRelative")
    @Mapping(target = "avatarUrlFull", expression = "java(entity.getAvatarUrl())")
    @Mapping(target = "attachments", ignore = true)
    @Mapping(target = "calendars", ignore = true)
    @Mapping(target = "locations", ignore = true)
    @Mapping(target = "ratings", ignore = true)
    @Mapping(target = "tags", ignore = true)
    @Mapping(target = "comments", ignore = true)
    @Mapping(target = "votes", ignore = true)
    @Mapping(target = "sources", ignore = true)
    @Mapping(target = "targets", ignore = true)
    @Mapping(target = "functionContexts", ignore = true)
    @Mapping(target = "events", ignore = true)
    public abstract XmEntityDto fullXmEntityToDto(XmEntity entity);

    @Named("fullXmEntityToEntity")
    @Mapping(target = "avatarUrlRelative", source = "avatarUrlRelative")
    @Mapping(target = "avatarUrlFull", source = "avatarUrlFull")
    @Mapping(target = "uniqueFields", ignore = true)
    public abstract XmEntity fullXmEntityToEntity(XmEntityDto dto);

    /**
     * Null out all collection fields on a DTO so they don't serialize as empty arrays.
     */
    public static void nullifyCollections(XmEntityDto dto) {
        dto.setTags(null);
        dto.setLocations(null);
        dto.setAttachments(null);
        dto.setComments(null);
        dto.setRatings(null);
        dto.setCalendars(null);
        dto.setTargets(null);
        dto.setSources(null);
        dto.setFunctionContexts(null);
        dto.setVotes(null);
        dto.setEvents(null);
    }
}
