package com.icthh.xm.ms.entity.service.mapper;

import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.service.dto.XmEntityDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
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

    @Mapping(target = "avatarUrlRelative", source = "avatarUrlRelative")
    @Mapping(target = "avatarUrlFull", source = "avatarUrlFull")
    @Mapping(target = "uniqueFields", ignore = true)
    public abstract XmEntity toEntity(XmEntityDto dto);

    public abstract List<XmEntityDto> toDtoList(List<XmEntity> entities);

    @Named("shallowDto")
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
    public abstract XmEntityDto toShallowDto(XmEntity entity);
}
