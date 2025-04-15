package com.icthh.xm.ms.entity.service;

import com.icthh.xm.ms.entity.util.XmHttpEntityUtils;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface StorageService {

    String store(HttpEntity<Resource> httpEntity, Integer size);
    String storeAvatar(HttpEntity<Resource> httpEntity, Integer resizeSize);

    default String store(MultipartFile file, Integer size) throws IOException {
        HttpEntity<Resource> httpResource = XmHttpEntityUtils.buildAvatarHttpEntity(file);
        return store(httpResource, size);
    }

    default String storeAvatar(MultipartFile file, Integer resizeSize) throws IOException {
        HttpEntity<Resource> httpResource = XmHttpEntityUtils.buildAvatarHttpEntity(file);
        return storeAvatar(httpResource, resizeSize);
    }

}
