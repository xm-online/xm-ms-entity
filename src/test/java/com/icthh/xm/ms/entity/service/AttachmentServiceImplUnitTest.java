package com.icthh.xm.ms.entity.service;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.permission.repository.PermittedRepository;
import com.icthh.xm.ms.entity.AbstractUnitTest;
import com.icthh.xm.ms.entity.domain.Attachment;
import com.icthh.xm.ms.entity.domain.Content;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.spec.AttachmentSpec;
import com.icthh.xm.ms.entity.repository.AttachmentRepository;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
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
import static org.mockito.Mockito.when;

public class AttachmentServiceImplUnitTest  extends AbstractUnitTest {

    private AttachmentService attachmentService;

    private AttachmentRepository attachmentRepository;
    private PermittedRepository permittedRepository;
    private PermittedSearchRepository permittedSearchRepository;
    private StartUpdateDateGenerationStrategy startUpdateDateGenerationStrategy;
    private XmEntityRepository xmEntityRepository;
    private XmEntitySpecService xmEntitySpecService;

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
        attachmentService = new AttachmentService(
            attachmentRepository, permittedRepository, permittedSearchRepository, startUpdateDateGenerationStrategy,
            xmEntityRepository, xmEntitySpecService
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
        Attachment a1 = new Attachment();
        a1.setTypeKey("KEY1");
        e.addAttachments(a1);
        exception.expect(BusinessException.class);
        exception.expect(hasProperty("code", is(AttachmentService.MAX_RESTRICTION)));
        attachmentService.assertLimitRestriction(spec, e);
    }

    @Test
    public void shouldPassForOtherCodes() {
        AttachmentSpec spec = new AttachmentSpec();
        spec.setMax(1);
        spec.setKey("KEY1");
        XmEntity e = new XmEntity();
        Attachment a1 = new Attachment();
        a1.setTypeKey("KEY2");
        e.addAttachments(a1);
        attachmentService.assertLimitRestriction(spec, e);
    }

    @Test
    public void shouldFailIfSpecNotFound() {
        XmEntity e = new XmEntity();
        e.setTypeKey("TYPE");
        Attachment a = new Attachment();
        a.setTypeKey("TYPE.A");
        exception.expect(IllegalArgumentException.class);
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

        when(xmEntityRepository.getOne(1L)).thenReturn(e);
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

        when(xmEntityRepository.getOne(1L)).thenReturn(mocked);
        when(xmEntitySpecService.findAttachment("T", "A.T")).thenReturn(Optional.of(spec));
        when(attachmentRepository.save(any())).thenReturn(result);

        exception.expect(BusinessException.class);
        exception.expect(hasProperty("code", is(AttachmentService.MAX_RESTRICTION)));
        attachmentService.save(a);
    }

    @Test
    public void shouldSaveWithOfCondition() {
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

        when(xmEntityRepository.getOne(1L)).thenReturn(mocked);
        when(xmEntitySpecService.findAttachment("T", "A.T")).thenReturn(Optional.of(spec));
        when(attachmentRepository.save(any())).thenReturn(result);

        assertThat(attachmentService.save(a).getId()).isEqualTo(222L);
    }

}
