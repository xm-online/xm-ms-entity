package com.icthh.xm.ms.entity.web.rest.facade;

import com.icthh.xm.ms.entity.domain.Profile;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.service.ProfileService;
import com.icthh.xm.ms.entity.service.dto.XmEntityDto;
import com.icthh.xm.ms.entity.service.mapper.XmEntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProfileFacade {

    private final ProfileService profileService;
    private final XmEntityMapper xmEntityMapper;

    public Optional<XmEntityDto> getSelfProfile() {
        Profile profile = profileService.getSelfProfile();
        return Optional.ofNullable(profile)
            .map(Profile::getXmentity)
            .map(xmEntityMapper::toDto);
    }

    public XmEntityDto updateProfile(XmEntityDto dto) {
        XmEntity entity = xmEntityMapper.toEntity(dto);
        XmEntity result = profileService.updateProfile(entity);
        return xmEntityMapper.toDto(result);
    }

    public Optional<XmEntityDto> getProfile(String userKey) {
        Profile profile = profileService.getProfile(userKey);
        return Optional.ofNullable(profile)
            .map(Profile::getXmentity)
            .map(xmEntityMapper::toDto);
    }
}
