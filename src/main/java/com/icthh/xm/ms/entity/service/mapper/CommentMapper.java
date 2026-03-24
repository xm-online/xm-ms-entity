package com.icthh.xm.ms.entity.service.mapper;

import com.icthh.xm.ms.entity.domain.Comment;
import com.icthh.xm.ms.entity.service.dto.CommentDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring", uses = {XmEntityRefMapper.class})
public abstract class CommentMapper extends LazyLoadingAwareMapper {

    @Mapping(target = "xmEntity", qualifiedByName = "shallowXmEntityToDto")
    @Mapping(target = "replies", conditionExpression = "java(org.hibernate.Hibernate.isInitialized(entity.getReplies()))")
    @Mapping(target = "comment", qualifiedByName = "shallowCommentToDto")
    public abstract CommentDto toDto(Comment entity);

    @Mapping(target = "xmEntity", qualifiedByName = "shallowXmEntityToEntity")
    @Mapping(target = "comment", qualifiedByName = "shallowCommentToEntity")
    public abstract Comment toEntity(CommentDto dto);

    public abstract Set<CommentDto> toDtoSet(Set<Comment> entities);

    public abstract Set<Comment> toEntitySet(Set<CommentDto> dtos);

    public abstract List<CommentDto> toDtoList(List<Comment> entities);

    @Named("shallowCommentToDto")
    protected CommentDto shallowCommentToDto(Comment entity) {
        if (entity == null) {
            return null;
        }
        CommentDto dto = new CommentDto();
        dto.setId(entity.getId());
        dto.setUserKey(entity.getUserKey());
        dto.setMessage(entity.getMessage());
        dto.setEntryDate(entity.getEntryDate());
        return dto;
    }

    @Named("shallowCommentToEntity")
    protected Comment shallowCommentToEntity(CommentDto dto) {
        if (dto == null) {
            return null;
        }
        Comment entity = new Comment();
        entity.setId(dto.getId());
        entity.setUserKey(dto.getUserKey());
        entity.setMessage(dto.getMessage());
        entity.setEntryDate(dto.getEntryDate());
        return entity;
    }
}
