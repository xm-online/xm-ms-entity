package com.icthh.xm.ms.entity.service;

import com.icthh.xm.commons.permission.annotation.FindWithPermission;
import com.icthh.xm.commons.permission.repository.PermittedRepository;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.ms.entity.domain.Comment;
import com.icthh.xm.ms.entity.repository.CommentRepository;
import com.icthh.xm.ms.entity.repository.search.CommentSearchRepository;
import com.icthh.xm.ms.entity.repository.search.PermittedSearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing Comment.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;

    private final CommentSearchRepository commentSearchRepository;

    private final XmAuthenticationContextHolder authContextHolder;

    private final PermittedRepository permittedRepository;

    private final PermittedSearchRepository permittedSearchRepository;

    private String getUserKey() {
        return authContextHolder.getContext().getUserKey()
            .orElseThrow(() -> new IllegalStateException("No authenticated user in context, can't get user key"));
    }

    /**
     * Save a comment.
     *
     * @param comment the entity to save
     * @return the persisted entity
     */
    public Comment save(Comment comment) {
        comment.setUserKey(getUserKey());
        Comment result = commentRepository.save(comment);
        commentSearchRepository.save(result);
        return result;
    }

    /**
     * Get all the comments.
     *
     * @param pageable the pagination information
     * @return the list of entities
     */
    @Transactional(readOnly = true)
    @FindWithPermission("COMMENT.GET_LIST")
    public Page<Comment> findAll(Pageable pageable, String privilegeKey) {
        return permittedRepository.findAll(pageable, Comment.class, privilegeKey);
    }

    /**
     * Get one comment by id.
     *
     * @param id the id of the entity
     * @return the entity
     */
    @Transactional(readOnly = true)
    public Comment findOne(Long id) {
        return commentRepository.findOne(id);
    }

    /**
     * Delete the  comment by id.
     *
     * @param id the id of the entity
     */
    public void delete(Long id) {
        commentRepository.delete(id);
        commentSearchRepository.delete(id);
    }

    /**
     * Search for the comment corresponding to the query.
     *
     * @param query    the query of the search
     * @param pageable the pagination information
     * @return the list of entities
     */
    @Transactional(readOnly = true)
    @FindWithPermission("COMMENT.SEARCH")
    public Page<Comment> search(String query, Pageable pageable, String privilegeKey) {
        return permittedSearchRepository.search(query, pageable, Comment.class, privilegeKey);
    }

}
