package com.icthh.xm.ms.entity.service;

import static org.elasticsearch.index.query.QueryBuilders.queryStringQuery;

import com.icthh.xm.ms.entity.domain.Link;
import com.icthh.xm.ms.entity.repository.LinkRepository;
import com.icthh.xm.ms.entity.repository.search.LinkSearchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing Link.
 */
@Service
@Transactional
public class LinkService {

    private final Logger log = LoggerFactory.getLogger(LinkService.class);

    private final LinkRepository linkRepository;

    private final LinkSearchRepository linkSearchRepository;

    public LinkService(LinkRepository linkRepository, LinkSearchRepository linkSearchRepository) {
        this.linkRepository = linkRepository;
        this.linkSearchRepository = linkSearchRepository;
    }

    /**
     * Save a link.
     *
     * @param link the entity to save
     * @return the persisted entity
     */
    public Link save(Link link) {
        log.debug("Request to save Link : {}", link);
        Link result = linkRepository.save(link);
        linkSearchRepository.save(result);
        return result;
    }

    /**
     *  Get all the links.
     *
     *  @param pageable the pagination information
     *  @return the list of entities
     */
    @Transactional(readOnly = true)
    public Page<Link> findAll(Pageable pageable) {
        log.debug("Request to get all Links");
        return linkRepository.findAll(pageable);
    }

    /**
     *  Get one link by id.
     *
     *  @param id the id of the entity
     *  @return the entity
     */
    @Transactional(readOnly = true)
    public Link findOne(Long id) {
        log.debug("Request to get Link : {}", id);
        return linkRepository.findOne(id);
    }

    /**
     *  Delete the  link by id.
     *
     *  @param id the id of the entity
     */
    public void delete(Long id) {
        log.debug("Request to delete Link : {}", id);
        linkRepository.delete(id);
        linkSearchRepository.delete(id);
    }

    /**
     * Search for the link corresponding to the query.
     *
     *  @param query the query of the search
     *  @param pageable the pagination information
     *  @return the list of entities
     */
    @Transactional(readOnly = true)
    public Page<Link> search(String query, Pageable pageable) {
        log.debug("Request to search for a page of Links for query {}", query);
        return linkSearchRepository.search(queryStringQuery(query), pageable);
    }
}
