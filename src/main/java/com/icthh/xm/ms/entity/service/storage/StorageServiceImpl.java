package com.icthh.xm.ms.entity.service.storage;

import com.icthh.xm.ms.entity.config.ApplicationProperties;
import com.icthh.xm.ms.entity.repository.backend.S3StorageRepository;
import com.icthh.xm.ms.entity.service.StorageService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StorageServiceImpl implements StorageService {

    private final S3StorageRepository s3StorageRepository;
    private final ApplicationProperties applicationProperties;

    private ApplicationProperties.StorageType storageType;

    @PostConstruct
    public void init() {
        storageType = applicationProperties.getObjectStorage().getAvatar().getStorageType();
    }

    public String store(HttpEntity<Resource> httpEntity, Integer size) {
        if (ApplicationProperties.StorageType.S3.equals(storageType)) {
            return s3StorageRepository.store(httpEntity, size);
        }
        throw new RuntimeException("Not implemented " + storageType);
    }

}
