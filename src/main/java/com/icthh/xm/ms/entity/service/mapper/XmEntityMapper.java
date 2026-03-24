package com.icthh.xm.ms.entity.service.mapper;

import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.service.dto.XmEntityDto;
import org.hibernate.Hibernate;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring", uses = {
    AttachmentMapper.class,
    CalendarMapper.class,
    CommentMapper.class,
    LinkMapper.class,
    LocationMapper.class,
    RatingMapper.class,
    TagMapper.class,
    FunctionContextMapper.class,
    VoteMapper.class,
    EventMapper.class,
    XmEntityRefMapper.class
})
public abstract class XmEntityMapper extends LazyLoadingAwareMapper {

    @Mapping(target = "attachments", conditionExpression = "java(org.hibernate.Hibernate.isInitialized(entity.getAttachments()))")
    @Mapping(target = "calendars", conditionExpression = "java(org.hibernate.Hibernate.isInitialized(entity.getCalendars()))")
    @Mapping(target = "locations", conditionExpression = "java(org.hibernate.Hibernate.isInitialized(entity.getLocations()))")
    @Mapping(target = "ratings", conditionExpression = "java(org.hibernate.Hibernate.isInitialized(entity.getRatings()))")
    @Mapping(target = "tags", conditionExpression = "java(org.hibernate.Hibernate.isInitialized(entity.getTags()))")
    @Mapping(target = "comments", conditionExpression = "java(org.hibernate.Hibernate.isInitialized(entity.getComments()))")
    @Mapping(target = "votes", conditionExpression = "java(org.hibernate.Hibernate.isInitialized(entity.getVotes()))")
    @Mapping(target = "sources", conditionExpression = "java(org.hibernate.Hibernate.isInitialized(entity.getSources()))")
    @Mapping(target = "targets", conditionExpression = "java(org.hibernate.Hibernate.isInitialized(entity.getTargets()))")
    @Mapping(target = "functionContexts", conditionExpression = "java(org.hibernate.Hibernate.isInitialized(entity.getFunctionContexts()))")
    @Mapping(target = "events", conditionExpression = "java(org.hibernate.Hibernate.isInitialized(entity.getEvents()))")
    @Mapping(target = "avatarUrlRelative", source = "avatarUrlRelative")
    @Mapping(target = "avatarUrlFull", expression = "java(entity.getAvatarUrl())")
    public abstract XmEntityDto toDto(XmEntity entity);

    /**
     * Null out DTO collections that were not initialized in the entity (lazy-loaded).
     * The conditionExpression skips mapping for uninitialized collections,
     * leaving the DTO's default empty HashSet. This @AfterMapping nullifies them
     * to match the old Hibernate module behavior (null for unloaded collections).
     */
    @AfterMapping
    protected void nullifyUninitializedCollections(XmEntity entity, @MappingTarget XmEntityDto dto) {
        if (!Hibernate.isInitialized(entity.getAttachments())) dto.setAttachments(null);
        if (!Hibernate.isInitialized(entity.getCalendars())) dto.setCalendars(null);
        if (!Hibernate.isInitialized(entity.getLocations())) dto.setLocations(null);
        if (!Hibernate.isInitialized(entity.getRatings())) dto.setRatings(null);
        if (!Hibernate.isInitialized(entity.getTags())) dto.setTags(null);
        if (!Hibernate.isInitialized(entity.getComments())) dto.setComments(null);
        if (!Hibernate.isInitialized(entity.getVotes())) dto.setVotes(null);
        if (!Hibernate.isInitialized(entity.getSources())) dto.setSources(null);
        if (!Hibernate.isInitialized(entity.getTargets())) dto.setTargets(null);
        if (!Hibernate.isInitialized(entity.getFunctionContexts())) dto.setFunctionContexts(null);
        if (!Hibernate.isInitialized(entity.getEvents())) dto.setEvents(null);
    }

    @Mapping(target = "avatarUrlRelative", source = "avatarUrlRelative")
    @Mapping(target = "avatarUrlFull", source = "avatarUrlFull")
    @Mapping(target = "uniqueFields", ignore = true)
    public abstract XmEntity toEntity(XmEntityDto dto);

    public abstract List<XmEntityDto> toDtoList(List<XmEntity> entities);

    @Named("_shallowDtoInternal")
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
    @Mapping(target = "avatarUrlRelative", source = "avatarUrlRelative")
    @Mapping(target = "avatarUrlFull", expression = "java(entity.getAvatarUrl())")
    protected abstract XmEntityDto toShallowDtoInternal(XmEntity entity);

    @Named("shallowDto")
    public XmEntityDto toShallowDto(XmEntity entity) {
        XmEntityDto dto = toShallowDtoInternal(entity);
        if (dto != null) {
            XmEntityRefMapper.nullifyCollections(dto);
        }
        return dto;
    }
}
