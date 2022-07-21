package com.icthh.xm.ms.entity.service;

import com.google.common.collect.Lists;
import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.exceptions.EntityNotFoundException;
import com.icthh.xm.commons.permission.repository.PermittedRepository;
import com.icthh.xm.ms.entity.AbstractUnitTest;
import com.icthh.xm.ms.entity.domain.Attachment;
import com.icthh.xm.ms.entity.domain.Content;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.spec.AttachmentSpec;
import com.icthh.xm.ms.entity.repository.AttachmentRepository;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
import com.icthh.xm.ms.entity.repository.backend.S3StorageRepository;
import com.icthh.xm.ms.entity.repository.search.PermittedSearchRepository;
import com.icthh.xm.ms.entity.service.impl.StartUpdateDateGenerationStrategy;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;

import static org.hamcrest.Matchers.hasProperty;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AttachmentServiceImplUnitTest  extends AbstractUnitTest {

    private AttachmentService attachmentService;

    private AttachmentRepository attachmentRepository;
    private PermittedRepository permittedRepository;
    private PermittedSearchRepository permittedSearchRepository;
    private StartUpdateDateGenerationStrategy startUpdateDateGenerationStrategy;
    private XmEntityRepository xmEntityRepository;
    private XmEntitySpecService xmEntitySpecService;
    private ContentService contentService;

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Before
    public void setUp() {
        attachmentRepository = Mockito.mock(AttachmentRepository.class);
        permittedRepository = Mockito.mock(PermittedRepository.class);
        permittedSearchRepository = Mockito.mock(PermittedSearchRepository.class);
        startUpdateDateGenerationStrategy = Mockito.mock(StartUpdateDateGenerationStrategy.class);
        xmEntityRepository = Mockito.mock(XmEntityRepository.class);
        xmEntitySpecService = Mockito.mock(XmEntitySpecService.class);
        contentService = Mockito.mock(ContentService.class);
        attachmentService = new AttachmentService(
            attachmentRepository, contentService, permittedRepository, permittedSearchRepository,
            startUpdateDateGenerationStrategy, xmEntityRepository, xmEntitySpecService
        );
    }

    @Test
    public void shouldFailIfMaxSizeIsZero() {
        AttachmentSpec spec = new AttachmentSpec();
        spec.setMax(0);
        exception.expect(BusinessException.class);
        exception.expect(hasProperty("code", is(AttachmentService.ZERO_RESTRICTION)));
        attachmentService.assertZeroRestriction(spec);
    }

    @Test
    public void shouldFailIfAttachmentSizeBiggerOrEqualsSpecValue() {
        AttachmentSpec spec = new AttachmentSpec();
        spec.setMax(1);
        spec.setKey("KEY1");
        XmEntity e = new XmEntity();
        e.setId(1L);
        when(attachmentRepository.countByXmEntityIdAndTypeKey(1L, "KEY1")).thenReturn(1);
        exception.expect(BusinessException.class);
        exception.expect(hasProperty("code", is(AttachmentService.MAX_RESTRICTION)));
        attachmentService.assertLimitRestriction(spec, e);
    }

    @Test
    public void shouldFailIfSpecNotFound() {
        XmEntity e = new XmEntity();
        e.setTypeKey("TYPE");
        Attachment a = new Attachment();
        a.setTypeKey("TYPE.A");
        exception.expect(EntityNotFoundException.class);
        exception.expectMessage(containsString("Spec.Attachment"));
        attachmentService.getSpec(e, a);
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

        exception.expect(BusinessException.class);
        exception.expect(hasProperty("code", is(AttachmentService.MAX_RESTRICTION)));
        attachmentService.save(a);
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
    public void search() {
        Attachment a = new Attachment();
        when(permittedSearchRepository.search("Q", Attachment.class, "P")).thenReturn(Lists.newArrayList(a, a));
        assertThat(attachmentService.search("Q", "P").size()).isEqualTo(2);
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
        ContentService contentService = new ContentService(null, null, s3StorageRepository, xmEntitySpecService);

        attachmentService = new AttachmentService(
            attachmentRepository, contentService, permittedRepository, permittedSearchRepository,
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
        verify(s3StorageRepository, Mockito.times(1)).delete("bucket", "fileName.png");
    }

}
