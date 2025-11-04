package com.icthh.xm.ms.entity.web.rest;

import com.codahale.metrics.annotation.Timed;
import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.exceptions.ErrorConstants;
import com.icthh.xm.commons.permission.annotation.PrivilegeDescription;
import com.icthh.xm.ms.entity.domain.Attachment;
import com.icthh.xm.ms.entity.service.AttachmentService;
import com.icthh.xm.ms.entity.web.rest.util.HeaderUtil;
import com.icthh.xm.ms.entity.web.rest.util.RespContentUtil;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

/**
 * REST controller for managing Attachment.
 */
@RestController
@RequestMapping("/api")
public class AttachmentResource {

    private static final String ENTITY_NAME = "attachment";

    private final AttachmentService attachmentService;
    private final AttachmentResource attachmentResource;

    public AttachmentResource(
                    AttachmentService attachmentService,
                    @Lazy AttachmentResource attachmentResource) {
        this.attachmentService = attachmentService;
        this.attachmentResource = attachmentResource;
    }

    /**
     * POST  /attachments : Create a new attachment.
     *
     * @param attachment the attachment to create
     * @return the ResponseEntity with status 201 (Created) and with body the new attachment, or with status 400 (Bad Request) if the attachment has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/attachments")
    @Timed
    @PreAuthorize("hasPermission({'attachment': #attachment}, 'ATTACHMENT.CREATE')")
    @PrivilegeDescription("Privilege to create a new attachment")
    public ResponseEntity<Attachment> createAttachment(@Valid @RequestBody Attachment attachment) throws URISyntaxException {
        if (attachment.getId() != null) {
            throw new BusinessException(ErrorConstants.ERR_BUSINESS_IDEXISTS,
                "A new attachment cannot already have an ID");
        }
        Attachment result = attachmentService.save(attachment);
        return ResponseEntity.created(new URI("/api/attachments/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * PUT  /attachments : Updates an existing attachment.
     *
     * @param attachment the attachment to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated attachment,
     * or with status 400 (Bad Request) if the attachment is not valid,
     * or with status 500 (Internal Server Error) if the attachment couldn't be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PutMapping("/attachments")
    @Timed
    @PreAuthorize("hasPermission({'id': #attachment.id, 'newAttachment': #attachment}, 'attachment', 'ATTACHMENT.UPDATE')")
    @PrivilegeDescription("Privilege to update an existing attachment")
    public ResponseEntity<Attachment> updateAttachment(@Valid @RequestBody Attachment attachment) throws URISyntaxException {
        if (attachment.getId() == null) {
            //in order to call method with permissions check
            return this.attachmentResource.createAttachment(attachment);
        }
        Attachment result = attachmentService.save(attachment);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(ENTITY_NAME, attachment.getId().toString()))
            .body(result);
    }

    /**
     * GET  /attachments : get all the attachments.
     *
     * @return the ResponseEntity with status 200 (OK) and the list of attachments in body
     */
    @GetMapping("/attachments")
    @Timed
    public List<Attachment> getAllAttachments() {
        return attachmentService.findAll(null);
    }

    /**
     * GET  /attachments/:id : get the "id" attachment.
     *
     * @param id the id of the attachment to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the attachment, or with status 404 (Not Found)
     */
    @GetMapping("/attachments/{id}")
    @Timed
    @PostAuthorize("hasPermission({'returnObject': returnObject.body}, 'ATTACHMENT.GET_LIST.ITEM')")
    @PrivilegeDescription("Privilege to get attachment by id")
    public ResponseEntity<Attachment> getAttachment(@PathVariable Long id) {
        Optional<Attachment> attachment = attachmentService.getOneWithContent(id);
        return RespContentUtil.wrapOrNotFound(attachment);
    }

    /**
     * DELETE  /attachments/:id : delete the "id" attachment.
     *
     * @param id the id of the attachment to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/attachments/{id}")
    @Timed
    @PreAuthorize("hasPermission({'id': #id}, 'attachment', 'ATTACHMENT.DELETE')")
    @PrivilegeDescription("Privilege to delete attachment by id")
    public ResponseEntity<Void> deleteAttachment(@PathVariable Long id) {
        attachmentService.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(ENTITY_NAME, id.toString())).build();
    }

    /**
     * GET  /attachments/:id : get the "id" attachment.
     *
     * @param id the id of the attachment to retrieve
     * @return the ResponseEntity with status 200 (OK) and url for attachment download, or with status 404 (Not Found)
     */
    @GetMapping("/attachments/{id}/download-link")
    @Timed
    @PostAuthorize("hasPermission({'returnObject': returnObject.body}, 'ATTACHMENT.GET_LIST.ITEM')")
    @PrivilegeDescription("Privilege to get attachment by id")
    public ResponseEntity<String> getAttachmentDownloadLink(@PathVariable Long id) {
        String downloadLink = attachmentService.getAttachmentDownloadLink(id);
        return RespContentUtil.wrapOrNotFound(Optional.ofNullable(downloadLink));
    }

}
