package com.icthh.xm.ms.entity.service;

import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.commons.permission.annotation.FindWithPermission;
import com.icthh.xm.commons.permission.repository.PermittedRepository;
import com.icthh.xm.ms.entity.domain.Attachment;
import com.icthh.xm.ms.entity.repository.AttachmentRepository;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
import com.icthh.xm.ms.entity.repository.search.AttachmentSearchRepository;
import com.icthh.xm.ms.entity.repository.search.PermittedSearchRepository;
import com.icthh.xm.ms.entity.service.impl.StartUpdateDateGenerationStrategy;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service Implementation for managing Attachment.
 */
@Service
@LepService(group = "service.attachment")
@Transactional
@RequiredArgsConstructor
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;

    private final AttachmentSearchRepository attachmentSearchRepository;

    private final PermittedRepository permittedRepository;

    private final PermittedSearchRepository permittedSearchRepository;

    private final StartUpdateDateGenerationStrategy startUpdateDateGenerationStrategy;

    private final XmEntityRepository xmEntityRepository;

    /**
     * Save a attachment.
     *
     * @param attachment the entity to save
     * @return the persisted entity
     */
    @LogicExtensionPoint("Save")
    public Attachment save(Attachment attachment) {

        startUpdateDateGenerationStrategy.preProcessStartDate(attachment,
                                                              attachment.getId(),
                                                              attachmentRepository,
                                                              Attachment::setStartDate,
                                                              Attachment::getStartDate);
        attachment.setXmEntity(xmEntityRepository.getOne(attachment.getXmEntity().getId()));
        Attachment result = attachmentRepository.save(attachment);
        attachmentSearchRepository.save(result);
        return result;
    }

    /**
     *  Get all the attachments.
     *
     *  @return the list of entities
     */
    @Transactional(readOnly = true)
    @FindWithPermission("ATTACHMENT.GET_LIST")
    public List<Attachment> findAll(String privilegeKey) {
        return permittedRepository.findAll(Attachment.class, privilegeKey);
    }

    /**
     *  Get one attachment by id.
     *
     * @param id the id of the entity
     * @return the entity
     */
    @Transactional(readOnly = true)
    public Attachment findOneWithContent(Long id) {
        Attachment attachment = attachmentRepository.findOne(id);
        if (attachment != null) {
            Hibernate.initialize(attachment.getContent());
        }
        return attachment;
    }

    /**
     * Get one attachment by id with content.
     *
     * @param id the id of the entity
     * @return the entity
     */
    @Transactional(readOnly = true)
    public Attachment findOne(Long id) {
        return attachmentRepository.findOne(id);
    }

    /**
     *  Delete the  attachment by id.
     *
     *  @param id the id of the entity
     */
    @LogicExtensionPoint("Delete")
    public void delete(Long id) {
        attachmentRepository.delete(id);
        attachmentSearchRepository.delete(id);
    }

    /**
     * Search for the attachment corresponding to the query.
     *
     *  @param query the query of the search
     *  @return the list of entities
     */
    @Transactional(readOnly = true)
    @FindWithPermission("ATTACHMENT.SEARCH")
    public List<Attachment> search(String query, String privilegeKey) {
        return permittedSearchRepository.search(query, Attachment.class, privilegeKey);
    }
}
