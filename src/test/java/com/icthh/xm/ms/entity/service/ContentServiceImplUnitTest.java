package com.icthh.xm.ms.entity.service;

import com.icthh.xm.commons.permission.repository.PermittedRepository;
import com.icthh.xm.ms.entity.AbstractUnitTest;
import com.icthh.xm.ms.entity.domain.Attachment;
import com.icthh.xm.ms.entity.domain.AttachmentStoreType;
import com.icthh.xm.ms.entity.domain.Content;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.spec.AttachmentSpec;
import com.icthh.xm.ms.entity.repository.ContentRepository;
import com.icthh.xm.ms.entity.repository.backend.AwsStorageRepository;
import com.icthh.xm.ms.entity.service.dto.UploadResultDto;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class ContentServiceImplUnitTest extends AbstractUnitTest {

    private PermittedRepository permittedRepository;
    private ContentRepository contentRepository;
    private AwsStorageRepository awsStorageRepository;


    private ContentService contentService;

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Before
    public void setUp() {
        permittedRepository = Mockito.mock(PermittedRepository.class);
        contentRepository = Mockito.mock(ContentRepository.class);
        awsStorageRepository = Mockito.mock(AwsStorageRepository.class);
        contentService = new ContentService(
            permittedRepository, contentRepository, awsStorageRepository
        );
    }

    @Test
    public void shouldSaveContentInDb() {
        AttachmentSpec attachmentSpec = new AttachmentSpec();

        XmEntity e = new XmEntity();
        e.setTypeKey("T");
        e.setId(1L);

        Content c = new Content();
        c.setValue("A".getBytes());

        Attachment a = new Attachment();
        a.setTypeKey("A.T");
        a.setContent(c);
        a.setXmEntity(e);

        Content result = new Content();
        result.setId(222L);
        result.setValue("A".getBytes());

        when(contentRepository.save(c)).thenReturn(result);

        Attachment save = contentService.save(attachmentSpec, a, c);
        assertThat(save.getContent().getId()).isEqualTo(222L);
        assertThat(save.getContentChecksum()).isEqualTo(DigestUtils.sha256Hex(c.getValue()));
        assertThat(save.getValueContentSize()).isEqualTo(c.getValue().length);
    }

    @Test
    public void shouldSaveContentInAws() {
        AttachmentSpec attachmentSpec = new AttachmentSpec();
        attachmentSpec.setStoreType(AttachmentStoreType.AWS);

        XmEntity e = new XmEntity();
        e.setTypeKey("T");
        e.setId(1L);

        Content c = new Content();
        c.setValue("A".getBytes());

        Attachment a = new Attachment();
        a.setTypeKey("A.T");
        a.setContent(c);
        a.setXmEntity(e);

        UploadResultDto result = new UploadResultDto("bucketName", "key", "eTag");

        when(awsStorageRepository.store(c, a.getName())).thenReturn(result);

        Attachment save = contentService.save(attachmentSpec, a, c);
        assertThat(save.getContentUrl()).isEqualTo(result.getBucketName() + "::" + result.getKey());
        assertThat(save.getContentChecksum()).isEqualTo(DigestUtils.sha256Hex(result.getETag()));
        assertThat(save.getValueContentSize()).isEqualTo(result.getETag().getBytes().length);
    }
}
