package com.icthh.xm.ms.entity.service.mapper;

import com.icthh.xm.ms.entity.domain.Profile;
import com.icthh.xm.ms.entity.service.dto.ProfileDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {XmEntityMapper.class, XmEntityRefMapper.class})
public abstract class ProfileMapper extends LazyLoadingAwareMapper {

    public abstract ProfileDto toDto(Profile entity);

    public abstract Profile toEntity(ProfileDto dto);
}
