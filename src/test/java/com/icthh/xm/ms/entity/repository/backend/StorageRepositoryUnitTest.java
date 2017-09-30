package com.icthh.xm.ms.entity.repository.backend;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.icthh.xm.ms.entity.config.ApplicationProperties;
import com.icthh.xm.ms.entity.config.amazon.AmazonS3Template;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;

public class StorageRepositoryUnitTest {

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

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        storageRepository = new StorageRepository(applicationProperties, amazonS3Template);
    }

    @Test
    public void testStore() throws IOException {
        when(applicationProperties.getAmazon()).thenReturn(amazon);
        when(amazon.getAws()).thenReturn(aws);
        when(aws.getTemplate()).thenReturn("template");
        when(amazon.getS3()).thenReturn(s3);
        when(s3.getBucket()).thenReturn("bucket");
        storageRepository.store(new MockMultipartFile("test.jpg", "mytest.jpg", "application/json", "trulala".getBytes()), 7);

        verify(applicationProperties, times(2)).getAmazon();
        verify(amazonS3Template).save(any(), any());
        verifyNoMoreInteractions(applicationProperties, amazonS3Template);
    }
}
