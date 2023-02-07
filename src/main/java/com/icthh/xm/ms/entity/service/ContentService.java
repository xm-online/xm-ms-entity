package com.icthh.xm.ms.entity.service;

import com.amazonaws.util.IOUtils;
import com.icthh.xm.commons.exceptions.EntityNotFoundException;
import com.icthh.xm.commons.permission.annotation.FindWithPermission;
import com.icthh.xm.commons.permission.annotation.PrivilegeDescription;
import com.icthh.xm.commons.permission.repository.PermittedRepository;
import com.icthh.xm.ms.entity.domain.Attachment;
import com.icthh.xm.ms.entity.domain.AttachmentStoreType;
import com.icthh.xm.ms.entity.domain.Content;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.spec.AttachmentSpec;
import com.icthh.xm.ms.entity.repository.AttachmentRepository;
import com.icthh.xm.ms.entity.repository.ContentRepository;
import com.icthh.xm.ms.entity.repository.backend.S3StorageRepository;
import com.icthh.xm.ms.entity.service.dto.S3ObjectDto;
import com.icthh.xm.ms.entity.service.dto.UploadResultDto;
import com.icthh.xm.ms.entity.util.FileUtils;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
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
    private final XmEntitySpecService xmEntitySpecService;

    @Transactional(readOnly = true)
    @FindWithPermission("CONTENT.GET_LIST")
    @PrivilegeDescription("Privilege to get all the contents")
    public List<Content> findAll(String privilegeKey) {
        return permittedRepository.findAll(Content.class, privilegeKey);
    }

    public Attachment save(Attachment attachment, Content content) {
        // XmEntityServiceImpl.addFileAttachment(XmEntity entity, MultipartFile file) already save file
        if (attachment.getContentUrl() != null && content == null) {
            return attachment;
        }

        if (content == null) {
            throw new IllegalArgumentException("Attachment content is null!");
        }

        AttachmentSpec spec = getSpec(attachment.getXmEntity(), attachment);
        if (spec.getStoreType() == AttachmentStoreType.S3) {
            String folderName = attachment.getXmEntity().getTypeKey();
            UploadResultDto uploadResult = s3StorageRepository.store(content, folderName, attachment.getName());
            attachment.setValueContentSize((long) content.getValue().length);
            attachment.setContentChecksum(uploadResult.getETag());
            attachment.setContentUrl(s3FileName(uploadResult));
        } else {
            Content savedContent = contentRepository.save(content);
            attachment.setContent(savedContent);
            attachment.setValueContentSize((long) content.getValue().length);
            attachment.setContentChecksum(DigestUtils.sha256Hex(content.getValue()));
        }

        return attachment;
    }

    private String s3FileName(UploadResultDto uploadResult) {
        return uploadResult.getBucketName() + FileUtils.FILE_NAME_SEPARATOR + uploadResult.getKey();
    }

    public void delete(Attachment attachment) {
        if (isS3Compatible(attachment.getContentUrl())) {
            Pair<String, String> s3BucketNameKey = FileUtils.getS3BucketNameKey(attachment.getContentUrl());
            s3StorageRepository.delete(s3BucketNameKey.getKey(), s3BucketNameKey.getValue());
        }
    }

    @SneakyThrows
    public Attachment enrichContent(Attachment attachment) {
        AttachmentSpec spec = getSpec(attachment.getXmEntity(), attachment);
        if (spec.getStoreType() == AttachmentStoreType.S3) {
            Pair<String, String> s3BucketNameKey = FileUtils.getS3BucketNameKey(attachment.getContentUrl());
            S3ObjectDto s3Object = s3StorageRepository.getS3Object(s3BucketNameKey.getKey(), s3BucketNameKey.getValue());
            Content content = attachment.getContent();
            if (content == null) {
                content = new Content();
                attachment.setContent(content);
            }

            content.setValue(IOUtils.toByteArray(s3Object.getObjectContent()));

            attachment.setValueContentType(s3Object.getContentType());
            attachment.setValueContentSize(s3Object.getContentLength());
            attachment.setContentChecksum(s3Object.getETag());
        } else {
            AttachmentRepository.enrich(attachment);
        }

        return attachment;
    }

    public String createExpirableLink(Attachment attachment) {
        AttachmentSpec spec = getSpec(attachment.getXmEntity(), attachment);
        Long expireLinkTime = Optional.ofNullable(spec.getExpireLinkTimeInMillis()).orElse(DEFAULT_EXPIRIBLE_LINK_TIME);
        return s3StorageRepository.createExpirableLink(attachment, expireLinkTime).toString();
    }

    public boolean supportDownloadLink (Attachment attachment) {
        return isS3Compatible(attachment.getContentUrl());
    }

    private boolean isS3Compatible(String contentUrl) {
        return StringUtils.contains(contentUrl, FileUtils.FILE_NAME_SEPARATOR);
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
