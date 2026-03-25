package com.icthh.xm.ms.entity.service.mapper;

import com.icthh.xm.ms.entity.domain.Rating;
import com.icthh.xm.ms.entity.service.dto.RatingDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Set;

@Mapper(componentModel = "spring", uses = {VoteMapper.class, XmEntityRefMapper.class})
public abstract class RatingMapper extends LazyLoadingAwareMapper {

    @Mapping(target = "xmEntity", qualifiedByName = "shallowXmEntityToDto")
    @Mapping(target = "votes", conditionExpression = "java(org.hibernate.Hibernate.isInitialized(entity.getVotes()))")
    public abstract RatingDto toDto(Rating entity);

    @Mapping(target = "xmEntity", qualifiedByName = "shallowXmEntityToEntity")
    public abstract Rating toEntity(RatingDto dto);

    public abstract Set<RatingDto> toDtoSet(Set<Rating> entities);

    public abstract Set<Rating> toEntitySet(Set<RatingDto> dtos);
}
