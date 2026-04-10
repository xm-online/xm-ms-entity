package com.icthh.xm.ms.entity.service.mapper;

import com.icthh.xm.ms.entity.domain.Content;
import com.icthh.xm.ms.entity.service.dto.ContentDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ContentMapper {

    ContentDto toDto(Content entity);

    Content toEntity(ContentDto dto);
}
