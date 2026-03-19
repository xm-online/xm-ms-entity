package com.icthh.xm.ms.entity.service.mapper;

import com.icthh.xm.ms.entity.domain.Comment;
import com.icthh.xm.ms.entity.service.dto.CommentDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring")
public abstract class CommentMapper extends LazyLoadingAwareMapper {

    @Mapping(target = "xmEntity", qualifiedByName = "shallowXmEntityToDto")
    @Mapping(target = "replies", conditionExpression = "java(org.hibernate.Hibernate.isInitialized(entity.getReplies()))")
    public abstract CommentDto toDto(Comment entity);

    @Mapping(target = "xmEntity", qualifiedByName = "shallowXmEntityToEntity")
    public abstract Comment toEntity(CommentDto dto);

    public abstract Set<CommentDto> toDtoSet(Set<Comment> entities);

    public abstract Set<Comment> toEntitySet(Set<CommentDto> dtos);

    public abstract List<CommentDto> toDtoList(List<Comment> entities);
}
