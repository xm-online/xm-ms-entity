package com.icthh.xm.ms.entity.service.mapper;

import com.icthh.xm.ms.entity.domain.Rating;
import com.icthh.xm.ms.entity.domain.Vote;
import com.icthh.xm.ms.entity.service.dto.RatingDto;
import com.icthh.xm.ms.entity.service.dto.VoteDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

@Mapper(componentModel = "spring", uses = {XmEntityRefMapper.class})
public abstract class RatingMapper extends LazyLoadingAwareMapper {

    @Autowired
    @Lazy
    private VoteMapper voteMapper;

    @Mapping(target = "xmEntity", qualifiedByName = "shallowXmEntityToDto")
    @Mapping(target = "votes", qualifiedByName = "shallowVotesToDto")
    public abstract RatingDto toDto(Rating entity);

    @Named("shallowRatingToDto")
    @Mapping(target = "xmEntity", qualifiedByName = "shallowXmEntityToDto")
    @Mapping(target = "votes", ignore = true)
    public abstract RatingDto toDtoWithoutVotes(Rating entity);

    @Mapping(target = "xmEntity", qualifiedByName = "shallowXmEntityToEntity")
    @Mapping(target = "votes", qualifiedByName = "shallowVotesToEntity")
    public abstract Rating toEntity(RatingDto dto);

    @Named("shallowRatingToEntity")
    @Mapping(target = "xmEntity", qualifiedByName = "shallowXmEntityToEntity")
    @Mapping(target = "votes", ignore = true)
    public abstract Rating toEntityWithoutVotes(RatingDto dto);

    public abstract Set<RatingDto> toDtoSet(Set<Rating> entities);

    public abstract Set<Rating> toEntitySet(Set<RatingDto> dtos);

    @Named("shallowVotesToDto")
    protected Set<VoteDto> shallowVotesToDto(Set<Vote> entities) {
        if (!org.hibernate.Hibernate.isInitialized(entities) || entities == null) {
            return null;
        }
        return entities.stream()
            .map(voteMapper::toDto)
            .collect(Collectors.toSet());
    }

    @Named("shallowVotesToEntity")
    protected Set<Vote> shallowVotesToEntity(Set<VoteDto> dtos) {
        if (dtos == null) {
            return null;
        }
        return dtos.stream()
            .map(voteMapper::toEntity)
            .collect(Collectors.toSet());
    }
}
