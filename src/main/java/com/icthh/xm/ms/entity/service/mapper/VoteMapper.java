package com.icthh.xm.ms.entity.service.mapper;

import com.icthh.xm.ms.entity.domain.Rating;
import com.icthh.xm.ms.entity.domain.Vote;
import com.icthh.xm.ms.entity.service.dto.RatingDto;
import com.icthh.xm.ms.entity.service.dto.VoteDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Set;

@Mapper(componentModel = "spring", uses = {XmEntityRefMapper.class})
public abstract class VoteMapper extends LazyLoadingAwareMapper {

    @Mapping(target = "xmEntity", qualifiedByName = "shallowXmEntityToDto")
    @Mapping(target = "rating", qualifiedByName = "shallowRatingToDto")
    public abstract VoteDto toDto(Vote entity);

    @Mapping(target = "xmEntity", qualifiedByName = "shallowXmEntityToEntity")
    @Mapping(target = "rating", qualifiedByName = "shallowRatingToEntity")
    public abstract Vote toEntity(VoteDto dto);

    public abstract Set<VoteDto> toDtoSet(Set<Vote> entities);

    public abstract Set<Vote> toEntitySet(Set<VoteDto> dtos);

    @Named("shallowRatingToDto")
    protected RatingDto shallowRatingToDto(Rating entity) {
        if (entity == null) {
            return null;
        }
        RatingDto dto = new RatingDto();
        dto.setId(entity.getId());
        dto.setTypeKey(entity.getTypeKey());
        return dto;
    }

    @Named("shallowRatingToEntity")
    protected Rating shallowRatingToEntity(RatingDto dto) {
        if (dto == null) {
            return null;
        }
        Rating entity = new Rating();
        entity.setId(dto.getId());
        entity.setTypeKey(dto.getTypeKey());
        return entity;
    }
}
