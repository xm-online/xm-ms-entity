package com.icthh.xm.ms.entity.service.storage;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.ms.entity.config.ApplicationProperties;
import com.icthh.xm.ms.entity.repository.backend.S3StorageRepository;
import com.icthh.xm.ms.entity.service.ContentService;
import com.icthh.xm.ms.entity.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;
import static com.icthh.xm.commons.exceptions.ErrorConstants.ERR_VALIDATION;

@Service
@RequiredArgsConstructor
public class StorageServiceImpl implements StorageService {

    private final S3StorageRepository s3StorageRepository;
    private final ContentService contentService;
    private final ApplicationProperties applicationProperties;

    public String store(HttpEntity<Resource> httpEntity, Integer size) {
        return s3StorageRepository.store(httpEntity, size);
    }

    public String storeAvatar(HttpEntity<Resource> httpEntity, Integer resizeSize) {
        final long contentSize = httpEntity.getHeaders().getContentLength();

        if (contentSize > applicationProperties.getObjectStorage().getAvatar().getMaxSize()) {
            throw new BusinessException(ERR_VALIDATION,
                "Avatar file must not exceed " + applicationProperties.getObjectStorage().getAvatar().getMaxSize() + " bytes [application.objectStorage.avatar.maxSize]");
        }

        return switch (applicationProperties.getObjectStorage().getAvatar().getStorageType()) {
            case S3 -> s3StorageRepository.store(httpEntity, (int) contentSize);
            case DB -> throw new RuntimeException("Not implemented");
            case FILE -> throw new RuntimeException("Not implemented");
        };

    }

}
