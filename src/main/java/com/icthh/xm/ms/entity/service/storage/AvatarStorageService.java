package com.icthh.xm.ms.entity.service.storage;

import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.util.XmHttpEntityUtils;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface AvatarStorageService {

    AvatarStorageResponse getAvatarResource(XmEntity xmEntity);
    String storeAvatar(HttpEntity<Resource> httpEntity, Integer resizeSize);

    default String storeAvatar(MultipartFile file, Integer resizeSize) throws IOException {
        HttpEntity<Resource> httpResource = XmHttpEntityUtils.buildAvatarHttpEntity(file);
        return storeAvatar(httpResource, resizeSize);
    }

}
