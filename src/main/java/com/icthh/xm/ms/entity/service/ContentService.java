package com.icthh.xm.ms.entity.service;


import com.amazonaws.util.IOUtils;
import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.exceptions.EntityNotFoundException;
import com.icthh.xm.commons.permission.annotation.FindWithPermission;
import com.icthh.xm.commons.permission.annotation.PrivilegeDescription;
import com.icthh.xm.commons.permission.repository.PermittedRepository;
import com.icthh.xm.ms.entity.config.ApplicationProperties;
import com.icthh.xm.ms.entity.domain.Attachment;
import com.icthh.xm.ms.entity.domain.AttachmentStoreType;
import com.icthh.xm.ms.entity.domain.Content;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.spec.AttachmentSpec;
import com.icthh.xm.ms.entity.repository.AttachmentRepository;
import com.icthh.xm.ms.entity.repository.ContentRepository;
import com.icthh.xm.ms.entity.repository.backend.FsFileStorageRepository;
import com.icthh.xm.ms.entity.repository.backend.S3StorageRepository;
import com.icthh.xm.ms.entity.service.dto.S3ObjectDto;
import com.icthh.xm.ms.entity.service.dto.UploadResultDto;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ContentService {

    private static final long DEFAULT_EXPIRIBLE_LINK_TIME = 60000L;

    private final PermittedRepository permittedRepository;
    private final ContentRepository contentRepository;
    private final S3StorageRepository s3StorageRepository;
    private final FsFileStorageRepository fsFileStorageRepository;
    private final XmEntitySpecService xmEntitySpecService;
    private final ApplicationProperties applicationProperties;

    @Transactional(readOnly = true)
    @FindWithPermission("CONTENT.GET_LIST")
    @PrivilegeDescription("Privilege to get all the contents")
    public List<Content> findAll(String privilegeKey) {
        return permittedRepository.findAll(Content.class, privilegeKey);
    }

    public Attachment save(Attachment attachment, Content content) {
        // XmEntityServiceImpl.addFileAttachment(XmEntity entity, MultipartFile file) already save file
        if (attachment.getContentUrl() != null && content == null) {
            checkTenantContentUrl(attachment.getContentUrl());
            return attachment;
        }

        if (content == null) {
            throw new IllegalArgumentException("Attachment content is null!");
        }

        AttachmentSpec spec = getSpec(attachment.getXmEntity(), attachment);
        AttachmentStoreType storeType = spec.getStoreType() == null ? AttachmentStoreType.DB : spec.getStoreType();
        String folderName = attachment.getXmEntity().getTypeKey();

        return switch (storeType) {
            case S3 -> saveAttachmentToS3(attachment, content, folderName);
            case FS -> saveAttachmentToFS(attachment, content, folderName);
            default -> {
                Content savedContent = contentRepository.save(content);
                attachment.setContent(savedContent);
                attachment.setValueContentSize((long) content.getValue().length);
                attachment.setContentChecksum(DigestUtils.sha256Hex(content.getValue()));
                yield attachment;
            }
        };

    }

    /**
     * Deletes the attachment content from the storage set in the attachment spec. A content url of another storage is
     * rejected, so a client supplied url can not route the deletion to another storage.
     */
    public void delete(Attachment attachment) {
        switch (getStoreType(attachment)) {
            case S3 -> s3StorageRepository.delete(attachment.getContentUrl());
            case FS -> fsFileStorageRepository.delete(attachment.getContentUrl());
            default -> {}
        }
    }

    /**
     * Returns the store type set in the attachment spec. A content url of another storage is rejected.
     * With {@code application.secure-attachment-content-url: false} returns the store type of the content url.
     */
    private AttachmentStoreType getStoreType(Attachment attachment) {
        if (!applicationProperties.isSecureAttachmentContentUrl()) {
            return AttachmentStoreType.byContentUrl(attachment.getContentUrl());
        }
        AttachmentStoreType storeType = xmEntitySpecService
            .findAttachment(attachment.getXmEntity().getTypeKey(), attachment.getTypeKey())
            .map(AttachmentSpec::getStoreType)
            .orElse(AttachmentStoreType.DB);
        if (AttachmentStoreType.byContentUrl(attachment.getContentUrl()) != storeType) {
            throw new BusinessException("error.content.url.store.type",
                "Content url does not match the store type " + storeType + " of the attachment spec");
        }
        return storeType;
    }

    @SneakyThrows
    public Attachment enrichContent(Attachment attachment) {
        AttachmentSpec spec = getSpec(attachment.getXmEntity(), attachment);
        AttachmentStoreType storeType = spec.getStoreType() == null ? AttachmentStoreType.DB : spec.getStoreType();
        return switch (storeType) {
            case AttachmentStoreType.S3 -> enrichFromS3(attachment);
            case AttachmentStoreType.FS -> enrichFromFS(attachment);
            default -> {
                AttachmentRepository.enrich(attachment);
                yield attachment;
            }
        };
    }

    private Attachment saveAttachmentToS3(Attachment attachment, Content content, String folderName) {
        UploadResultDto uploadResult = s3StorageRepository.store(content, folderName, attachment.getName());
        attachment.setValueContentSize((long) content.getValue().length);
        attachment.setContentChecksum(uploadResult.getETag());
        attachment.setContentUrl(uploadResult.toXmContentName());
        return attachment;
    }

    private Attachment saveAttachmentToFS(Attachment attachment, Content content, String folderName) {
        UploadResultDto uploadResult = fsFileStorageRepository.store(content, folderName, attachment.getName());
        attachment.setValueContentSize((long) content.getValue().length);
        attachment.setContentChecksum(uploadResult.getETag());
        attachment.setContentUrl(uploadResult.toXmContentName());
        return attachment;
    }

    @SneakyThrows
    private Attachment enrichFromS3(Attachment attachment) {
        S3ObjectDto s3Object = s3StorageRepository.getS3Object(attachment.getContentUrl());
        Content content = attachment.getContent();
        if (content == null) {
            content = new Content();
            attachment.setContent(content);
        }

        content.setValue(IOUtils.toByteArray(s3Object.getObjectContent()));

        attachment.setValueContentType(s3Object.getContentType());
        attachment.setValueContentSize(s3Object.getContentLength());
        attachment.setContentChecksum(s3Object.getETag());
        return attachment;
    }

    @SneakyThrows
    private Attachment enrichFromFS(Attachment attachment) {
        Resource resource =  fsFileStorageRepository.getFileFromFs(attachment.getContentUrl());
        Content content = attachment.getContent();
        if (content == null) {
            content = new Content();
            attachment.setContent(content);
        }
        content.setValue(resource.getContentAsByteArray());
        attachment.setValueContentSize(resource.contentLength());
        return attachment;
    }

    private void checkTenantContentUrl(String contentUrl) {
        switch (AttachmentStoreType.byContentUrl(contentUrl)) {
            case S3 -> s3StorageRepository.checkTenantContentUrl(contentUrl);
            case FS -> fsFileStorageRepository.checkTenantContentUrl(contentUrl);
            default -> { }
        }
    }

    public String createExpirableLink(Attachment attachment) {
        AttachmentSpec spec = getSpec(attachment.getXmEntity(), attachment);
        Long expireLinkTime = Optional.ofNullable(spec.getExpireLinkTimeInMillis()).orElse(DEFAULT_EXPIRIBLE_LINK_TIME);
        return s3StorageRepository.createExpirableLink(attachment, expireLinkTime).toString();
    }

    public boolean supportDownloadLink (Attachment attachment) {
        return AttachmentStoreType.S3 == getStoreType(attachment);
    }

    private AttachmentSpec getSpec(XmEntity entity, Attachment attachment) {
        return xmEntitySpecService
            .findAttachment(entity.getTypeKey(), attachment.getTypeKey())
            .orElseThrow(
                () -> new EntityNotFoundException("Spec.Attachment not found for entity type key " + entity.getTypeKey()
                    + " and attachment key: " + attachment.getTypeKey())
            );
    }
}
