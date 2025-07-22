package com.icthh.xm.ms.entity.service.storage;

import com.icthh.xm.ms.entity.domain.XmEntity;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;

public interface AvatarStorageService {

    AvatarStorageResponse getAvatarResource(XmEntity xmEntity);
    String storeAvatar(HttpEntity<Resource> httpEntity, Integer resizeSize);

}
