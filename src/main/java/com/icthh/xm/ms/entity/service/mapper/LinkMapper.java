package com.icthh.xm.ms.entity.service.mapper;

import com.icthh.xm.ms.entity.domain.Link;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.service.dto.LinkDto;
import com.icthh.xm.ms.entity.service.dto.XmEntityDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring", uses = {XmEntityRefMapper.class})
public abstract class LinkMapper extends LazyLoadingAwareMapper {

    @Autowired
    protected XmEntityRefMapper xmEntityRefMapper;

    @Mapping(target = "target", qualifiedByName = "targetXmEntityToDto")
    @Mapping(target = "source", qualifiedByName = "shallowXmEntityToDto")
    public abstract LinkDto toDto(Link entity);

    @Mapping(target = "target", qualifiedByName = "targetXmEntityToEntity")
    @Mapping(target = "source", qualifiedByName = "shallowXmEntityToEntity")
    public abstract Link toEntity(LinkDto dto);

    public abstract Set<LinkDto> toDtoSet(Set<Link> entities);

    public abstract Set<Link> toEntitySet(Set<LinkDto> dtos);

    public abstract List<LinkDto> toDtoList(List<Link> entities);

    @Named("targetXmEntityToDto")
    protected XmEntityDto targetXmEntityToDto(XmEntity entity) {
        XmEntityDto dto = xmEntityRefMapper.fullXmEntityToDto(entity);
        if (dto != null) {
            XmEntityRefMapper.nullifyCollections(dto);
            // Match SimpleLinkSerializer: version and updatedBy are not included in link target
            dto.setVersion(null);
            dto.setUpdatedBy(null);
        }
        return dto;
    }

    @Named("targetXmEntityToEntity")
    protected XmEntity targetXmEntityToEntity(XmEntityDto dto) {
        return xmEntityRefMapper.fullXmEntityToEntity(dto);
    }
}
