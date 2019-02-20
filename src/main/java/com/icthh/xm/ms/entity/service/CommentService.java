package com.icthh.xm.ms.entity.service;

import static com.google.common.collect.ImmutableMap.of;

import com.google.common.collect.ImmutableMap;
import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.commons.permission.annotation.FindWithPermission;
import com.icthh.xm.commons.permission.repository.PermittedRepository;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.ms.entity.domain.Comment;
import com.icthh.xm.ms.entity.repository.CommentRepository;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
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
@LepService(group = "service.comments")
@Transactional
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;

    private final XmAuthenticationContextHolder authContextHolder;

    private final PermittedRepository permittedRepository;

    private final PermittedSearchRepository permittedSearchRepository;

    private final XmEntityRepository xmEntityRepository;

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
    @LogicExtensionPoint("Save")
    public Comment save(Comment comment) {
        comment.setXmEntity(xmEntityRepository.getOne(comment.getXmEntity().getId()));
        comment.setUserKey(getUserKey());
        return commentRepository.save(comment);
    }

    /**
     * Get all the comments.
     *
     * @param pageable the pagination information
     * @return the list of entities
     */
    @Transactional(readOnly = true)
    @FindWithPermission("COMMENT.GET_LIST")
    @LogicExtensionPoint("FindAll")
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
    @LogicExtensionPoint("FindOne")
    public Comment findOne(Long id) {
        return commentRepository.findById(id).orElse(null);
    }

    /**
     * Delete the  comment by id.
     *
     * @param id the id of the entity
     */
    @LogicExtensionPoint("Delete")
    public void delete(Long id) {
        commentRepository.deleteById(id);
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
    @LogicExtensionPoint("Search")
    public Page<Comment> search(String query, Pageable pageable, String privilegeKey) {
        return permittedSearchRepository.search(query, pageable, Comment.class, privilegeKey);
    }

    @Transactional(readOnly = true)
    @FindWithPermission("COMMENT.GET_LIST.BY_XM_ENTITY")
    @LogicExtensionPoint("FindByXmEntity")
    public Page<Comment> findByXmEntity(Long id, Pageable pageable, String privilegeKey) {
        return permittedRepository.findByCondition("returnObject.xmEntity.id = :id", of("id", id), pageable, Comment.class, privilegeKey);
    }
}
