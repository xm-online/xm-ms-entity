package com.icthh.xm.ms.entity.service.storage;

import com.icthh.xm.ms.entity.repository.backend.S3StorageRepository;
import com.icthh.xm.ms.entity.service.ContentService;
import com.icthh.xm.ms.entity.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class StorageServiceImpl implements StorageService {

    private final S3StorageRepository s3StorageRepository;
    private final ContentService contentService;

    public String store(MultipartFile file, Integer size) {
        return s3StorageRepository.store(file, size);
    }

    public String store(HttpEntity<Resource> httpEntity, Integer size) {
        return s3StorageRepository.store(httpEntity, size);
    }
}
