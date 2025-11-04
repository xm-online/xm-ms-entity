package com.icthh.xm.ms.entity.service;

import com.icthh.xm.ms.entity.domain.Attachment;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.service.storage.AvatarStorageResponse;
import com.icthh.xm.ms.entity.util.XmHttpEntityUtils;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface XmeStorageServiceFacade {

    String store(HttpEntity<Resource> httpEntity, Integer size);

    AvatarStorageResponse getAvatarResource(XmEntity xmEntity);
    String storeAvatar(HttpEntity<Resource> httpEntity, Integer resizeSize);

    Attachment save(Attachment attachment);

    default String store(MultipartFile file, Integer size) throws IOException {
        HttpEntity<Resource> httpResource = XmHttpEntityUtils.buildAvatarHttpEntity(file);
        return store(httpResource, size);
    }

}
