package com.icthh.xm.ms.entity.web.rest;

import com.codahale.metrics.annotation.Timed;
import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.exceptions.ErrorConstants;
import com.icthh.xm.ms.entity.domain.Content;
import com.icthh.xm.ms.entity.repository.ContentRepository;
import com.icthh.xm.ms.entity.service.ContentService;
import com.icthh.xm.ms.entity.web.rest.util.HeaderUtil;
import com.icthh.xm.ms.entity.web.rest.util.RespContentUtil;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import javax.validation.Valid;

/**
 * REST controller for managing Content.
 */
@RestController
@RequestMapping("/api")
public class ContentResource {

    private static final String ENTITY_NAME = "content";

    private final ContentRepository contentRepository;
    private final ContentService contentService;
    private final ContentResource contentResource;

    public ContentResource(
                    ContentRepository contentRepository,
                    ContentService contentService,
                    @Lazy ContentResource contentResource) {
        this.contentRepository = contentRepository;
        this.contentService = contentService;
        this.contentResource = contentResource;
    }

    /**
     * POST  /contents : Create a new content.
     *
     * @param content the content to create
     * @return the ResponseEntity with status 201 (Created) and with body the new content, or with status 400 (Bad Request) if the content has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/contents")
    @Timed
    @PreAuthorize("hasPermission({'content': #content}, 'CONTENT.CREATE')")
    public ResponseEntity<Content> createContent(@Valid @RequestBody Content content) throws URISyntaxException {
        if (content.getId() != null) {
            throw new BusinessException(ErrorConstants.ERR_BUSINESS_IDEXISTS,
                                        "A new content cannot already have an ID");
        }
        Content result = contentRepository.save(content);
        return ResponseEntity.created(new URI("/api/contents/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * PUT  /contents : Updates an existing content.
     *
     * @param content the content to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated content,
     * or with status 400 (Bad Request) if the content is not valid,
     * or with status 500 (Internal Server Error) if the content couldn't be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PutMapping("/contents")
    @Timed
    @PreAuthorize("hasPermission({'id': #content.id, 'newContent': #content}, 'content', 'CONTENT.UPDATE')")
    public ResponseEntity<Content> updateContent(@Valid @RequestBody Content content) throws URISyntaxException {
        if (content.getId() == null) {
            //in order to call method with permissions check
            return this.contentResource.createContent(content);
        }
        Content result = contentRepository.save(content);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(ENTITY_NAME, content.getId().toString()))
            .body(result);
    }

    /**
     * GET  /contents : get all the contents.
     *
     * @return the ResponseEntity with status 200 (OK) and the list of contents in body
     */
    @GetMapping("/contents")
    @Timed
    public List<Content> getAllContents() {
        return contentService.findAll(null);
    }

    /**
     * GET  /contents/:id : get the "id" content.
     *
     * @param id the id of the content to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the content, or with status 404 (Not Found)
     */
    @GetMapping("/contents/{id}")
    @Timed
    @PostAuthorize("hasPermission({'returnObject': returnObject.body}, 'CONTENT.GET_LIST.ITEM')")
    public ResponseEntity<Content> getContent(@PathVariable Long id) {
        Content content = contentRepository.findOne(id);
        return RespContentUtil.wrapOrNotFound(Optional.ofNullable(content));
    }

    /**
     * DELETE  /contents/:id : delete the "id" content.
     *
     * @param id the id of the content to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/contents/{id}")
    @Timed
    @PreAuthorize("hasPermission({'id': #id}, 'content', 'CONTENT.DELETE')")
    public ResponseEntity<Void> deleteContent(@PathVariable Long id) {
        contentRepository.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(ENTITY_NAME, id.toString())).build();
    }

}
