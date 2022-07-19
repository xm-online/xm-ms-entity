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
import com.icthh.xm.ms.entity.repository.backend.AwsStorageRepository;
import com.icthh.xm.ms.entity.service.dto.UploadResultDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
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
    private final AwsStorageRepository awsStorageRepository;

    @Transactional(readOnly = true)
    @FindWithPermission("CONTENT.GET_LIST")
    @PrivilegeDescription("Privilege to get all the contents")
    public List<Content> findAll(String privilegeKey) {
        return permittedRepository.findAll(Content.class, privilegeKey);
    }

    public Attachment save(AttachmentSpec spec, Attachment attachment, Content content) {
        if (content == null) {
            log.warn("Content is null!");
            return attachment;
        }
        if (spec.getStoreType() == AttachmentStoreType.AWS) {
            UploadResultDto uploadResult = awsStorageRepository.store(content, attachment.getName());
            attachment.setValueContentSize((long) uploadResult.getETag().getBytes().length);
            attachment.setContentChecksum(DigestUtils.sha256Hex(uploadResult.getETag().getBytes()));
            attachment.setContentUrl(uploadResult.getBucketName() + FILE_NAME_SEPARATOR + uploadResult.getKey());
        } else {
            Content savedContent = contentRepository.save(content);
            attachment.setContent(savedContent);
            attachment.setValueContentSize((long) content.getValue().length);
            attachment.setContentChecksum(DigestUtils.sha256Hex(content.getValue()));
        }

        return attachment;
    }

    public void delete(AttachmentSpec spec, Attachment attachment) {
        if (spec.getStoreType() == AttachmentStoreType.AWS) {
            String[] split = attachment.getContentUrl().split(FILE_NAME_SEPARATOR);
            if (split.length != 2) {
                throw new IllegalArgumentException("");
            }
            String bucket = split[0];
            String key = split[1];
            awsStorageRepository.delete(bucket, key);
        }
    }

    public Attachment enrichContent(AttachmentSpec spec, Attachment attachment) {
        if (spec.getStoreType() == AttachmentStoreType.AWS) {
            attachment.setContentUrl(createExpirableLink(attachment.getContentUrl()));
        } else {
            AttachmentRepository.enrich(attachment);
        }

        return attachment;
    }

    private String createExpirableLink(String contentUrl) {
        String[] split = contentUrl.split(FILE_NAME_SEPARATOR);
        if (split.length != 2) {
            throw new IllegalArgumentException("");
        }
        String bucket = split[0];
        String key = split[1];
        return awsStorageRepository.createExpirableLink(bucket, key).toString();
    }
}
