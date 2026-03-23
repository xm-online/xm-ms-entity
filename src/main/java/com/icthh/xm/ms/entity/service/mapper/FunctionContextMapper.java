package com.icthh.xm.ms.entity.service.mapper;

import com.icthh.xm.ms.entity.domain.FunctionContext;
import com.icthh.xm.ms.entity.service.dto.FunctionContextDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Set;

@Mapper(componentModel = "spring", uses = {XmEntityRefMapper.class})
public abstract class FunctionContextMapper extends LazyLoadingAwareMapper {

    @Mapping(target = "xmEntity", qualifiedByName = "shallowXmEntityToDto")
    public abstract FunctionContextDto toDto(FunctionContext entity);

    @Mapping(target = "xmEntity", qualifiedByName = "shallowXmEntityToEntity")
    public abstract FunctionContext toEntity(FunctionContextDto dto);

    public abstract Set<FunctionContextDto> toDtoSet(Set<FunctionContext> entities);

    public abstract Set<FunctionContext> toEntitySet(Set<FunctionContextDto> dtos);
}
