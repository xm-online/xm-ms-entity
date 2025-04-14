package com.icthh.xm.ms.entity.service;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.web.multipart.MultipartFile;

public interface StorageService {
    String store(MultipartFile file, Integer size);
    String store(HttpEntity<Resource> httpEntity, Integer size);
}
