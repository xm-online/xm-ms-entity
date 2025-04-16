package com.icthh.xm.ms.entity.service.storage;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.ms.entity.config.ApplicationProperties;
import com.icthh.xm.ms.entity.domain.Content;
import com.icthh.xm.ms.entity.repository.ContentRepository;
import com.icthh.xm.ms.entity.repository.backend.S3StorageRepository;
import com.icthh.xm.ms.entity.service.StorageService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Objects;

import static com.icthh.xm.commons.exceptions.ErrorConstants.ERR_VALIDATION;
import static com.icthh.xm.ms.entity.web.rest.XmRestApiConstants.XM_HEADER_CONTENT_NAME;

@Service
@RequiredArgsConstructor
public class StorageServiceImpl implements StorageService {

    private final S3StorageRepository s3StorageRepository;
    private final ContentRepository contentRepository;
    private final ApplicationProperties applicationProperties;

    private Integer maxSize;
    private String dbFilePrefix;

    @PostConstruct
    public void init() {
        maxSize = applicationProperties.getObjectStorage().getAvatar().getMaxSize();
        dbFilePrefix = applicationProperties.getObjectStorage().getAvatar().getDbFilePrefix();
    }

    public String store(HttpEntity<Resource> httpEntity, Integer size) {
        return s3StorageRepository.store(httpEntity, size);
    }

    public String storeAvatar(HttpEntity<Resource> httpEntity, Integer resizeSize) throws IOException {
        final long contentSize = httpEntity.getHeaders().getContentLength();

        if (contentSize > maxSize) {
            throw new BusinessException(ERR_VALIDATION,
                "Avatar file must not exceed " + maxSize + " bytes [application.objectStorage.avatar.maxSize]");
        }

        return switch (applicationProperties.getObjectStorage().getAvatar().getStorageType()) {
            case S3 -> s3StorageRepository.store(httpEntity, (int) contentSize);
            case DB -> dbStoreStrategy(httpEntity);
            case FILE -> throw new RuntimeException("Not implemented");
        };

    }

    private String dbStoreStrategy(HttpEntity<Resource> httpEntity) throws IOException {
        Content content = new Content();
        content.setValue(Objects.requireNonNull(httpEntity.getBody()).getContentAsByteArray());
        content = contentRepository.save(content);
        return dbFilePrefix + content.getId() + "-" + httpEntity.getHeaders().getFirst(XM_HEADER_CONTENT_NAME);
    }

}
