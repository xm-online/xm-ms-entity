package com.icthh.xm.ms.entity.service.storage;

import com.icthh.xm.ms.entity.config.ApplicationProperties;
import com.icthh.xm.ms.entity.config.Constants;
import com.icthh.xm.ms.entity.domain.Content;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.repository.ContentRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.net.URI;

@Service
@RequiredArgsConstructor
public class AvatarStorageServiceImpl implements AvatarStorageService {

    private final AvatarStorageResponse DEFAULT_AVATAR_URL = AvatarStorageResponse.withRedirectUrl(URI.create(Constants.DEFAULT_AVATAR_URL));

    private final ContentRepository contentRepository;
    private final ApplicationProperties applicationProperties;

    @Override
    public AvatarStorageResponse getAvatarResource(XmEntity xmEntity) {
        final String avatarUrl = xmEntity.getAvatarUrl();
        final String avatarFileUrl = xmEntity.getAvatarUrlRelative();

        if (Boolean.TRUE.equals(xmEntity.isRemoved()) || xmEntity.getAvatarUrl() == null) {
            return DEFAULT_AVATAR_URL;
        }

        ApplicationProperties.StorageType storageType = applicationProperties.getObjectStorage().getAvatar().getStorageType();

        return switch (storageType) {
            case DB -> AvatarStorageResponse.withResource(getResourceFromDB(avatarFileUrl), URI.create(avatarFileUrl));
            case S3 -> AvatarStorageResponse.withRedirectUrl(URI.create(avatarUrl));
            default -> throw new RuntimeException("Unsupported storage type: " + storageType);
        };
    }

    private Resource getResourceFromDB(String fileName) {
        String noPrefix = fileName.replace(applicationProperties.getObjectStorage().getAvatar().getDbFilePrefix(), "");
        String[] tokens = StringUtils.split(noPrefix, "-");
        long contentId = Long.parseLong(tokens[0]);
        Content content = contentRepository.findResourceById(contentId);
        return new ByteArrayResource(content.getValue());
    }

}
