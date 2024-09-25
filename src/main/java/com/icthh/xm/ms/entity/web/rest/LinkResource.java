package com.icthh.xm.ms.entity.web.rest;

import com.codahale.metrics.annotation.Timed;
import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.exceptions.ErrorConstants;
import com.icthh.xm.commons.permission.annotation.PrivilegeDescription;
import com.icthh.xm.ms.entity.domain.Link;
import com.icthh.xm.ms.entity.service.LinkService;
import com.icthh.xm.ms.entity.service.TransactionPropagationService;
import com.icthh.xm.ms.entity.web.rest.util.HeaderUtil;
import com.icthh.xm.ms.entity.web.rest.util.PaginationUtil;
import com.icthh.xm.ms.entity.web.rest.util.RespContentUtil;
import io.swagger.annotations.ApiParam;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
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
 * REST controller for managing Link.
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class LinkResource extends TransactionPropagationService<LinkResource> {

    private static final String ENTITY_NAME = "link";

    private final LinkService linkService;

    /**
     * POST  /links : Create a new link.
     *
     * @param link the link to create
     * @return the ResponseEntity with status 201 (Created) and with body the new link, or with status 400 (Bad Request) if the link has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/links")
    @Timed
    @PreAuthorize("hasPermission({'link': #link}, 'LINK.CREATE')")
    @PrivilegeDescription("Privilege to create a new link")
    public ResponseEntity<Link> createLink(@Valid @RequestBody Link link) throws URISyntaxException {
        if (link.getId() != null) {
            throw new BusinessException(ErrorConstants.ERR_BUSINESS_IDEXISTS,
                                        "A new link cannot already have an ID");
        }
        Link result = linkService.save(link);
        return ResponseEntity.created(new URI("/api/links/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * PUT  /links : Updates an existing link.
     *
     * @param link the link to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated link,
     * or with status 400 (Bad Request) if the link is not valid,
     * or with status 500 (Internal Server Error) if the link couldn't be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PutMapping("/links")
    @Timed
    @PreAuthorize("hasPermission({'id': #link.id, 'newLink': #link}, 'link', 'LINK.UPDATE')")
    @PrivilegeDescription("Privilege to updates an existing link")
    public ResponseEntity<Link> updateLink(@Valid @RequestBody Link link) throws URISyntaxException {
        if (link.getId() == null) {
            //in order to call method with permissions check
            return self.createLink(link);
        }
        Link result = linkService.save(link);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(ENTITY_NAME, link.getId().toString()))
            .body(result);
    }

    /**
     * GET  /links : get all the links.
     *
     * @param pageable the pagination information
     * @return the ResponseEntity with status 200 (OK) and the list of links in body
     */
    @GetMapping("/links")
    @Timed
    public ResponseEntity<List<Link>> getAllLinks(@ApiParam Pageable pageable) {
        Page<Link> page = linkService.findAll(pageable, null);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/links");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    /**
     * GET  /links/:id : get the "id" link.
     *
     * @param id the id of the link to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the link, or with status 404 (Not Found)
     */
    @GetMapping("/links/{id}")
    @Timed
    @PostAuthorize("hasPermission({'returnObject': returnObject.body}, 'LINK.GET_LIST.ITEM')")
    @PrivilegeDescription("Privilege to get the link by id")
    public ResponseEntity<Link> getLink(@PathVariable Long id) {
        Link link = linkService.findOne(id);
        return RespContentUtil.wrapOrNotFound(Optional.ofNullable(link));
    }

    /**
     * DELETE  /links/:id : delete the "id" link.
     *
     * @param id the id of the link to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/links/{id}")
    @Timed
    @PreAuthorize("hasPermission({'id': #id}, 'link', 'LINK.DELETE')")
    @PrivilegeDescription("Privilege to delete the link by id")
    public ResponseEntity<Void> deleteLink(@PathVariable Long id) {
        linkService.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(ENTITY_NAME, id.toString())).build();
    }
}
