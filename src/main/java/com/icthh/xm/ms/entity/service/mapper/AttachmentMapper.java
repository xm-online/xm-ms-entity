package com.icthh.xm.ms.entity.service.mapper;

import com.icthh.xm.ms.entity.domain.Attachment;
import com.icthh.xm.ms.entity.service.dto.AttachmentDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring", uses = {ContentMapper.class, XmEntityRefMapper.class})
public abstract class AttachmentMapper extends LazyLoadingAwareMapper {

    @Mapping(target = "xmEntity", qualifiedByName = "shallowXmEntityToDto")
    public abstract AttachmentDto toDto(Attachment entity);

    @Mapping(target = "xmEntity", qualifiedByName = "shallowXmEntityToEntity")
    public abstract Attachment toEntity(AttachmentDto dto);

    public abstract Set<AttachmentDto> toDtoSet(Set<Attachment> entities);

    public abstract Set<Attachment> toEntitySet(Set<AttachmentDto> dtos);

    public abstract List<AttachmentDto> toDtoList(List<Attachment> entities);
}
