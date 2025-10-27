package com.icthh.xm.ms.entity.service.impl;

import com.icthh.xm.commons.permission.repository.PermittedRepository;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.ms.entity.AbstractJupiterSpringBootTest;
import com.icthh.xm.ms.entity.config.ApplicationProperties;
import com.icthh.xm.ms.entity.config.amazon.AmazonS3Template;
import com.icthh.xm.ms.entity.domain.Attachment;
import com.icthh.xm.ms.entity.domain.AttachmentStoreType;
import com.icthh.xm.ms.entity.domain.Content;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.spec.AttachmentSpec;
import com.icthh.xm.ms.entity.repository.ContentRepository;
import com.icthh.xm.ms.entity.repository.backend.FsFileStorageRepository;
import com.icthh.xm.ms.entity.repository.backend.S3StorageRepository;
import com.icthh.xm.ms.entity.service.ContentService;
import com.icthh.xm.ms.entity.service.XmEntitySpecService;
import com.icthh.xm.ms.entity.service.dto.UploadResultDto;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_AUTH_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;

@Slf4j
public class ContentServiceIntTest extends AbstractJupiterSpringBootTest {

    private static final String PREFIX = "prefix";
    private static final String TENANT_KEY = "RESINTTEST";

    private ContentService contentService;

    private S3StorageRepository s3StorageRepository;
    @Mock
    private FsFileStorageRepository fsFileStorageRepository;

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

    @Mock
    private XmEntitySpecService xmEntitySpecService;

    @BeforeTransaction
    public void beforeTransaction() {
        TenantContextUtils.setTenant(tenantContextHolder, TENANT_KEY);
    }

    @SneakyThrows
    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);

        lepManager.beginThreadContext(ctx -> {
            ctx.setValue(THREAD_CONTEXT_KEY_TENANT_CONTEXT, tenantContextHolder.getContext());
            ctx.setValue(THREAD_CONTEXT_KEY_AUTH_CONTEXT, authContextHolder.getContext());
        });

        enrichAmazonProperties(applicationProperties.getAmazon());

        s3StorageRepository = new S3StorageRepository(applicationProperties, amazonS3Template, tenantContextHolder);
        contentService = new ContentService(permittedRepository, contentRepository, s3StorageRepository, fsFileStorageRepository, xmEntitySpecService);
    }

    private void enrichAmazonProperties(ApplicationProperties.Amazon amazon) {
        amazon.getS3().setBucketPrefix(PREFIX);
    }

    @AfterEach
    public void tearDown() {
        lepManager.endThreadContext();
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
    }


    @Test
    @Transactional
    public void shouldSaveContentInDb() {
        AttachmentSpec attachmentSpec = new AttachmentSpec();

        XmEntity xmEntity = new XmEntity();
        xmEntity.setTypeKey("T");

        Attachment attachment = new Attachment();
        attachment.setXmEntity(xmEntity);

        Content content = new Content();
        content.setValue("A".getBytes());

        Mockito.when(xmEntitySpecService.findAttachment(xmEntity.getTypeKey(), attachment.getTypeKey()))
            .thenReturn(Optional.of(attachmentSpec));

        Attachment save = contentService.save(attachment, content);

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
        attachmentSpec.setExpireLinkTimeInMillis(100000L);

        Attachment attachment = new Attachment();
        attachment.setName("test.doc");
        attachment.setXmEntity(xmEntity);

        Content content = new Content();
        content.setValue("A".getBytes());

        String bucketName = prepareBucketName();
        Mockito.when(amazonS3Template.createBucketIfNotExist(PREFIX, TENANT_KEY)).thenReturn(bucketName);
        Mockito.when(amazonS3Template.save(eq(bucketName), contains(attachment.getXmEntity().getTypeKey().toLowerCase() + "/"), eq(content),
            eq(attachment.getName()))).thenReturn(new UploadResultDto(bucketName, "someFileKey", "etag"));
        Mockito.when(xmEntitySpecService.findAttachment(xmEntity.getTypeKey(), attachment.getTypeKey()))
            .thenReturn(Optional.of(attachmentSpec));

        Attachment save = contentService.save(attachment, content);
        assertThat(save.getContentUrl()).isNotBlank();
        assertThat(save.getContentUrl()).isEqualTo(bucketName + "::someFileKey");
    }

    @Test
    @Transactional
    public void shouldReturnLinkFromAws() throws MalformedURLException {
        AttachmentSpec attachmentSpec = new AttachmentSpec();
        attachmentSpec.setStoreType(AttachmentStoreType.S3);
        attachmentSpec.setExpireLinkTimeInMillis(100000L);

        String fileName = "fileName";
        String bucketName = prepareBucketName();
        String contentUrl = bucketName + "::" + fileName;

        XmEntity xmEntity = new XmEntity();
        xmEntity.setTypeKey("T");

        Attachment attachment = new Attachment();
        attachment.setContentUrl(contentUrl);
        attachment.setXmEntity(xmEntity);

        String contentPathUrl = "http://localhost:8090/" + bucketName + "/fileName";
        Mockito.when(amazonS3Template.createExpirableLink(eq(attachment),
            eq(attachmentSpec.getExpireLinkTimeInMillis()))).thenReturn(new URL(contentPathUrl));

        Mockito.when(xmEntitySpecService.findAttachment(xmEntity.getTypeKey(), attachment.getTypeKey()))
            .thenReturn(Optional.of(attachmentSpec));

        String result = contentService.createExpirableLink(attachment);

        assertThat(result).isNotBlank();
        assertThat(result).isEqualTo(contentPathUrl);
    }

    private String prepareBucketName() {
        return PREFIX + "-" + TENANT_KEY.toLowerCase().replace("_", "-");
    }

}
