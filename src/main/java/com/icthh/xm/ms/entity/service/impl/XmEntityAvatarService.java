package com.icthh.xm.ms.entity.service.impl;

import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.ext.IdOrKey;
import com.icthh.xm.ms.entity.service.ProfileService;
import com.icthh.xm.ms.entity.service.StorageService;
import com.icthh.xm.ms.entity.service.XmEntityService;
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
    private final StorageService storageService;
    private final ProfileService profileService;

    @Transactional
    public URI updateAvatar(IdOrKey idOrKey, HttpEntity<Resource> avatarHttpEntity) {
        XmEntity source;

        if (idOrKey.isSelf()) {
            source = profileService.getSelfProfile().getXmentity();
            log.debug("Self resolved entity id = {}, typeKet = {}", source.getId(), source.getTypeKey());
        } else {
            source = xmEntityService.findOne(idOrKey, List.of());
            log.debug("Resolved entity id = {}, typeKet = {}", source.getId(), source.getTypeKey());
        }

        String avatarUrl = storageService.store(avatarHttpEntity, null);
        log.info("Avatar {} stored for entity {}", avatarUrl, idOrKey);

        source.setAvatarUrl(avatarUrl);
        return URI.create(avatarUrl);
    }

}
