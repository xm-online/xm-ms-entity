package com.icthh.xm.ms.entity.service.impl;

import com.icthh.xm.ms.entity.config.ApplicationProperties;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.ext.IdOrKey;
import com.icthh.xm.ms.entity.service.XmEntityService;
import com.icthh.xm.ms.entity.service.XmeStorageServiceFacade;
import com.icthh.xm.ms.entity.service.storage.AvatarStorageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class XmEntityAvatarService {

    private final XmEntityService xmEntityService;
    private final ApplicationProperties applicationProperties;
    private final XmeStorageServiceFacade xmeStorageServiceFacade;

    @Transactional
    public URI updateAvatar(IdOrKey idOrKey, HttpEntity<Resource> avatarHttpEntity) {
        XmEntity source = xmEntityService.findOne(idOrKey, List.of());

        String avatarUrl = xmeStorageServiceFacade.storeAvatar(
            avatarHttpEntity,
            applicationProperties.getObjectStorage().getMaxImageSize());
        log.info("Avatar {} stored for entity {}", avatarUrl, idOrKey);

        source.setAvatarUrl(avatarUrl);
        return URI.create(avatarUrl);
    }

    @Transactional(readOnly = true)
    public AvatarStorageResponse getAvatar(IdOrKey idOrKey) {
        XmEntity source = xmEntityService.findOne(idOrKey, List.of());
        return xmeStorageServiceFacade.getAvatarResource(source);
    }

}
