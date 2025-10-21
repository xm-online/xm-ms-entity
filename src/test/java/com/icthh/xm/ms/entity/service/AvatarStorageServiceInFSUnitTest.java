package com.icthh.xm.ms.entity.service;

import com.icthh.xm.commons.config.client.service.TenantConfigService;
import com.icthh.xm.ms.entity.AbstractJupiterUnitTest;
import com.icthh.xm.ms.entity.config.ApplicationProperties;
import com.icthh.xm.ms.entity.domain.Content;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.repository.ContentRepository;
import com.icthh.xm.ms.entity.repository.backend.FileStorageRepository;
import com.icthh.xm.ms.entity.repository.backend.S3StorageRepository;
import com.icthh.xm.ms.entity.service.storage.AvatarStorageResponse;
import com.icthh.xm.ms.entity.service.storage.AvatarStorageServiceImpl;
import com.icthh.xm.ms.entity.util.EntityUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
public class AvatarStorageServiceInFSUnitTest extends AbstractJupiterUnitTest {

    private static final String FILE_PREFIX = "./";

    public AvatarStorageServiceImpl avatarStorageService;

    @Mock
    private ContentRepository contentRepository;

    @Mock
    private ApplicationProperties applicationProperties;

    @Mock
    private TenantConfigService tenantConfigService;

    @Mock
    private ApplicationProperties.ObjectStorage objectStorage;

    @Mock
    private S3StorageRepository s3StorageRepository;

    @Mock
    private FileStorageRepository fileStorageRepository;


    @BeforeEach
    void setUp() {
        lenient().when(applicationProperties.getObjectStorage()).thenReturn(objectStorage);
        lenient().when(tenantConfigService.getConfig()).thenReturn(Map.of("baseUrl", "http://tst"));
        lenient().when(objectStorage.getStorageType()).thenReturn(ApplicationProperties.StorageType.FILE);
        lenient().when(objectStorage.getFileRoot()).thenReturn(FILE_PREFIX);
        avatarStorageService = new AvatarStorageServiceImpl(contentRepository, applicationProperties, tenantConfigService, s3StorageRepository, fileStorageRepository);
        avatarStorageService.init();
    }

    @Test
    public void shouldReturnDefaultAvatarIfEntityIsRemoved() {
        XmEntity xmEntity = EntityUtils.newEntity(entity -> {
            entity.setRemoved(true);
        });

        AvatarStorageResponse avatarResource = avatarStorageService.getAvatarResource(xmEntity);
        assertThat(avatarResource).isNotNull();
        assertThat(avatarResource.uri().toString()).isEqualTo("http://tst/assets/img/anonymous.png");
    }

    @Test
    public void shouldReturnDefaultAvatarIfAvatarUrlIsMissing() {
        XmEntity xmEntity = EntityUtils.newEntity(entity -> {
            entity.setRemoved(false);
        });

        avatarStorageService.getAvatarResource(xmEntity);

        AvatarStorageResponse avatarResource = avatarStorageService.getAvatarResource(xmEntity);
        assertThat(avatarResource).isNotNull();
        assertThat(avatarResource.avatarResource()).isNull();
        assertThat(avatarResource.uri().toString()).isEqualTo("http://tst/assets/img/anonymous.png");
    }

    @Test
    public void shouldGetAvatarResourceFromFileStorage() throws IOException {
        // given
        String fileUrl = "123";

        XmEntity xmEntity = EntityUtils.newEntity(entity -> {
            entity.setRemoved(false);
            entity.avatarUrl("file://" + fileUrl);
            entity.setAvatarUrlRelative("file://" + fileUrl);
        });

        // when
        AvatarStorageResponse response = avatarStorageService.getAvatarResource(xmEntity);

        // then
        assertThat(response.uri()).isEqualTo(URI.create("file://" + fileUrl));
    }

    @Test
    public void shouldGetAvatarResourceAWS() {
        String dbFileName = "wer-ert-rty";
        String dbFileUrl = "file://aws/wer-ert-rty";

        when(objectStorage.getStorageType()).thenReturn(ApplicationProperties.StorageType.FILE);
        avatarStorageService = new AvatarStorageServiceImpl(contentRepository, applicationProperties, tenantConfigService, s3StorageRepository, fileStorageRepository);
        avatarStorageService.init();

        XmEntity xmEntity = EntityUtils.newEntity(entity -> {
            entity.setRemoved(false);
            entity.avatarUrl(dbFileUrl);
            entity.setAvatarUrlRelative(dbFileName);
        });

        // when
        AvatarStorageResponse response = avatarStorageService.getAvatarResource(xmEntity);

        // then
        assertThat(response.avatarResource()).isNull();
        assertThat(response.uri()).isEqualTo(URI.create(dbFileUrl));
    }

}
