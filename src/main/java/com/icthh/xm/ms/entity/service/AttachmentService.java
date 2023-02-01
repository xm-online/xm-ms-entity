package com.icthh.xm.ms.entity.service;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.exceptions.EntityNotFoundException;
import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.commons.permission.annotation.FindWithPermission;
import com.icthh.xm.commons.permission.repository.PermittedRepository;
import com.icthh.xm.commons.permission.annotation.PrivilegeDescription;
import com.icthh.xm.ms.entity.domain.Attachment;
import com.icthh.xm.ms.entity.domain.Content;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.spec.AttachmentSpec;
import com.icthh.xm.ms.entity.repository.AttachmentRepository;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
import com.icthh.xm.ms.entity.repository.search.PermittedSearchRepository;
import com.icthh.xm.ms.entity.service.impl.StartUpdateDateGenerationStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.unit.DataSize;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Service Implementation for managing Attachment.
 */
@Service
@LepService(group = "service.attachment")
@Transactional
@RequiredArgsConstructor
public class AttachmentService {

    public static final String ZERO_RESTRICTION = "error.attachment.zero";
    public static final String MAX_RESTRICTION = "error.attachment.max";
    public static final String SIZE_RESTRICTION = "error.attachment.size";

    private final AttachmentRepository attachmentRepository;
    private final ContentService contentService;

    private final PermittedRepository permittedRepository;

    private final PermittedSearchRepository permittedSearchRepository;

    private final StartUpdateDateGenerationStrategy startUpdateDateGenerationStrategy;

    private final XmEntityRepository xmEntityRepository;

    private final XmEntitySpecService xmEntitySpecService;

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

    /**
     * Search for the attachment corresponding to the query.
     *
     *  @param query the query of the search
     *  @return the list of entities
     */
    @Deprecated
    @Transactional(readOnly = true)
    @FindWithPermission("ATTACHMENT.SEARCH")
    @PrivilegeDescription("Privilege to search for the attachment corresponding to the query")
    public List<Attachment> search(String query, String privilegeKey) {
        return permittedSearchRepository.search(query, Attachment.class, privilegeKey);
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
        if (content.getValue() == null || content.getValue().length == 0) {
            return;
        }
        DataSize dataSize = DataSize.parse(spec.getSize());

        if (dataSize.toBytes() < content.getValue().length) {
            throw new BusinessException(SIZE_RESTRICTION, "Spec for " + spec.getKey() + " allows to add file max size " + spec.getSize());
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
