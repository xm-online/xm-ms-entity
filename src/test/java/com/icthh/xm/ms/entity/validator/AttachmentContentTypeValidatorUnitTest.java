package com.icthh.xm.ms.entity.validator;

import com.icthh.xm.ms.entity.AbstractJupiterUnitTest;
import com.icthh.xm.ms.entity.config.ApplicationProperties;
import com.icthh.xm.ms.entity.config.XmEntityTenantConfigService;
import com.icthh.xm.ms.entity.domain.Attachment;
import com.icthh.xm.ms.entity.domain.Content;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.spec.AttachmentSpec;
import com.icthh.xm.ms.entity.service.XmEntitySpecService;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class AttachmentContentTypeValidatorUnitTest extends AbstractJupiterUnitTest {

    @Mock
    private XmEntitySpecService xmEntitySpecService;

    @Mock
    private XmEntityTenantConfigService xmEntityTenantConfigService;

    @Mock
    private ConstraintValidatorContext context;

    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder violationBuilder;

    private AttachmentContentTypeValidator validator;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        validator = new AttachmentContentTypeValidator(xmEntitySpecService, xmEntityTenantConfigService);

        when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilder);
        when(violationBuilder.addConstraintViolation()).thenReturn(context);

        XmEntityTenantConfigService.XmEntityTenantConfig tenantConfig = new XmEntityTenantConfigService.XmEntityTenantConfig();
        tenantConfig.getAttachmentValidation().setContentTypeValidationEnabled(true);
        when(xmEntityTenantConfigService.getXmEntityTenantConfig()).thenReturn(tenantConfig);
    }

    @Test
    public void shouldPassValidationWhenDisabled() {
        Attachment attachment = createAttachmentWithContent("test.jpg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
        
        XmEntityTenantConfigService.XmEntityTenantConfig tenantConfig = new XmEntityTenantConfigService.XmEntityTenantConfig();
        tenantConfig.getAttachmentValidation().setContentTypeValidationEnabled(false);
        when(xmEntityTenantConfigService.getXmEntityTenantConfig()).thenReturn(tenantConfig);

        assertTrue(validator.isValid(attachment, context));
    }

    @Test
    public void shouldPassValidationWhenNoSpec() {
        Attachment attachment = createAttachmentWithContent("test.jpg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
        assertTrue(validator.isValid(attachment, context));
    }

    @Test
    public void shouldPassValidationWhenNoContentTypesInSpec() {
        Attachment attachment = createAttachmentWithContent("test.jpg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
        
        AttachmentSpec spec = new AttachmentSpec();
        spec.setContentTypes(null);
        when(xmEntitySpecService.findAttachment(anyString(), anyString())).thenReturn(java.util.Optional.of(spec));

        assertTrue(validator.isValid(attachment, context));
    }

    @Test
    public void shouldPassValidationWhenEmptyContentTypesInSpec() {
        Attachment attachment = createAttachmentWithContent("test.jpg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
        
        AttachmentSpec spec = new AttachmentSpec();
        spec.setContentTypes(Arrays.asList());
        when(xmEntitySpecService.findAttachment(anyString(), anyString())).thenReturn(java.util.Optional.of(spec));

        assertTrue(validator.isValid(attachment, context));
    }

    @Test
    public void shouldPassValidationWhenNoContent() {
        Attachment attachment = createAttachmentWithContent("test.jpg", new byte[0]);
        
        AttachmentSpec spec = new AttachmentSpec();
        spec.setContentTypes(Arrays.asList("image/jpeg"));
        when(xmEntitySpecService.findAttachment(anyString(), anyString())).thenReturn(java.util.Optional.of(spec));

        assertTrue(validator.isValid(attachment, context));
    }

    @Test
    public void shouldPassValidationForJpegFile() {
        byte[] jpegBytes = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00};
        Attachment attachment = createAttachmentWithContent("test.jpg", jpegBytes);
        
        AttachmentSpec spec = new AttachmentSpec();
        spec.setContentTypes(Arrays.asList("image/jpeg"));
        when(xmEntitySpecService.findAttachment(anyString(), anyString())).thenReturn(java.util.Optional.of(spec));

        assertTrue(validator.isValid(attachment, context));
    }

    @Test
    public void shouldPassValidationForPngFile() {
        byte[] pngBytes = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        Attachment attachment = createAttachmentWithContent("test.png", pngBytes);
        
        AttachmentSpec spec = new AttachmentSpec();
        spec.setContentTypes(Arrays.asList("image/png"));
        when(xmEntitySpecService.findAttachment(anyString(), anyString())).thenReturn(java.util.Optional.of(spec));

        assertTrue(validator.isValid(attachment, context));
    }

    @Test
    public void shouldPassValidationForPdfFile() {
        byte[] pdfBytes = {0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x34};
        Attachment attachment = createAttachmentWithContent("test.pdf", pdfBytes);
        
        AttachmentSpec spec = new AttachmentSpec();
        spec.setContentTypes(Arrays.asList("application/pdf"));
        when(xmEntitySpecService.findAttachment(anyString(), anyString())).thenReturn(java.util.Optional.of(spec));

        assertTrue(validator.isValid(attachment, context));
    }

    @Test
    public void shouldNotPassValidationForWildcardContentType() {
        byte[] jpegBytes = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00};
        Attachment attachment = createAttachmentWithContent("test.jpg", jpegBytes);
        
        AttachmentSpec spec = new AttachmentSpec();
        spec.setContentTypes(Arrays.asList("image/*"));
        when(xmEntitySpecService.findAttachment(anyString(), anyString())).thenReturn(java.util.Optional.of(spec));

        assertFalse(validator.isValid(attachment, context));
    }

    @Test
    public void shouldFailValidationForWrongContentType() {
        byte[] jpegBytes = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00};
        Attachment attachment = createAttachmentWithContent("test.jpg", jpegBytes);
        
        AttachmentSpec spec = new AttachmentSpec();
        spec.setContentTypes(Arrays.asList("application/pdf"));
        when(xmEntitySpecService.findAttachment(anyString(), anyString())).thenReturn(java.util.Optional.of(spec));

        assertFalse(validator.isValid(attachment, context));
    }

    @Test
    public void shouldFailValidationForTextFileWhenImageExpected() {
        byte[] textBytes = "Hello World".getBytes();
        Attachment attachment = createAttachmentWithContent("test.txt", textBytes);
        
        AttachmentSpec spec = new AttachmentSpec();
        spec.setContentTypes(Arrays.asList("image/jpeg"));
        when(xmEntitySpecService.findAttachment(anyString(), anyString())).thenReturn(java.util.Optional.of(spec));

        assertFalse(validator.isValid(attachment, context));
    }

    @Test
    public void shouldPassValidationForMultipleAllowedTypes() {
        byte[] jpegBytes = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00};
        Attachment attachment = createAttachmentWithContent("test.jpg", jpegBytes);
        
        AttachmentSpec spec = new AttachmentSpec();
        spec.setContentTypes(Arrays.asList("image/jpeg", "image/png", "application/pdf"));
        when(xmEntitySpecService.findAttachment(anyString(), anyString())).thenReturn(java.util.Optional.of(spec));

        assertTrue(validator.isValid(attachment, context));
    }

    @Test
    public void shouldPassValidationForNullAttachment() {
        assertTrue(validator.isValid(null, context));
    }

    private Attachment createAttachmentWithContent(String fileName, byte[] contentBytes) {
        Attachment attachment = new Attachment();
        attachment.setTypeKey("TEST");
        
        XmEntity xmEntity = new XmEntity();
        xmEntity.setTypeKey("ENTITY_TYPE");
        attachment.setXmEntity(xmEntity);
        
        Content content = new Content();
        content.setValue(contentBytes);
        attachment.setContent(content);
        
        return attachment;
    }
}
