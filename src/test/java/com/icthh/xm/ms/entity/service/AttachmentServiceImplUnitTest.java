package com.icthh.xm.ms.entity.service;

import com.google.common.collect.Lists;
import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.exceptions.EntityNotFoundException;
import com.icthh.xm.commons.permission.repository.PermittedRepository;
import com.icthh.xm.ms.entity.AbstractJupiterUnitTest;
import com.icthh.xm.ms.entity.domain.Attachment;
import com.icthh.xm.ms.entity.domain.Content;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.spec.AttachmentSpec;
import com.icthh.xm.ms.entity.repository.AttachmentRepository;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
import com.icthh.xm.ms.entity.repository.backend.FsFileStorageRepository;
import com.icthh.xm.ms.entity.repository.backend.S3StorageRepository;
import com.icthh.xm.ms.entity.service.impl.StartUpdateDateGenerationStrategy;
import com.icthh.xm.ms.entity.config.ApplicationProperties;
import com.icthh.xm.ms.entity.validator.AttachmentContentTypeValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AttachmentServiceImplUnitTest extends AbstractJupiterUnitTest {

    private AttachmentService attachmentService;
    private AttachmentContentTypeValidator attachmentContentTypeValidator;

    private AttachmentRepository attachmentRepository;
    private PermittedRepository permittedRepository;
    private StartUpdateDateGenerationStrategy startUpdateDateGenerationStrategy;
    private XmEntityRepository xmEntityRepository;
    private XmEntitySpecService xmEntitySpecService;
    private ContentService contentService;
    private ApplicationProperties applicationProperties;

    @BeforeEach
    public void setUp() {
        attachmentRepository = Mockito.mock(AttachmentRepository.class);
        permittedRepository = Mockito.mock(PermittedRepository.class);
        startUpdateDateGenerationStrategy = Mockito.mock(StartUpdateDateGenerationStrategy.class);
        xmEntityRepository = Mockito.mock(XmEntityRepository.class);
        xmEntitySpecService = Mockito.mock(XmEntitySpecService.class);
        contentService = Mockito.mock(ContentService.class);
        applicationProperties = Mockito.mock(ApplicationProperties.class);
        attachmentContentTypeValidator = new AttachmentContentTypeValidator(applicationProperties, xmEntitySpecService);
        attachmentService = new AttachmentService(
            attachmentRepository, contentService, permittedRepository,
            startUpdateDateGenerationStrategy, xmEntityRepository, xmEntitySpecService
        );

        ApplicationProperties.AttachmentValidation validation = new ApplicationProperties.AttachmentValidation();
        validation.setContentTypeValidationEnabled(false);
        when(applicationProperties.getAttachmentValidation()).thenReturn(validation);
    }

    @Test
    public void shouldFailIfMaxSizeIsZero() {

        BusinessException thrown = assertThrows(BusinessException.class, () -> {
                AttachmentSpec spec = new AttachmentSpec();
                spec.setMax(0);
                attachmentService.assertZeroRestriction(spec);
            }
        );

        assertInstanceOf(BusinessException.class, thrown);
        assertThat(thrown.getCode()).isEqualTo(AttachmentService.ZERO_RESTRICTION);
        //thrown.expect(BusinessException.class);
        //thrown.expect(hasProperty("code", is(AttachmentService.ZERO_RESTRICTION)));
    }

    @Test
    public void shouldFailIfAttachmentSizeBiggerOrEqualsSpecValue() {

        when(attachmentRepository.countByXmEntityIdAndTypeKey(1L, "KEY1")).thenReturn(1);

        BusinessException thrown = assertThrows(BusinessException.class, () -> {
            AttachmentSpec spec = new AttachmentSpec();
            spec.setMax(1);
            spec.setKey("KEY1");
            XmEntity e = new XmEntity();
            e.setId(1L);
            attachmentService.assertLimitRestriction(spec, e);
        });

        assertInstanceOf(BusinessException.class, thrown);
        assertThat(thrown.getCode()).isEqualTo(AttachmentService.MAX_RESTRICTION);
    }

    @Test
    public void shouldFailIfAttachmentContentSizeBiggerOfSpecValue() {

        when(attachmentRepository.countByXmEntityIdAndTypeKey(1L, "KEY1")).thenReturn(1);

        BusinessException thrown = assertThrows(BusinessException.class, () -> {
            AttachmentSpec spec = new AttachmentSpec();
            spec.setKey("KEY1");
            spec.setSize("1");

            Content c = new Content();
            c.setValue("Hello world!".getBytes());
            attachmentService.assertFileSize(spec, c);
        });

        assertInstanceOf(BusinessException.class, thrown);
        assertThat(thrown.getCode()).isEqualTo(AttachmentService.SIZE_RESTRICTION);

    }

    @Test
    public void shouldFailIfSpecNotFound() {

        EntityNotFoundException thrown = assertThrows(EntityNotFoundException.class, () -> {
            XmEntity e = new XmEntity();
            e.setTypeKey("TYPE");
            Attachment a = new Attachment();
            a.setTypeKey("TYPE.A");
            attachmentService.getSpec(e, a);
        });


        assertInstanceOf(EntityNotFoundException.class, thrown);
        assertThat(thrown.getMessage().contains("Spec.Attachment")).isTrue();
        //exception.expect(EntityNotFoundException.class);
        //exception.expectMessage(containsString("Spec.Attachment"));

    }

    @Test
    public void shouldPassIfSpecProvided() {
        XmEntity e = new XmEntity();
        e.setTypeKey("TYPE");

        Attachment a = new Attachment();
        a.setTypeKey("TYPE.A");

        AttachmentSpec spec = new AttachmentSpec();
        spec.setKey("TYPE.A");

        when(xmEntitySpecService.findAttachment("TYPE", "TYPE.A")).thenReturn(Optional.of(spec));

        assertThat(attachmentService.getSpec(e, a).getKey()).isEqualTo("TYPE.A");
    }

    @Test
    public void shouldSaveForSpecNullValueWithoutValidation() {
        XmEntity e = new XmEntity();
        e.setTypeKey("T");
        e.setId(1L);

        Content c = new Content();
        c.setValue("A".getBytes());

        Attachment a = new Attachment();
        a.setTypeKey("A.T");
        a.setId(5L);
        a.setContent(c);
        a.setXmEntity(e);

        Attachment result = new Attachment();
        result.setId(222L);

        AttachmentSpec spec = new AttachmentSpec();
        spec.setKey("A.T");
        spec.setSize("1MB");

        when(xmEntityRepository.findById(1L)).thenReturn(Optional.of(e));
        when(xmEntitySpecService.findAttachment("T", "A.T")).thenReturn(Optional.of(spec));
        when(attachmentRepository.save(any())).thenReturn(result);

        assertThat(attachmentService.save(a).getId()).isEqualTo(222L);
    }

    @Test
    public void shouldFailForMaxCount() {
        XmEntity e = new XmEntity();
        e.setTypeKey("T");
        e.setId(1L);

        Content c = new Content();
        c.setValue("A".getBytes());

        Attachment a = new Attachment();
        a.setTypeKey("A.T");
        a.setContent(c);
        a.setXmEntity(e);

        Attachment result = new Attachment();
        result.setId(222L);

        AttachmentSpec spec = new AttachmentSpec();
        spec.setKey("A.T");
        spec.setMax(1);
        spec.setSize("1MB");

        XmEntity mocked = new XmEntity();
        mocked.setId(1L);
        mocked.setTypeKey("T");
        Attachment a1 = new Attachment();
        a1.setTypeKey("A.T");
        mocked.addAttachments(a1);

        when(xmEntityRepository.findById(1L)).thenReturn(Optional.of(mocked));
        when(xmEntitySpecService.findAttachment("T", "A.T")).thenReturn(Optional.of(spec));
        when(attachmentRepository.save(any())).thenReturn(result);
        when(attachmentRepository.countByXmEntityIdAndTypeKey(1L, "A.T")).thenReturn(1);

        BusinessException thrown = assertThrows(BusinessException.class, () -> {
            attachmentService.save(a);
        });

        assertInstanceOf(BusinessException.class, thrown);
        assertThat(thrown.getCode()).isEqualTo(AttachmentService.MAX_RESTRICTION);

        //exception.expect(BusinessException.class);
        //exception.expect(hasProperty("code", is(AttachmentService.MAX_RESTRICTION)));

    }

    @Test
    public void shouldFailForMaxContentSize() {
        XmEntity e = new XmEntity();
        e.setTypeKey("T");
        e.setId(1L);

        Content c = new Content();
        c.setValue("Hello world!".getBytes());

        Attachment a = new Attachment();
        a.setTypeKey("A.T");
        a.setContent(c);
        a.setXmEntity(e);

        Attachment result = new Attachment();
        result.setId(222L);

        AttachmentSpec spec = new AttachmentSpec();
        spec.setKey("A.T");
        spec.setMax(1);
        spec.setSize("1");

        XmEntity mocked = new XmEntity();
        mocked.setId(1L);
        mocked.setTypeKey("T");
        Attachment a1 = new Attachment();
        a1.setTypeKey("A.T");
        mocked.addAttachments(a1);

        when(xmEntityRepository.findById(1L)).thenReturn(Optional.of(mocked));
        when(xmEntitySpecService.findAttachment("T", "A.T")).thenReturn(Optional.of(spec));
        when(attachmentRepository.save(any())).thenReturn(result);
        when(attachmentRepository.countByXmEntityIdAndTypeKey(1L, "A.T")).thenReturn(1);


        BusinessException thrown = assertThrows(BusinessException.class, () -> {
            attachmentService.save(a);
        });

        assertInstanceOf(BusinessException.class, thrown);
        assertThat(thrown.getCode()).isEqualTo(AttachmentService.SIZE_RESTRICTION);

        /*exception.expect(BusinessException.class);
        exception.expect(hasProperty("code", is(AttachmentService.SIZE_RESTRICTION)));
        attachmentService.save(a);*/
    }

    @Test
    public void shouldSaveWithOkCondition() {
        XmEntity e = new XmEntity();
        e.setTypeKey("T");
        e.setId(1L);

        Content c = new Content();
        c.setValue("A".getBytes());

        Attachment a = new Attachment();
        a.setTypeKey("A.T");
        a.setContent(c);
        a.setXmEntity(e);

        Attachment result = new Attachment();
        result.setId(222L);

        AttachmentSpec spec = new AttachmentSpec();
        spec.setKey("A.T");
        spec.setMax(2); //1 is added in Mock
        spec.setSize("1MB");

        XmEntity mocked = new XmEntity();
        mocked.setId(1L);
        mocked.setTypeKey("T");
        Attachment a1 = new Attachment();
        a1.setTypeKey("A.T");
        mocked.addAttachments(a1);

        when(xmEntityRepository.findById(1L)).thenReturn(Optional.of(mocked));
        when(xmEntitySpecService.findAttachment("T", "A.T")).thenReturn(Optional.of(spec));
        when(attachmentRepository.save(any())).thenReturn(result);
        when(attachmentRepository.countByXmEntityIdAndTypeKey(1L, "A.T")).thenReturn(1);

        assertThat(attachmentService.save(a).getId()).isEqualTo(222L);
    }

    @Test
    public void findAll() {
        Attachment a = new Attachment();
        when(permittedRepository.findAll(Attachment.class, "P1")).thenReturn(Lists.newArrayList(a ,a));
        assertThat(attachmentService.findAll("P1").size()).isEqualTo(2);
    }

    @Test
    public void getById() {
        Attachment a = new Attachment();
        a.setId(1L);
        when(attachmentRepository.findById(1L)).thenReturn(Optional.of(a));
        assertThat(attachmentService.findById(1L).get().getId()).isEqualTo(1L);
        assertThat(attachmentService.findById(2L).isPresent()).isEqualTo(false);
    }

    @Test
    public void findById() {
        Attachment a = new Attachment();
        a.setId(1L);
        when(attachmentRepository.findById(1L)).thenReturn(Optional.of(a));
        assertThat(attachmentService.findOne(1L).getId()).isEqualTo(1L);
        assertThat(attachmentService.findOne(2L)).isNull();
    }

    @Test
    public void getByIdWithContext() {
        XmEntity e = new XmEntity();
        e.setTypeKey("T");

        Content c = new Content();
        c.setValue("A".getBytes());

        Attachment a = new Attachment();
        a.setTypeKey("A.T");
        a.setId(1L);
        a.setContent(c);
        a.setXmEntity(e);

        AttachmentSpec spec = new AttachmentSpec();
        spec.setKey("A.T");

        when(attachmentRepository.findById(1L)).thenReturn(Optional.of(a));
        when(xmEntitySpecService.findAttachment("T", "A.T")).thenReturn(Optional.of(spec));
        when(contentService.enrichContent(a)).thenReturn(a);

        assertThat(attachmentService.getOneWithContent(1L).get().getId()).isEqualTo(1L);
        assertThat(attachmentService.getOneWithContent(2L).isPresent()).isEqualTo(false);
    }

    @Test
    public void findByIdWithContext() {
        XmEntity e = new XmEntity();
        e.setTypeKey("T");

        Content c = new Content();
        c.setValue("A".getBytes());

        Attachment a = new Attachment();
        a.setTypeKey("A.T");
        a.setId(1L);
        a.setContent(c);
        a.setXmEntity(e);

        AttachmentSpec spec = new AttachmentSpec();
        spec.setKey("A.T");

        when(xmEntitySpecService.findAttachment("T", "A.T")).thenReturn(Optional.of(spec));
        when(attachmentRepository.findById(1L)).thenReturn(Optional.of(a));
        when(contentService.enrichContent(a)).thenReturn(a);

        assertThat(attachmentService.findOneWithContent(1L).getId()).isEqualTo(1L);
        assertThat(attachmentService.findOneWithContent(2L)).isNull();
    }

    @Test
    public void shouldDeleteItemInDb() {
        XmEntity e = new XmEntity();
        e.setTypeKey("T");

        Content c = new Content();
        c.setValue("A".getBytes());

        Attachment a = new Attachment();
        a.setTypeKey("A.T");
        a.setId(1L);
        a.setContent(c);
        a.setXmEntity(e);

        when(attachmentRepository.findById(1L)).thenReturn(Optional.of(a));

        attachmentService.delete(1L);
        verify(attachmentRepository, Mockito.times(1)).deleteById(1L);
        verify(contentService, Mockito.times(1)).delete(a);
    }

    @Test
    public void shouldDeleteItemInS3() {
        S3StorageRepository s3StorageRepository  = Mockito.mock(S3StorageRepository.class);
        FsFileStorageRepository fsFileStorageRepository = Mockito.mock(FsFileStorageRepository.class);
        ContentService contentService = new ContentService(null, null, s3StorageRepository, fsFileStorageRepository, xmEntitySpecService, attachmentContentTypeValidator);

        attachmentService = new AttachmentService(
            attachmentRepository, contentService, permittedRepository,
            startUpdateDateGenerationStrategy, xmEntityRepository, xmEntitySpecService
        );


        XmEntity e = new XmEntity();
        e.setTypeKey("T");

        Content c = new Content();
        c.setValue("A".getBytes());

        Attachment a = new Attachment();
        a.setTypeKey("A.T");
        a.setId(1L);
        a.setContentUrl("bucket::fileName.png");
        a.setContent(c);
        a.setXmEntity(e);

        when(attachmentRepository.findById(1L)).thenReturn(Optional.of(a));

        attachmentService.delete(1L);
        verify(attachmentRepository, Mockito.times(1)).deleteById(1L);
        verify(s3StorageRepository, Mockito.times(1)).delete("bucket::fileName.png");
    }

    @Test
    public void shouldPassContentTypeValidationWhenEnable() {
        AttachmentSpec spec = new AttachmentSpec();
        spec.setContentTypes(Arrays.asList("image/jpeg", "image/png"));
        
        Attachment attachment = createAttachmentWithJpegContent();

        ApplicationProperties.AttachmentValidation validation = new ApplicationProperties.AttachmentValidation();
        validation.setContentTypeValidationEnabled(true);
        when(applicationProperties.getAttachmentValidation()).thenReturn(validation);

        assertDoesNotThrow(() -> attachmentService.assertContentType(spec, attachment));
    }

    @Test
    public void shouldPassContentTypeValidationWhenEmptySpecContentTypes() {
        AttachmentSpec spec = new AttachmentSpec();
        spec.setContentTypes(Arrays.asList());

        Attachment attachment = createAttachmentWithPdfContent();

        ApplicationProperties.AttachmentValidation validation = new ApplicationProperties.AttachmentValidation();
        validation.setContentTypeValidationEnabled(true);
        when(applicationProperties.getAttachmentValidation()).thenReturn(validation);

        assertDoesNotThrow(() -> attachmentService.assertContentType(spec, attachment));
    }

    @Test
    public void shouldNOTPassContentTypeValidationWhenSpecContentTypesNotEq() {
        ContentService contentService = new ContentService(null, null, null, null, xmEntitySpecService, attachmentContentTypeValidator);

        attachmentService = new AttachmentService(
                attachmentRepository, contentService, permittedRepository,
                startUpdateDateGenerationStrategy, xmEntityRepository, xmEntitySpecService
        );

        AttachmentSpec spec = new AttachmentSpec();
        spec.setContentTypes(Arrays.asList("application/pdf"));

        Attachment attachment = createAttachmentWithJpegContent();

        ApplicationProperties.AttachmentValidation validation = new ApplicationProperties.AttachmentValidation();
        validation.setContentTypeValidationEnabled(true);
        when(applicationProperties.getAttachmentValidation()).thenReturn(validation);
        when(xmEntitySpecService.findAttachment(eq(attachment.getXmEntity().getTypeKey()), eq(attachment.getTypeKey()))).thenReturn(Optional.of(spec));

        BusinessException thrown = assertThrows(BusinessException.class, () -> {
            attachmentService.assertContentType(spec, attachment);
        });

        assertInstanceOf(BusinessException.class, thrown);
        assertThat(thrown.getCode()).isEqualTo(AttachmentService.CONTENT_TYPE_RESTRICTION);
    }

    private Attachment createAttachmentWithJpegContent() {
        byte[] jpegBytes = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00};
        XmEntity e = new XmEntity();
        e.setTypeKey("T");

        Content c = new Content();
        c.setValue(jpegBytes);

        Attachment attachment = new Attachment();
        attachment.setTypeKey("A.T");
        attachment.setId(1L);
        attachment.setContentUrl("bucket::test.jpeg");
        attachment.setContent(c);
        attachment.setXmEntity(e);
        return attachment;
    }

    private Attachment createAttachmentWithPdfContent() {
        byte[] pdfBytes = {0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x34};
        XmEntity e = new XmEntity();
        e.setTypeKey("T");

        Content c = new Content();
        c.setValue(pdfBytes);

        Attachment attachment = new Attachment();
        attachment.setTypeKey("A.T");
        attachment.setId(1L);
        attachment.setContentUrl("bucket::test.pdf");
        attachment.setContent(c);
        attachment.setXmEntity(e);
        attachment.setValueContentType("application/pdf");
        return attachment;
    }
}
