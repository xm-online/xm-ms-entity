package com.icthh.xm.ms.entity.service;

import static org.elasticsearch.index.query.QueryBuilders.*;

import com.icthh.xm.ms.entity.domain.Attachment;
import com.icthh.xm.ms.entity.repository.AttachmentRepository;
import com.icthh.xm.ms.entity.repository.search.AttachmentSearchRepository;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing Attachment.
 */
@Slf4j
@Service
@Transactional
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;

    private final AttachmentSearchRepository attachmentSearchRepository;

    public AttachmentService(AttachmentRepository attachmentRepository, AttachmentSearchRepository attachmentSearchRepository) {
        this.attachmentRepository = attachmentRepository;
        this.attachmentSearchRepository = attachmentSearchRepository;
    }

    /**
     * Save a attachment.
     *
     * @param attachment the entity to save
     * @return the persisted entity
     */
    public Attachment save(Attachment attachment) {
        log.debug("Request to save Attachment : {}", attachment);
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
    public List<Attachment> findAll() {
        log.debug("Request to get all Attachments");
        return attachmentRepository.findAll();
    }

    /**
     *  Get one attachment by id.
     *
     * @param id the id of the entity
     * @return the entity
     */
    @Transactional(readOnly = true)
    public Attachment findOneWithContent(Long id) {
        log.debug("Request to get Attachment : {}", id);
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
        log.debug("Request to get Attachment : {}", id);
        return attachmentRepository.findOne(id);
    }

    /**
     *  Delete the  attachment by id.
     *
     *  @param id the id of the entity
     */
    public void delete(Long id) {
        log.debug("Request to delete Attachment : {}", id);
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
    public List<Attachment> search(String query) {
        log.debug("Request to search Attachments for query {}", query);
        return StreamSupport
            .stream(attachmentSearchRepository.search(queryStringQuery(query)).spliterator(), false)
            .collect(Collectors.toList());
    }
}
