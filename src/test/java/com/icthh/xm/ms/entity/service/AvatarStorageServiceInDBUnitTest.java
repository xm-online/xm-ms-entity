package com.icthh.xm.ms.entity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.icthh.xm.commons.config.client.service.TenantConfigService;
import com.icthh.xm.ms.entity.AbstractJupiterUnitTest;
import com.icthh.xm.ms.entity.config.ApplicationProperties;
import com.icthh.xm.ms.entity.domain.Content;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.repository.ContentRepository;
import com.icthh.xm.ms.entity.repository.backend.FsFileStorageRepository;
import com.icthh.xm.ms.entity.repository.backend.S3StorageRepository;
import com.icthh.xm.ms.entity.service.storage.AvatarStorageResponse;
import com.icthh.xm.ms.entity.service.storage.AvatarStorageServiceImpl;
import com.icthh.xm.ms.entity.util.EntityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.URI;
import java.util.Map;


@ExtendWith(MockitoExtension.class)
public class AvatarStorageServiceInDBUnitTest extends AbstractJupiterUnitTest {

    private static final String DB_PREFIX = "db://xme/entity/obj";

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
    private FsFileStorageRepository fsFileStorageRepository;


    @BeforeEach
    void setUp() {
        lenient().when(applicationProperties.getObjectStorage()).thenReturn(objectStorage);
        lenient().when(tenantConfigService.getConfig()).thenReturn(Map.of("baseUrl", "http://tst"));
        lenient().when(objectStorage.getStorageType()).thenReturn(ApplicationProperties.StorageType.DB);
        lenient().when(objectStorage.getDbFilePrefix()).thenReturn(DB_PREFIX);
        avatarStorageService = new AvatarStorageServiceImpl(contentRepository, applicationProperties, tenantConfigService, s3StorageRepository, fsFileStorageRepository);
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
    public void shouldGetAvatarResourceFromDbStorage() throws IOException {
        // given
        String dbFileUrl = "123";
        byte[] contentBytes = "test content".getBytes();
        Content content = new Content();
        content.setValue(contentBytes);

        when(contentRepository.findResourceById(123L)).thenReturn(content);

        XmEntity xmEntity = EntityUtils.newEntity(entity -> {
            entity.setRemoved(false);
            entity.avatarUrl("db://" + dbFileUrl);
            entity.setAvatarUrlRelative(dbFileUrl);
        });

        // when
        AvatarStorageResponse response = avatarStorageService.getAvatarResource(xmEntity);

        // then
        assertThat(response.avatarResource()).isNotNull();
        assertThat(response.uri()).isEqualTo(URI.create(dbFileUrl));
        assertThat(response.avatarResource().exists()).isTrue();
        assertThat(response.avatarResource().contentLength()).isGreaterThan(0);
    }

    @Test
    public void shouldGetAvatarResourceAWS() {
        String dbFileName = "wer-ert-rty";
        String dbFileUrl = "https://aws/wer-ert-rty";

        when(objectStorage.getStorageType()).thenReturn(ApplicationProperties.StorageType.S3);
        avatarStorageService = new AvatarStorageServiceImpl(contentRepository, applicationProperties, tenantConfigService, s3StorageRepository, fsFileStorageRepository);
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

    @Test
    public void shouldReturnAvatarResourceFromDbStorage() {
        String dbFileName = "/123/test.jpg";

        XmEntity xmEntity = EntityUtils.newEntity(entity -> {
            entity.setRemoved(false);
            entity.avatarUrl(DB_PREFIX + dbFileName);
            entity.setAvatarUrlRelative(dbFileName);
        });

        Content content = new Content();
        content.setValue("test content".getBytes());
        when(contentRepository.findResourceById(123L)).thenReturn(content);

        AvatarStorageResponse response = avatarStorageService.getAvatarResource(xmEntity);
        assertThat(response.avatarResource()).isNotNull();
        assertThat(response.uri()).isEqualTo(URI.create(dbFileName));
    }


}
