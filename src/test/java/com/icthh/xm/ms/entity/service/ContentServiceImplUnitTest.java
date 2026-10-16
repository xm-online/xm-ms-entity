package com.icthh.xm.ms.entity.service;

import com.icthh.xm.commons.permission.repository.PermittedRepository;
import com.icthh.xm.ms.entity.AbstractJupiterUnitTest;
import com.icthh.xm.ms.entity.config.XmEntityTenantConfigService;
import com.icthh.xm.ms.entity.domain.Attachment;
import com.icthh.xm.ms.entity.domain.AttachmentStoreType;
import com.icthh.xm.ms.entity.domain.Content;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.spec.AttachmentSpec;
import com.icthh.xm.ms.entity.repository.ContentRepository;
import com.icthh.xm.ms.entity.repository.backend.FsFileStorageRepository;
import com.icthh.xm.ms.entity.repository.backend.S3StorageRepository;
import com.icthh.xm.ms.entity.service.dto.UploadResultDto;
import com.icthh.xm.ms.entity.validator.AttachmentContentTypeValidator;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class ContentServiceImplUnitTest extends AbstractJupiterUnitTest {

    private ContentRepository contentRepository;
    private S3StorageRepository s3StorageRepository;
    private FsFileStorageRepository fsFileStorageRepository;
    private XmEntitySpecService xmEntitySpecService;

    private ContentService contentService;

    @BeforeEach
    public void setUp() {
        PermittedRepository permittedRepository = Mockito.mock(PermittedRepository.class);
        contentRepository = Mockito.mock(ContentRepository.class);
        s3StorageRepository = Mockito.mock(S3StorageRepository.class);
        fsFileStorageRepository = Mockito.mock(FsFileStorageRepository.class);
        xmEntitySpecService = Mockito.mock(XmEntitySpecService.class);
        contentService = new ContentService(
            permittedRepository, contentRepository, s3StorageRepository, fsFileStorageRepository, xmEntitySpecService
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
        Mockito.when(xmEntitySpecService.findAttachment(e.getTypeKey(), a.getTypeKey()))
            .thenReturn(Optional.of(attachmentSpec));

        Attachment save = contentService.save(a, c);
        assertThat(save.getContent().getId()).isEqualTo(222L);
        assertThat(save.getContentChecksum()).isEqualTo(DigestUtils.sha256Hex(c.getValue()));
        assertThat(save.getValueContentSize()).isEqualTo(c.getValue().length);
    }

    @Test
    public void shouldSaveContentInAws() {
        AttachmentSpec attachmentSpec = new AttachmentSpec();
        attachmentSpec.setStoreType(AttachmentStoreType.S3);

        XmEntity e = new XmEntity();
        e.setTypeKey("T");
        e.setId(1L);

        Content c = new Content();
        c.setValue("A".getBytes());

        Attachment a = new Attachment();
        a.setTypeKey("A.T");
        a.setContent(c);
        a.setXmEntity(e);

        UploadResultDto result = new UploadResultDto("bucketName", "T/key", "A");

        Mockito.when(xmEntitySpecService.findAttachment(e.getTypeKey(), a.getTypeKey()))
            .thenReturn(Optional.of(attachmentSpec));
        when(s3StorageRepository.store(c, e.getTypeKey(), a.getName())).thenReturn(result);

        Attachment save = contentService.save(a, c);
        assertThat(save.getContentUrl()).isEqualTo(result.getBucketName() + "::" + result.getKey());
        assertThat(save.getContentChecksum()).isEqualTo(result.getETag());
        assertThat(save.getValueContentSize()).isEqualTo(result.getETag().getBytes().length);
    }

    @Test
    public void shouldSaveContentInFS() {
        AttachmentSpec attachmentSpec = new AttachmentSpec();
        attachmentSpec.setStoreType(AttachmentStoreType.FS);

        XmEntity e = new XmEntity();
        e.setTypeKey("T");
        e.setId(1L);

        Content c = new Content();
        c.setValue("A".getBytes());

        Attachment a = new Attachment();
        a.setTypeKey("A.T");
        a.setContent(c);
        a.setXmEntity(e);

        UploadResultDto result = new UploadResultDto("bebe", "fileName", "123");

        Mockito.when(xmEntitySpecService.findAttachment(e.getTypeKey(), a.getTypeKey()))
            .thenReturn(Optional.of(attachmentSpec));
        when(fsFileStorageRepository.store(c, e.getTypeKey(), a.getName())).thenReturn(result);

        Attachment save = contentService.save(a, c);
        assertThat(save.getContentUrl()).isEqualTo("bebe::fileName");
        assertThat(save.getContentChecksum()).isEqualTo("123");
        assertThat(save.getValueContentSize()).isEqualTo(c.getValue().length);
    }

    @Test
    public void shouldDeleteContentFromStorageOfContentUrl() {
        // the store type of the spec may change after the attachment is saved: the storage of the content url is used
        contentService.delete(attachmentWithSpec(null, "file://T/abc-file"));
        verify(fsFileStorageRepository).delete("file://T/abc-file");

        contentService.delete(attachmentWithSpec(AttachmentStoreType.FS, "bucket::T/key"));
        verify(s3StorageRepository).delete("bucket::T/key");
    }

    @Test
    public void shouldNotTouchStorageWhenDbAttachmentIsDeleted() {
        contentService.delete(attachmentWithSpec(AttachmentStoreType.FS, null));
        contentService.delete(attachmentWithSpec(null, "https://example.com/doc.pdf"));

        verifyNoInteractions(fsFileStorageRepository, s3StorageRepository);
    }

    @Test
    public void shouldSupportDownloadLinkOnlyForS3ContentUrl() {
        assertThat(contentService.supportDownloadLink(attachmentWithSpec(null, "bucket::T/key"))).isTrue();
        assertThat(contentService.supportDownloadLink(attachmentWithSpec(AttachmentStoreType.S3, "file://T/abc-file"))).isFalse();
        assertThat(contentService.supportDownloadLink(attachmentWithSpec(AttachmentStoreType.S3, null))).isFalse();
    }

    private Attachment attachmentWithSpec(AttachmentStoreType storeType, String contentUrl) {
        AttachmentSpec attachmentSpec = new AttachmentSpec();
        attachmentSpec.setStoreType(storeType);

        XmEntity e = new XmEntity();
        e.setTypeKey("T");

        Attachment a = new Attachment();
        a.setTypeKey("A.T");
        a.setContentUrl(contentUrl);
        a.setXmEntity(e);

        when(xmEntitySpecService.findAttachment("T", "A.T")).thenReturn(Optional.of(attachmentSpec));
        return a;
    }

}
