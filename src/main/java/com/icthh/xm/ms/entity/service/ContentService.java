package com.icthh.xm.ms.entity.service;

import com.icthh.xm.commons.permission.annotation.FindWithPermission;
import com.icthh.xm.commons.permission.annotation.PrivilegeDescription;
import com.icthh.xm.commons.permission.repository.PermittedRepository;
import com.icthh.xm.ms.entity.domain.Attachment;
import com.icthh.xm.ms.entity.domain.AttachmentStoreType;
import com.icthh.xm.ms.entity.domain.Content;
import com.icthh.xm.ms.entity.domain.spec.AttachmentSpec;
import com.icthh.xm.ms.entity.repository.AttachmentRepository;
import com.icthh.xm.ms.entity.repository.ContentRepository;
import com.icthh.xm.ms.entity.repository.backend.S3StorageRepository;
import com.icthh.xm.ms.entity.service.dto.UploadResultDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ContentService {

    private static final String FILE_NAME_SEPARATOR = "::";

    private final PermittedRepository permittedRepository;
    private final ContentRepository contentRepository;
    private final S3StorageRepository s3StorageRepository;

    @Transactional(readOnly = true)
    @FindWithPermission("CONTENT.GET_LIST")
    @PrivilegeDescription("Privilege to get all the contents")
    public List<Content> findAll(String privilegeKey) {
        return permittedRepository.findAll(Content.class, privilegeKey);
    }

    public Attachment save(AttachmentSpec spec, Attachment attachment, Content content) {
        // XmEntityServiceImpl.addFileAttachment(XmEntity entity, MultipartFile file) already save file
        if (attachment.getContentUrl() != null && content == null) {
            return attachment;
        }

        if (content == null) {
            throw new IllegalArgumentException("Attachment content is null!");
        }

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
        return uploadResult.getBucketName() + FILE_NAME_SEPARATOR + uploadResult.getKey();
    }

    public void delete(AttachmentSpec spec, Attachment attachment) {
        if (spec.getStoreType() == AttachmentStoreType.S3) {
            Pair<String, String> s3BucketNameKey = getS3BucketNameKey(attachment.getContentUrl());
            s3StorageRepository.delete(s3BucketNameKey.getKey(), s3BucketNameKey.getValue());
        }
    }

    public Attachment enrichContent(AttachmentSpec spec, Attachment attachment) {
        if (spec.getStoreType() == AttachmentStoreType.S3) {
            attachment.setContentUrl(createExpirableLink(attachment.getContentUrl()));
        } else {
            AttachmentRepository.enrich(attachment);
        }

        return attachment;
    }

    private String createExpirableLink(String contentUrl) {
        Pair<String, String> s3BucketNameKey = getS3BucketNameKey(contentUrl);
        return s3StorageRepository.createExpirableLink(s3BucketNameKey.getKey(), s3BucketNameKey.getValue()).toString();
    }

    private Pair<String, String> getS3BucketNameKey(String contentUrl) {
        String[] split = contentUrl.split(FILE_NAME_SEPARATOR);
        if (split.length != 2) {
            throw new IllegalArgumentException("Invalid format for link = " + contentUrl);
        }

        return Pair.of(split[0], split[1]);
    }
}
