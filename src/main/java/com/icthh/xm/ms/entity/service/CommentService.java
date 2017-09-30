package com.icthh.xm.ms.entity.service;

import static org.elasticsearch.index.query.QueryBuilders.queryStringQuery;

import com.icthh.xm.ms.entity.config.tenant.TenantContext;
import com.icthh.xm.ms.entity.domain.Comment;
import com.icthh.xm.ms.entity.repository.CommentRepository;
import com.icthh.xm.ms.entity.repository.search.CommentSearchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing Comment.
 */
@Service
@Transactional
public class CommentService {

    private final Logger log = LoggerFactory.getLogger(CommentService.class);

    private final CommentRepository commentRepository;

    private final CommentSearchRepository commentSearchRepository;

    public CommentService(CommentRepository commentRepository, CommentSearchRepository commentSearchRepository) {
        this.commentRepository = commentRepository;
        this.commentSearchRepository = commentSearchRepository;
    }

    /**
     * Save a comment.
     *
     * @param comment the entity to save
     * @return the persisted entity
     */
    public Comment save(Comment comment) {
        String userKey = TenantContext.getCurrent().getUserKey();
        comment.setUserKey(userKey);
        log.debug("Request to save Comment : {}", comment);
        Comment result = commentRepository.save(comment);
        commentSearchRepository.save(result);
        return result;
    }

    /**
     *  Get all the comments.
     *
     *  @param pageable the pagination information
     *  @return the list of entities
     */
    @Transactional(readOnly = true)
    public Page<Comment> findAll(Pageable pageable) {
        log.debug("Request to get all Comments");
        return commentRepository.findAll(pageable);
    }

    /**
     *  Get one comment by id.
     *
     *  @param id the id of the entity
     *  @return the entity
     */
    @Transactional(readOnly = true)
    public Comment findOne(Long id) {
        log.debug("Request to get Comment : {}", id);
        return commentRepository.findOne(id);
    }

    /**
     *  Delete the  comment by id.
     *
     *  @param id the id of the entity
     */
    public void delete(Long id) {
        log.debug("Request to delete Comment : {}", id);
        commentRepository.delete(id);
        commentSearchRepository.delete(id);
    }

    /**
     * Search for the comment corresponding to the query.
     *
     *  @param query the query of the search
     *  @param pageable the pagination information
     *  @return the list of entities
     */
    @Transactional(readOnly = true)
    public Page<Comment> search(String query, Pageable pageable) {
        log.debug("Request to search for a page of Comments for query {}", query);
        return commentSearchRepository.search(queryStringQuery(query), pageable);
    }
}
