package com.icthh.xm.ms.entity.web.rest;

import com.codahale.metrics.annotation.Timed;
import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.exceptions.ErrorConstants;
import com.icthh.xm.ms.entity.domain.Comment;
import com.icthh.xm.ms.entity.service.CommentService;
import com.icthh.xm.ms.entity.web.rest.util.HeaderUtil;
import com.icthh.xm.ms.entity.web.rest.util.PaginationUtil;
import com.icthh.xm.ms.entity.web.rest.util.RespContentUtil;
import io.swagger.annotations.ApiParam;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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
import javax.validation.Valid;

/**
 * REST controller for managing Comment.
 */
@RestController
@RequestMapping("/api")
public class CommentResource {

    private static final String ENTITY_NAME = "comment";

    private final CommentService commentService;
    private final CommentResource commentResource;

    public CommentResource(
                    CommentService commentService,
                    @Lazy CommentResource commentResource) {
        this.commentService = commentService;
        this.commentResource = commentResource;
    }

    /**
     * POST  /comments : Create a new comment.
     *
     * @param comment the comment to create
     * @return the ResponseEntity with status 201 (Created) and with body the new comment, or with status 400 (Bad Request) if the comment has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/comments")
    @Timed
    @PreAuthorize("hasPermission({'comment': #comment}, 'COMMENT.CREATE')")
    public ResponseEntity<Comment> createComment(@Valid @RequestBody Comment comment) throws URISyntaxException {
        if (comment.getId() != null) {
            throw new BusinessException(ErrorConstants.ERR_BUSINESS_IDEXISTS,
                                        "A new comment cannot already have an ID");
        }
        Comment result = commentService.save(comment);
        return ResponseEntity.created(new URI("/api/comments/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * PUT  /comments : Updates an existing comment.
     *
     * @param comment the comment to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated comment,
     * or with status 400 (Bad Request) if the comment is not valid,
     * or with status 500 (Internal Server Error) if the comment couldn't be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PutMapping("/comments")
    @Timed
    @PreAuthorize("hasPermission({'id': #comment.id, 'newComment': #comment}, 'comment', 'COMMENT.UPDATE')")
    public ResponseEntity<Comment> updateComment(@Valid @RequestBody Comment comment) throws URISyntaxException {
        if (comment.getId() == null) {
            //in order to call method with permissions check
            return this.commentResource.createComment(comment);
        }
        Comment result = commentService.save(comment);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(ENTITY_NAME, comment.getId().toString()))
            .body(result);
    }

    /**
     * GET  /comments : get all the comments.
     *
     * @param pageable the pagination information
     * @return the ResponseEntity with status 200 (OK) and the list of comments in body
     */
    @GetMapping("/comments")
    @Timed
    public ResponseEntity<List<Comment>> getAllComments(@ApiParam Pageable pageable) {
        Page<Comment> page = commentService.findAll(pageable, null);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/comments");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    /**
     * GET  /comments/:id : get the "id" comment.
     *
     * @param id the id of the comment to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the comment, or with status 404 (Not Found)
     */
    @GetMapping("/comments/{id}")
    @Timed
    @PostAuthorize("hasPermission({'returnObject': returnObject.body}, 'COMMENT.GET_LIST.ITEM')")
    public ResponseEntity<Comment> getComment(@PathVariable Long id) {
        Comment comment = commentService.findOne(id);
        return RespContentUtil.wrapOrNotFound(Optional.ofNullable(comment));
    }

    /**
     * DELETE  /comments/:id : delete the "id" comment.
     *
     * @param id the id of the comment to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/comments/{id}")
    @Timed
    @PreAuthorize("hasPermission({'id': #id}, 'comment', 'COMMENT.DELETE')")
    public ResponseEntity<Void> deleteComment(@PathVariable Long id) {
        commentService.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(ENTITY_NAME, id.toString())).build();
    }

}
