package com.icthh.xm.ms.entity.repository.backend;

import com.icthh.xm.ms.entity.AbstractUnitTest;
import com.icthh.xm.ms.entity.config.ApplicationProperties;
import com.icthh.xm.ms.entity.config.amazon.AmazonS3Template;
import com.icthh.xm.ms.entity.service.storage.file.FileStorageService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;

import static com.icthh.xm.ms.entity.config.ApplicationProperties.FileStorage.SupportedFileStorageType.AWS;
import static com.icthh.xm.ms.entity.config.ApplicationProperties.FileStorage.SupportedFileStorageType.MINIO;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class StorageRepositoryUnitTest extends AbstractUnitTest {
    private StorageRepository storageRepository;

    @Mock
    private ApplicationProperties applicationProperties;
    @Mock
    private AmazonS3Template amazonS3Template;
    @Mock
    private ApplicationProperties.Amazon amazon;
    @Mock
    private ApplicationProperties.Amazon.Aws aws;
    @Mock
    private ApplicationProperties.Amazon.S3 s3;
    @Mock
    private ApplicationProperties.FileStorage fileStorage;
    @Mock
    private ApplicationProperties.FileStorage.Minio minio;

    @Mock
    private FileStorageService fileStorageService;


    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        storageRepository = new StorageRepository(applicationProperties, fileStorageService, amazonS3Template);

        when(applicationProperties.getFileStorage()).thenReturn(fileStorage);
    }

    @Test
    public void testAwsStore() throws IOException {
        when(fileStorage.getActiveType()).thenReturn(AWS);
        when(applicationProperties.getAmazon()).thenReturn(amazon);
        when(amazon.getAws()).thenReturn(aws);
        when(aws.getTemplate()).thenReturn("template");
        when(amazon.getS3()).thenReturn(s3);
        when(s3.getBucket()).thenReturn("bucket");

        storageRepository.store(new MockMultipartFile("test.jpg", "mytest.jpg", "application/json", "trulala".getBytes()), 7);

        verify(applicationProperties, times(2)).getAmazon();
        verify(amazonS3Template).save(any(), any());
    }

    @Test
    public void testMinioStore() {
        when(fileStorage.getActiveType()).thenReturn(MINIO);
        when(fileStorage.getMinio()).thenReturn(minio);
        when(minio.getTemplate()).thenReturn("template");
        when(minio.getBucket()).thenReturn("bucket");

        storageRepository.store(new MockMultipartFile("test.jpg", "mytest.jpg", "application/json", "trulala".getBytes()), 7);

        verify(applicationProperties, times(4)).getFileStorage();
        verify(fileStorageService).save(any(), any());
    }
}
