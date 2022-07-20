package com.icthh.xm.ms.entity.service.impl;

import com.icthh.xm.commons.permission.repository.PermittedRepository;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.ms.entity.AbstractSpringBootTest;
import com.icthh.xm.ms.entity.config.ApplicationProperties;
import com.icthh.xm.ms.entity.config.amazon.AmazonS3Template;
import com.icthh.xm.ms.entity.domain.Attachment;
import com.icthh.xm.ms.entity.domain.AttachmentStoreType;
import com.icthh.xm.ms.entity.domain.Content;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.spec.AttachmentSpec;
import com.icthh.xm.ms.entity.repository.ContentRepository;
import com.icthh.xm.ms.entity.repository.backend.S3StorageRepository;
import com.icthh.xm.ms.entity.service.ContentService;
import com.icthh.xm.ms.entity.service.dto.UploadResultDto;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.net.MalformedURLException;
import java.net.URL;

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_AUTH_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@Slf4j
public class ContentServiceIntTest extends AbstractSpringBootTest {

    private static final String PREFIX = "prefix";
    private static final String TENANT_KEY = "RESINTTEST";

    private ContentService contentService;
    private S3StorageRepository s3StorageRepository;

    @Autowired
    private PermittedRepository permittedRepository;

    @Autowired
    private ContentRepository contentRepository;

    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Autowired
    private XmAuthenticationContextHolder authContextHolder;

    @Autowired
    private LepManager lepManager;

    @Autowired
    private ApplicationProperties applicationProperties;

    @Mock
    private AmazonS3Template amazonS3Template;

    @BeforeTransaction
    public void beforeTransaction() {
        TenantContextUtils.setTenant(tenantContextHolder, TENANT_KEY);
    }

    @SneakyThrows
    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        lepManager.beginThreadContext(ctx -> {
            ctx.setValue(THREAD_CONTEXT_KEY_TENANT_CONTEXT, tenantContextHolder.getContext());
            ctx.setValue(THREAD_CONTEXT_KEY_AUTH_CONTEXT, authContextHolder.getContext());
        });

        enrichAmazonProperties(applicationProperties.getAmazon());

        s3StorageRepository = new S3StorageRepository(applicationProperties, amazonS3Template, tenantContextHolder);
        contentService = new ContentService(permittedRepository, contentRepository, s3StorageRepository);
    }

    private void enrichAmazonProperties(ApplicationProperties.Amazon amazon) {
        amazon.getS3().setBucketPrefix(PREFIX);
        amazon.getAws().setExpireLinkTimeInMillis(10000L);
    }

    @After
    public void tearDown() {
        lepManager.endThreadContext();
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
    }


    @Test
    @Transactional
    public void shouldSaveContentInDb() {
        AttachmentSpec attachmentSpec = new AttachmentSpec();
        Attachment attachment = new Attachment();
        Content content = new Content();
        content.setValue("A".getBytes());

        Attachment save = contentService.save(attachmentSpec, attachment, content);

        assertThat(save.getContent().getId()).isNotNull();
        assertThat(save.getContentChecksum()).isEqualTo(DigestUtils.sha256Hex(content.getValue()));
        assertThat(save.getValueContentSize()).isEqualTo(content.getValue().length);
    }

    @Test
    @Transactional
    public void shouldSaveContentInAws() {
        XmEntity xmEntity = new XmEntity();
        xmEntity.setTypeKey("T");

        AttachmentSpec attachmentSpec = new AttachmentSpec();
        attachmentSpec.setStoreType(AttachmentStoreType.S3);

        Attachment attachment = new Attachment();
        attachment.setName("test.doc");
        attachment.setXmEntity(xmEntity);

        Content content = new Content();
        content.setValue("A".getBytes());

        String bucketName = prepareBucketName();
        Mockito.when(amazonS3Template.createBucketIfNotExist(PREFIX, TENANT_KEY)).thenReturn(bucketName);
        Mockito.when(amazonS3Template.save(eq(bucketName), any(), any(), eq(content.getValue().length),
            eq(attachment.getName()))).thenReturn(new UploadResultDto(bucketName, "someFileKey", "etag"));

        Attachment save = contentService.save(attachmentSpec, attachment, content);
        assertThat(save.getContentUrl()).isNotBlank();
        assertThat(save.getContentUrl()).isEqualTo(bucketName + "::someFileKey");
    }

    @Test
    @Transactional
    public void shouldReturnLinkFromAws() throws MalformedURLException {
        AttachmentSpec attachmentSpec = new AttachmentSpec();
        attachmentSpec.setStoreType(AttachmentStoreType.S3);

        String fileName = "fileName";
        String bucketName = prepareBucketName();
        String contentUrl = bucketName + "::" + fileName;
        Attachment attachment = new Attachment();
        attachment.setContentUrl(contentUrl);

        String contentPathUrl = "http://localhost:8090/" + bucketName + "/fileName";
        Mockito.when(amazonS3Template.createExpirableLink(eq(bucketName), eq(fileName),
            eq(applicationProperties.getAmazon().getAws().getExpireLinkTimeInMillis()))).thenReturn(new URL(contentPathUrl));

        Attachment result = contentService.enrichContent(attachmentSpec, attachment);

        assertThat(result.getContentUrl()).isNotBlank();
        assertThat(result.getContentUrl()).isEqualTo(contentPathUrl);
    }

    private String prepareBucketName() {
        return PREFIX + "-" + TENANT_KEY.toLowerCase().replace("_", "-");
    }

}
