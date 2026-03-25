package com.icthh.xm.ms.entity.service.mapper;

import com.icthh.xm.ms.entity.domain.Content;
import com.icthh.xm.ms.entity.service.dto.ContentDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.hibernate.Hibernate;

@Mapper(componentModel = "spring")
public interface ContentMapper {

    @Mapping(target = "value", source = "content", qualifiedByName = "getContentValue")
    ContentDto toDto(Content content);

    Content toEntity(ContentDto dto);

    @Named("getContentValue")
    default byte[] getContentValue(Content content) {
        if (content == null) {
            return null;
        }
        if (!Hibernate.isInitialized(content)) {
            return null;
        }
        return content.getValue();
    }
}
