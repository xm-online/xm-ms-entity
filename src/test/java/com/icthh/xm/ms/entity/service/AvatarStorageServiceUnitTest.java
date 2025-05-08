package com.icthh.xm.ms.entity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.icthh.xm.ms.entity.AbstractJupiterUnitTest;
import com.icthh.xm.ms.entity.config.ApplicationProperties;
import com.icthh.xm.ms.entity.domain.Content;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.repository.ContentRepository;
import com.icthh.xm.ms.entity.service.storage.AvatarStorageResponse;
import com.icthh.xm.ms.entity.service.storage.AvatarStorageService;
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


@ExtendWith(MockitoExtension.class)
public class AvatarStorageServiceUnitTest extends AbstractJupiterUnitTest {

    public AvatarStorageService avatarStorageService;

    @Mock
    private ContentRepository contentRepository;

    @Mock
    private ApplicationProperties applicationProperties;

    @Mock
    private ApplicationProperties.AvatarStorage avatar;


    @Mock
    private ApplicationProperties.ObjectStorage objectStorage;

    @BeforeEach
    void setUp() {
        lenient().when(applicationProperties.getObjectStorage()).thenReturn(objectStorage);
        lenient().when(objectStorage.getAvatar()).thenReturn(avatar);
        avatarStorageService = new AvatarStorageServiceImpl(contentRepository, applicationProperties);
    }

    @Test
    public void shouldThrowExceptionIfFileISRemoved() {
        XmEntity xmEntity = EntityUtils.newEntity(entity -> {
            entity.setRemoved(true);
        });

        RuntimeException thrown = Assertions.assertThrows(RuntimeException.class, () -> {
            avatarStorageService.getAvatarResource(xmEntity);
        }, "Here should transfer to default url in webapp");

        Assertions.assertEquals("Here should transfer to default url in webapp", thrown.getMessage());
    }

    @Test
    public void shouldThrowExceptionIfFileUrlIsMissing() throws IOException {
        XmEntity xmEntity = EntityUtils.newEntity(entity -> {
            entity.setRemoved(false);
        });

        RuntimeException thrown = Assertions.assertThrows(RuntimeException.class, () -> {
            avatarStorageService.getAvatarResource(xmEntity);
        }, "Here should transfer to default url in webapp");

        Assertions.assertEquals("Here should transfer to default url in webapp", thrown.getMessage());
    }

    @Test
    public void shouldGetAvatarResourceFromDbStorage() throws IOException {
        // given
        String dbFileUrl = "123";
        byte[] contentBytes = "test content".getBytes();
        Content content = new Content();
        content.setValue(contentBytes);

        when(avatar.getStorageType()).thenReturn(ApplicationProperties.StorageType.DB);
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

        when(avatar.getStorageType()).thenReturn(ApplicationProperties.StorageType.S3);

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
    public void shouldThrowExceptionIfStorageTypeIsFS() {
        String dbFileName = "wer-ert-rty";
        String dbFileUrl = "file://wer-ert-rty";

        XmEntity xmEntity = EntityUtils.newEntity(entity -> {
            entity.setRemoved(false);
            entity.avatarUrl(dbFileUrl);
            entity.setAvatarUrlRelative(dbFileName);
        });

        when(avatar.getStorageType()).thenReturn(ApplicationProperties.StorageType.FILE);
        RuntimeException thrown = Assertions.assertThrows(RuntimeException.class, () -> {
            avatarStorageService.getAvatarResource(xmEntity);
        }, "Not implemented");

        Assertions.assertEquals("Unsupported storage type: " + ApplicationProperties.StorageType.FILE, thrown.getMessage());
    }

}
