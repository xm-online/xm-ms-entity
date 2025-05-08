package com.icthh.xm.ms.entity.service.storage;

import com.icthh.xm.ms.entity.config.ApplicationProperties;
import com.icthh.xm.ms.entity.domain.Content;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.repository.ContentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class AvatarStorageServiceImpl implements AvatarStorageService {

    private final ContentRepository contentRepository;
    private final ApplicationProperties applicationProperties;

    @Override
    public AvatarStorageResponse getAvatarResource(XmEntity xmEntity) {
        final String avatarUrl = xmEntity.getAvatarUrl();
        final String avatarFileUrl = xmEntity.getAvatarUrlRelative();
        if (xmEntity.isRemoved() || xmEntity.getAvatarUrl() == null) {
            throw new RuntimeException("Here should transfer to default url in webapp");
        }

        ApplicationProperties.StorageType storageType = applicationProperties.getObjectStorage().getAvatar().getStorageType();

        return switch (storageType) {
            case DB -> AvatarStorageResponse.withResource(getResourceFromDB(avatarFileUrl), URI.create(avatarFileUrl));
            case S3 -> AvatarStorageResponse.withRedirectUrl(URI.create(avatarUrl));
            default -> throw new RuntimeException("Unsupported storage type: " + storageType);
        };
    }

    private Resource getResourceFromDB(String fileName) {
        long contentId = Long.parseLong(fileName);
        Content content = contentRepository.findResourceById(contentId);
        return new ByteArrayResource(content.getValue());
    }

}
