package com.icthh.xm.ms.entity.repository.backend;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.tenant.TenantContext;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantKey;
import com.icthh.xm.ms.entity.AbstractJupiterUnitTest;
import com.icthh.xm.ms.entity.config.ApplicationProperties;
import com.icthh.xm.ms.entity.config.amazon.AmazonS3Template;
import java.io.IOException;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;

public class S3StorageRepositoryUnitTest extends AbstractJupiterUnitTest {

    private S3StorageRepository s3StorageRepository;
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
    private TenantContextHolder tenantContextHolder;

    @BeforeEach
    public void before() {
        MockitoAnnotations.initMocks(this);
        s3StorageRepository = new S3StorageRepository(applicationProperties, amazonS3Template, tenantContextHolder);
    }

    @Test
    public void testStore() throws IOException {
        when(applicationProperties.getAmazon()).thenReturn(amazon);
        when(amazon.getAws()).thenReturn(aws);
        when(aws.getTemplate()).thenReturn("template");
        when(amazon.getS3()).thenReturn(s3);
        when(s3.getBucket()).thenReturn("bucket");
        s3StorageRepository.store(new MockMultipartFile("test.jpg", "mytest.jpg", "application/json", "trulala".getBytes()), 7);

        verify(applicationProperties, times(2)).getAmazon();
        verify(amazonS3Template).save(any(), any());
        verifyNoMoreInteractions(applicationProperties, amazonS3Template);
    }

    @Test
    public void shouldReadAndDeleteAnyBucketWhenTenantAccessCheckIsOff() {
        when(applicationProperties.isSecureAttachmentTenantAccess()).thenReturn(false);
        s3StorageRepository.getS3Object("prefix-other::folder/key");
        s3StorageRepository.delete("prefix-other::folder/key");
        verify(amazonS3Template).getS3Object("prefix-other", "folder/key");
        verify(amazonS3Template).delete("prefix-other", "folder/key");
        verify(amazonS3Template, never()).getBucketName(any(), any());
    }

    @Test
    public void shouldReadAndDeleteTenantBucketWhenTenantAccessCheckIsOn() {
        mockTenantBucket();
        s3StorageRepository.getS3Object("prefix-test::folder/key");
        s3StorageRepository.delete("prefix-test::folder/key");
        verify(amazonS3Template).getS3Object("prefix-test", "folder/key");
        verify(amazonS3Template).delete("prefix-test", "folder/key");
    }

    @Test
    public void shouldRejectOtherTenantBucketWhenTenantAccessCheckIsOn() {
        mockTenantBucket();
        assertThrows(BusinessException.class, () -> s3StorageRepository.getS3Object("prefix-other::folder/key"));
        assertThrows(BusinessException.class, () -> s3StorageRepository.delete("prefix-other::folder/key"));
        verify(amazonS3Template, never()).getS3Object(any(), any());
        verify(amazonS3Template, never()).delete(any(), any());
    }

    private void mockTenantBucket() {
        when(applicationProperties.isSecureAttachmentTenantAccess()).thenReturn(true);
        TenantContext tenantContext = org.mockito.Mockito.mock(TenantContext.class);
        when(tenantContextHolder.getContext()).thenReturn(tenantContext);
        when(tenantContext.getTenantKey()).thenReturn(Optional.of(TenantKey.valueOf("TEST")));
        when(applicationProperties.getAmazon()).thenReturn(amazon);
        when(amazon.getS3()).thenReturn(s3);
        when(s3.getBucketPrefix()).thenReturn("prefix");
        when(amazonS3Template.getBucketName("prefix", "TEST")).thenReturn("prefix-test");
    }
}
