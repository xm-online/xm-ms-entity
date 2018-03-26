package com.icthh.xm.ms.entity.service;

import com.icthh.xm.ms.entity.repository.backend.StorageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
@RequiredArgsConstructor
public class StorageService {

    private final StorageRepository storageRepository;

    public String store(MultipartFile file, Integer size) {
        return storageRepository.store(file, size);
    }

    public String store(HttpEntity<Resource> httpEntity, Integer size) {
        return storageRepository.store(httpEntity, size);
    }
}
