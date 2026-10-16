package com.icthh.xm.ms.entity.service;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.exceptions.EntityNotFoundException;
import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.commons.permission.annotation.FindWithPermission;
import com.icthh.xm.commons.permission.annotation.PrivilegeDescription;
import com.icthh.xm.commons.permission.repository.PermittedRepository;
import com.icthh.xm.ms.entity.config.ApplicationProperties;
import com.icthh.xm.ms.entity.domain.Attachment;
import com.icthh.xm.ms.entity.domain.AttachmentStoreType;
import com.icthh.xm.ms.entity.domain.Content;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.spec.AttachmentSpec;
import com.icthh.xm.ms.entity.lep.keyresolver.TypeKeyResolver;
import com.icthh.xm.ms.entity.repository.AttachmentRepository;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
import com.icthh.xm.ms.entity.service.impl.StartUpdateDateGenerationStrategy;
import com.icthh.xm.ms.entity.validator.AttachmentContentTypeValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.unit.DataSize;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;

import static com.google.common.collect.ImmutableMap.of;
import static com.icthh.xm.ms.entity.domain.AttachmentStoreType.DB;
import static com.icthh.xm.ms.entity.domain.AttachmentStoreType.byContentUrl;

/**
 * Service Implementation for managing Attachment.
 */
@Slf4j
@Service
@LepService(group = "service.attachment")
@Transactional
@RequiredArgsConstructor
public class AttachmentService extends TransactionPropagationService<AttachmentService> {

    public static final String ZERO_RESTRICTION = "error.attachment.zero";
    public static final String MAX_RESTRICTION = "error.attachment.max";
    public static final String SIZE_RESTRICTION = "error.attachment.size";
    public static final String CONTENT_TYPE_RESTRICTION = "error.attachment.contentType";

    private final AttachmentRepository attachmentRepository;
    private final ContentService contentService;

    private final PermittedRepository permittedRepository;

    private final StartUpdateDateGenerationStrategy startUpdateDateGenerationStrategy;

    private final XmEntityRepository xmEntityRepository;

    private final XmEntitySpecService xmEntitySpecService;
    private final AttachmentContentTypeValidator contentTypeValidator;
    private final ApplicationProperties applicationProperties;

    /**
     * Save an attachment received from a client (REST create/update). The client sets the content url only of a DB
     * attachment and only a url of the DB type (not {@code file://} or {@code bucket::key}), or keeps the stored one.
     * The FS/S3 content url is set by the storage when the content is saved and is kept on update, so a client can not
     * point the attachment to another stored file. Internal callers (LEP, addFileAttachment, import) use {@link #save}.
     * The check is switched off by {@code application.secure-attachment-content-url: false}.
     *
     * @param attachment the entity to save
     * @return the persisted entity
     */
    public Attachment saveFromClient(Attachment attachment) {
        if (applicationProperties.isSecureAttachmentContentUrl()) {
            checkClientContentUrl(attachment);
        }
        return self.save(attachment);
    }

    private void checkClientContentUrl(Attachment attachment) {
        if (attachment.getId() == null && StringUtils.isEmpty(attachment.getContentUrl())) {
            return;
        }
        // save reads the entity and the stored attachment again in this transaction from the persistence context
        XmEntity entity = xmEntityRepository.findById(attachment.getXmEntity().getId()).orElseThrow(
            () -> new EntityNotFoundException("No entity found by id: " + attachment.getXmEntity().getId())
        );
        AttachmentStoreType storeType = Optional.ofNullable(getSpec(entity, attachment).getStoreType())
            .orElse(DB);
        this.contentUrlDataBaseTypeOrNull(attachment, storeType);
        this.contentRequiredForAnyStoreExceptDb(attachment, storeType);
        this.clientCanNotUpdateContentUrlForAnyStoreExceptDb(attachment, storeType);
    }

    private void clientCanNotUpdateContentUrlForAnyStoreExceptDb(Attachment attachment, AttachmentStoreType storeType) {
        if (attachment.getId() == null || storeType == DB) {
            return;
        }

        String storedUrl = attachmentRepository.findById(attachment.getId()).map(Attachment::getContentUrl).orElse(null);
        if (!Objects.equals(attachment.getContentUrl(), storedUrl)) {
            log.warn("Content url of attachment id={} can not be changed by a client, the stored value is kept. "
                + "Set application.secure-attachment-content-url: false to restore the previous behaviour", attachment.getId());
        }
        attachment.setContentUrl(storedUrl);
    }

    private void contentRequiredForAnyStoreExceptDb(Attachment attachment, AttachmentStoreType storeType) {
        if (attachment.getId() == null && storeType != DB) {
            if (attachment.getContent() == null) {
                throw new BusinessException("error.content.url.not.allowed",
                    "Attachment of the " + storeType + " store type is created only with the content");
            }
            // the content url is set by the storage
            attachment.setContentUrl(null);
        }
    }

    private void contentUrlDataBaseTypeOrNull(Attachment attachment, AttachmentStoreType storeType) {
        Long id = attachment.getId();
        String clientUrl = attachment.getContentUrl();
        if (storeType == DB && byContentUrl(clientUrl) != DB && !isStoredContentUrl(id, clientUrl)) {
            throw new BusinessException("error.content.url.not.allowed",
                "Content url of the file storage can not be set by a client");
        }
    }

    private boolean isStoredContentUrl(Long id, String contentUrl) {
        return id != null && attachmentRepository.findById(id)
            .map(Attachment::getContentUrl)
            .filter(contentUrl::equals)
            .isPresent();
    }

    /**
     * Save a attachment.
     *
     * @param attachment the entity to save
     * @return the persisted entity
     */
    @LogicExtensionPoint("Save")
    public Attachment save(Attachment attachment) {
        Objects.nonNull(attachment);
        Objects.nonNull(attachment.getXmEntity());
        Objects.nonNull(attachment.getXmEntity().getId());
        startUpdateDateGenerationStrategy.preProcessStartDate(attachment,
                                                              attachment.getId(),
                                                              attachmentRepository,
                                                              Attachment::setStartDate,
                                                              Attachment::getStartDate);

        XmEntity entity = xmEntityRepository.findById(attachment.getXmEntity().getId()).orElseThrow(
            () -> new EntityNotFoundException("No entity found by id: " + attachment.getXmEntity().getId())
        );


        AttachmentSpec spec = getSpec(entity, attachment);

        // check file size by spec
        assertFileSize(spec, attachment.getContent());

        // check content by content type spec
        assertContentType(spec, attachment);

        //check only for addingNew
        if (attachment.getId() == null && spec.getMax() != null) {
            //forbid to add element if spec.max = 0
            assertZeroRestriction(spec);
            //forbid to add element if spec.max <= addedSize
            assertLimitRestriction(spec, entity);
        }

        attachment.setXmEntity(entity);

        Content content = null;
        if (attachment.getContent() != null) {
            content = attachment.getContent();
            attachment.setContent(null);
        }

        Attachment savedAttachment = attachmentRepository.save(attachment);
        savedAttachment = contentService.save(savedAttachment, content);
        return attachmentRepository.save(savedAttachment);
    }

    /**
     *  Get all the attachments.
     *
     *  @return the list of entities
     */
    @Transactional(readOnly = true)
    @FindWithPermission("ATTACHMENT.GET_LIST")
    @PrivilegeDescription("Privilege to get all the attachments")
    public List<Attachment> findAll(String privilegeKey) {
        return permittedRepository.findAll(Attachment.class, privilegeKey);
    }

    @Transactional(readOnly = true)
    @FindWithPermission("ATTACHMENT.GET_LIST.BY_XM_ENTITY")
    @LogicExtensionPoint(value = "FindByXmEntity", resolver = TypeKeyResolver.class)
    @PrivilegeDescription("Privilege to search for the attachment by xmEntity id and typeKey")
    public Page<Attachment> findByXmEntity(Long id, String typeKey, Pageable pageable, String privilegeKey) {
        return permittedRepository.findByCondition("returnObject.xmEntity.id = :id and returnObject.typeKey = :typeKey",
            of("id", id, "typeKey", typeKey), pageable, Attachment.class, privilegeKey);
    }

    /**
     *  Get one attachment by id.
     *
     * @param id the id of the entity
     * @return the entity
     *
     * @deprecated: use getOneWithContent to avoid null check
     */
    @Transactional(readOnly = true)
    @Deprecated
    public Attachment findOneWithContent(Long id) {
        return getOneWithContent(id).orElse(null);
    }

    /**
     *  Get one attachment by id.
     *
     * @param id the id of the entity
     * @return attachment
     *
     * @Deprecated: use #getOneWithContent to avoid null check
     */
    @Transactional(readOnly = true)
    public Optional<Attachment> getOneWithContent(Long id) {
        return attachmentRepository.findById(id)
            .map(contentService::enrichContent);
    }

    /**
     * Get one attachment by id with content.
     *
     * @param id the id of the entity
     * @return the entity
     *
     * @deprecated: use #getOne to avoid null check
     */
    @Transactional(readOnly = true)
    @Deprecated
    public Attachment findOne(Long id) {
        return findById(id).orElse(null);
    }

    /**
     * Get one attachment by id with content.
     *
     * @param id the id of the entity
     * @return the entity
     */
    @Transactional(readOnly = true)
    public Optional<Attachment> findById(Long id) {
        return attachmentRepository.findById(id);
    }

    /**
     *  Delete the  attachment by id.
     *
     *  @param id the id of the entity
     */
    @LogicExtensionPoint("Delete")
    public void delete(Long id) {
        findById(id).ifPresent(attachment -> {
            contentService.delete(attachment);
            attachmentRepository.deleteById(attachment.getId());
        });
    }

    public void deleteAll(Iterable<Attachment> entities) {
        attachmentRepository.deleteAll(entities);
    }

    public Page<Attachment> findAll(Specification<Attachment> spec, Pageable pageable) {
        return attachmentRepository.findAll(spec, pageable);
    }

    public List<Attachment> saveAll(List<Attachment> list) {
        return attachmentRepository.saveAll(list);
    }

    public void deleteInBatch(List<Attachment> list) {
        attachmentRepository.deleteInBatch(list);
    }

    protected AttachmentSpec getSpec(XmEntity entity, Attachment attachment) {
        Objects.nonNull(entity);
        return xmEntitySpecService
            .findAttachment(entity.getTypeKey(), attachment.getTypeKey())
            .orElseThrow(
                () -> new EntityNotFoundException("Spec.Attachment not found for entity type key " + entity.getTypeKey()
                    + " and attachment key: " + attachment.getTypeKey())
            );
    }

    protected void assertZeroRestriction(AttachmentSpec spec) {
        if (Integer.valueOf(0).equals(spec.getMax())) {
            throw new BusinessException(ZERO_RESTRICTION, "Spec for " + spec.getKey() + " allows to add " + spec.getMax() + " elements");
        }
    }

    protected void assertLimitRestriction(AttachmentSpec spec, XmEntity entity) {
        if (attachmentRepository.countByXmEntityIdAndTypeKey(entity.getId(), spec.getKey()) >= spec.getMax()) {
            throw new BusinessException(MAX_RESTRICTION, "Spec for " + spec.getKey() + " allows to add " + spec.getMax() + " elements");
        }
    }


    protected void assertFileSize(AttachmentSpec spec, Content content) {
        if (content == null || content.getValue() == null || content.getValue().length == 0) {
            return;
        }
        DataSize dataSize = DataSize.parse(spec.getSize());

        if (dataSize.toBytes() < content.getValue().length) {
            throw new BusinessException(SIZE_RESTRICTION, "Spec for " + spec.getKey() + " allows to add file max size " + spec.getSize());
        }
    }

    protected void assertContentType(AttachmentSpec spec, Attachment attachment) {
        if (!contentTypeValidator.isValid(attachment, null)) {
            throw new BusinessException(CONTENT_TYPE_RESTRICTION,
                    "Attachment content type is not allowed for " + spec.getKey());
        }
    }

    @Transactional(readOnly = true)
    public String getAttachmentDownloadLink(Long id) {
        return findById(id)
            .filter(contentService::supportDownloadLink)
            .map(contentService::createExpirableLink)
            .orElseThrow(() -> new EntityNotFoundException("Attachment not found by id" + id));
    }
}
