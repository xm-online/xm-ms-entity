package com.icthh.xm.ms.entity.service.impl;

import com.icthh.xm.ms.entity.domain.Attachment;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.service.AttachmentService;
import com.icthh.xm.ms.entity.service.StorageService;
import com.icthh.xm.ms.entity.service.XmeStorageServiceFacade;
import com.icthh.xm.ms.entity.service.storage.AvatarStorageResponse;
import com.icthh.xm.ms.entity.service.storage.AvatarStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class XmeStorageServiceFacadeImpl implements XmeStorageServiceFacade {

    private final StorageService storageService;
    private final AvatarStorageService avatarStorageService;
    private final AttachmentService attachmentService;

    public String store(HttpEntity<Resource> httpEntity, Integer size) {
        return storageService.store(httpEntity, size);
    }

    public AvatarStorageResponse getAvatarResource(XmEntity xmEntity) {
        return avatarStorageService.getAvatarResource(xmEntity);
    }

    public String storeAvatar(HttpEntity<Resource> httpEntity, Integer resizeSize) {
        return avatarStorageService.storeAvatar(httpEntity, resizeSize);
    }

    public Attachment save(Attachment attachment) {
        return attachmentService.save(attachment);
    }

}
