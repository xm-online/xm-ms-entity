package com.icthh.xm.ms.entity.service.mapper;

import com.icthh.xm.ms.entity.domain.Tag;
import com.icthh.xm.ms.entity.service.dto.TagDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Set;

@Mapper(componentModel = "spring", uses = {XmEntityRefMapper.class})
public abstract class TagMapper extends LazyLoadingAwareMapper {

    @Mapping(target = "xmEntity", qualifiedByName = "shallowXmEntityToDto")
    public abstract TagDto toDto(Tag entity);

    @Mapping(target = "xmEntity", qualifiedByName = "shallowXmEntityToEntity")
    public abstract Tag toEntity(TagDto dto);

    public abstract Set<TagDto> toDtoSet(Set<Tag> entities);

    public abstract Set<Tag> toEntitySet(Set<TagDto> dtos);
}
