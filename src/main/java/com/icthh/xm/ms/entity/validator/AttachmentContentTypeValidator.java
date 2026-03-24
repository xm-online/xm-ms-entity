package com.icthh.xm.ms.entity.validator;

import com.icthh.xm.ms.entity.config.ApplicationProperties;
import com.icthh.xm.ms.entity.domain.Attachment;
import com.icthh.xm.ms.entity.domain.Content;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.spec.AttachmentSpec;
import com.icthh.xm.ms.entity.service.XmEntitySpecService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.tika.Tika;
import org.apache.tika.mime.MediaType;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class AttachmentContentTypeValidator implements ConstraintValidator<AttachmentContentType, Attachment> {

    private final Tika tika = new Tika();

    private final ApplicationProperties applicationProperties;
    private final XmEntitySpecService xmEntitySpecService;

    @Override
    public boolean isValid(Attachment attachment, ConstraintValidatorContext context) {
        if (!isContentTypeValidationEnabled()) {
            return true;
        }

        if (Objects.isNull(attachment) || Objects.isNull(attachment.getXmEntity())) {
            return true;
        }

        try {
            XmEntity entity = attachment.getXmEntity();
            AttachmentSpec spec = xmEntitySpecService
                .findAttachment(entity.getTypeKey(), attachment.getTypeKey())
                .orElse(null);

            if (Objects.isNull(spec) || CollectionUtils.isEmpty(spec.getContentTypes())) {
                return true;
            }

            List<String> allowedContentTypes = spec.getContentTypes();

            byte[] contentBytes = getContentBytes(attachment);
            if (contentBytes == null || contentBytes.length == 0) {
                return true;
            }

            String detectedContentType = detectContentType(contentBytes);

            boolean isValid = allowedContentTypes.stream()
                .anyMatch(allowed -> isContentTypeMatch(detectedContentType, allowed));

            if (!isValid && context != null) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                    "Detected content type '" + detectedContentType +
                    "' is not allowed. Allowed types: " + allowedContentTypes)
                .addConstraintViolation();
            }

            return isValid;
        } catch (Exception e) {
            log.error("Error during attachment content type validation", e);
            return false;
        }
    }

    private boolean isContentTypeValidationEnabled() {
        return applicationProperties.getAttachmentValidation().isContentTypeValidationEnabled();
    }

    private byte[] getContentBytes(Attachment attachment) {
        return Optional.ofNullable(attachment.getContent()).map(Content::getValue).orElse(null);

    }

    private String detectContentType(byte[] contentBytes) {
        try (InputStream inputStream = new ByteArrayInputStream(contentBytes)) {
            return tika.detect(inputStream);
        } catch (IOException e) {
            log.warn("Error detecting content type with Tika", e);
            throw new IllegalArgumentException("Error detecting content type", e);
        }
    }

    private boolean isContentTypeMatch(String detected, String allowed) {
        try {
            MediaType detectedType = MediaType.parse(detected);
            MediaType allowedType = MediaType.parse(allowed);

            if (detectedType.equals(allowedType)) {
                return true;
            }

        } catch (Exception e) {
            log.debug("Error parsing media types with Tika", e);
            if (detected.equalsIgnoreCase(allowed)) {
                return true;
            }
        }

        return false;
    }
}
