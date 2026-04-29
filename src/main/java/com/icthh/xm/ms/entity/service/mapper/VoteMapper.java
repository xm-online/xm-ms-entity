package com.icthh.xm.ms.entity.service.mapper;

import com.icthh.xm.ms.entity.domain.Rating;
import com.icthh.xm.ms.entity.domain.Vote;
import com.icthh.xm.ms.entity.service.dto.RatingDto;
import com.icthh.xm.ms.entity.service.dto.VoteDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

@Mapper(componentModel = "spring", uses = {XmEntityRefMapper.class})
public abstract class VoteMapper extends LazyLoadingAwareMapper {

    @Autowired
    @Lazy
    private RatingMapper ratingMapper;

    @Mapping(target = "xmEntity", qualifiedByName = "fullXmEntityToDto")
    @Mapping(target = "rating", qualifiedByName = "shallowRatingToDto")
    public abstract VoteDto toDto(Vote entity);

    @Mapping(target = "xmEntity", qualifiedByName = "fullXmEntityToEntity")
    @Mapping(target = "rating", qualifiedByName = "shallowRatingToEntity")
    public abstract Vote toEntity(VoteDto dto);

    public abstract Set<VoteDto> toDtoSet(Set<Vote> entities);

    public abstract Set<Vote> toEntitySet(Set<VoteDto> dtos);

    @Named("shallowRatingToDto")
    protected RatingDto shallowRatingToDto(Rating entity) {
        if (entity == null) {
            return null;
        }

        return ratingMapper.toDtoWithoutVotes(entity);
    }

    @Named("shallowRatingToEntity")
    protected Rating shallowRatingToEntity(RatingDto dto) {
        if (dto == null) {
            return null;
        }

        return ratingMapper.toEntityWithoutVotes(dto);
    }
}
