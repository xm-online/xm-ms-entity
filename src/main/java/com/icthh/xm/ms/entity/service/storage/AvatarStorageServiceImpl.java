package com.icthh.xm.ms.entity.service.storage;

import com.icthh.xm.commons.config.client.service.TenantConfigService;
import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.ms.entity.config.ApplicationProperties;
import com.icthh.xm.ms.entity.domain.Content;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.repository.ContentRepository;
import com.icthh.xm.ms.entity.repository.backend.FsFileStorageRepository;
import com.icthh.xm.ms.entity.repository.backend.S3StorageRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.util.Objects;

import static com.icthh.xm.commons.exceptions.ErrorConstants.ERR_VALIDATION;
import static com.icthh.xm.ms.entity.config.Constants.*;
import static com.icthh.xm.ms.entity.web.rest.XmRestApiConstants.XM_HEADER_CONTENT_NAME;

@Service
@RequiredArgsConstructor
public class AvatarStorageServiceImpl implements AvatarStorageService {

    private final ContentRepository contentRepository;
    private final ApplicationProperties applicationProperties;
    private final TenantConfigService tenantConfigService;

    private final S3StorageRepository s3StorageRepository;
    private final FsFileStorageRepository fsFileStorageRepository;

    private Integer maxSize;
    private String dbFilePrefix;
    private ApplicationProperties.StorageType storageType;
    private ApplicationProperties.AvatarDefault avatarDefault;

    @PostConstruct
    public void init() {
        maxSize = applicationProperties.getObjectStorage().getMaxSize();
        dbFilePrefix = applicationProperties.getObjectStorage().getDbFilePrefix();
        storageType = applicationProperties.getObjectStorage().getStorageType();
        avatarDefault = applicationProperties.getAvatarDefault();
    }

    @Override
    public AvatarStorageResponse getAvatarResource(XmEntity xmEntity) {

        final String avatarUrl = xmEntity.getAvatarUrl();

        if (Boolean.TRUE.equals(xmEntity.isRemoved()) || avatarUrl == null) {
            String url = (String) tenantConfigService.getConfig()
                .getOrDefault("baseUrl", avatarDefault.getDefaultAvatarUrlPrefix());
            return AvatarStorageResponse.withRedirectUrl(URI.create(url + avatarDefault.getDefaultAvatarUrl()));
        }

        final String avatarRelatedUrl = xmEntity.getAvatarUrlRelative();

        return switch (storageType) {
            case DB -> AvatarStorageResponse.withResource(getResourceFromDB(avatarRelatedUrl), URI.create(avatarRelatedUrl));
            case S3 -> AvatarStorageResponse.withRedirectUrl(URI.create(avatarUrl));
            case FILE -> AvatarStorageResponse.withResource(getResourceFromFs(avatarRelatedUrl), URI.create(avatarUrl));
        };
    }

    public String storeAvatar(HttpEntity<Resource> httpEntity, Integer resizeSize) {
        final long contentSize = httpEntity.getHeaders().getContentLength();

        if (contentSize > maxSize) {
            throw new BusinessException(ERR_VALIDATION,
                "Avatar file must not exceed " + maxSize + " bytes [application.objectStorage.avatar.maxSize]");
        }

        return switch (storageType) {
            case S3 -> s3StorageRepository.store(httpEntity, (int) contentSize);
            case DB -> dbStoreStrategy(httpEntity);
            case FILE -> fsFileStorageRepository.store(httpEntity, (int) contentSize);
        };

    }

    @SneakyThrows(value = IOException.class)
    private String dbStoreStrategy(HttpEntity<Resource> httpEntity) {
        byte[] data = Objects.requireNonNull(httpEntity.getBody()).getContentAsByteArray();
        Content content = new Content();
        content.setValue(data);
        content = contentRepository.save(content);
        return dbFilePrefix + content.getId() + "/" + httpEntity.getHeaders().getFirst(XM_HEADER_CONTENT_NAME);
    }

    private Resource getResourceFromFs(String fileName) {
        String noPrefix = fileName.replace(FILE_PREFIX, "");
        return fsFileStorageRepository.getFileFromFs(noPrefix);
    }

    private Resource getResourceFromDB(String fileName) {
        String noPrefix = fileName.replace(dbFilePrefix, "");
        String[] tokens = StringUtils.split(noPrefix, "/");
        Long contentId = Long.parseLong(tokens[0]);
        Content content = contentRepository.findResourceById(contentId);
        return new ByteArrayResource(content.getValue());
    }

}
