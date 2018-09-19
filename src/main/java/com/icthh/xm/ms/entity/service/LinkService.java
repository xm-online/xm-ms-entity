package com.icthh.xm.ms.entity.service;

import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.commons.permission.annotation.FindWithPermission;
import com.icthh.xm.ms.entity.domain.Link;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.repository.LinkPermittedRepository;
import com.icthh.xm.ms.entity.repository.LinkRepository;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
import com.icthh.xm.ms.entity.repository.search.PermittedSearchRepository;
import com.icthh.xm.ms.entity.service.impl.StartUpdateDateGenerationStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * Service Implementation for managing Link.
 */
@Slf4j
@Service
@LepService(group = "service.link")
@Transactional
@RequiredArgsConstructor
public class LinkService {

    private final LinkRepository linkRepository;

    private final LinkPermittedRepository permittedRepository;

    private final PermittedSearchRepository permittedSearchRepository;

    private final StartUpdateDateGenerationStrategy startUpdateDateGenerationStrategy;

    private final XmEntityRepository xmEntityRepository;

    /**
     * Save a link.
     *
     * @param link the entity to save
     * @return the persisted entity
     */
    @LogicExtensionPoint("Save")
    public Link save(Link link) {

        startUpdateDateGenerationStrategy.preProcessStartDate(link,
                                                              link.getId(),
                                                              linkRepository,
                                                              Link::setStartDate,
                                                              Link::getStartDate);
        link.setTarget(xmEntityRepository.getOne(entityId(link.getTarget())));
        link.setSource(xmEntityRepository.getOne(entityId(link.getSource())));
        Link result = linkRepository.save(link);
        return result;
    }

    private Long entityId(XmEntity entity) {
        Long id = entity.getId();
        if (id == null) {
            id = xmEntityRepository.save(entity).getId();
        }
        return id;
    }

    /**
     *  Get all the links.
     *
     *  @param pageable the pagination information
     *  @return the list of entities
     */
    @Transactional(readOnly = true)
    @FindWithPermission("LINK.GET_LIST")
    @LogicExtensionPoint("FindAll")
    public Page<Link> findAll(Pageable pageable, String privilegeKey) {
        return permittedRepository.findAll(pageable, Link.class, privilegeKey);
    }

    /**
     *  Get one link by id.
     *
     *  @param id the id of the entity
     *  @return the entity
     */
    @Transactional(readOnly = true)
    @LogicExtensionPoint("FindOne")
    public Link findOne(Long id) {
        return linkRepository.findOne(id);
    }

    /**
     * Get all link by source ID and target typeKey
     *
     * @param id      source entity ID
     * @param typeKey target type key
     * @return list of links
     */
    @Transactional(readOnly = true)
    public List<Link> findBySourceIdAndTargetTypeKey(Long id, String typeKey) {
        log.debug("Request to get link by sourceId={} and typeKey={}", id, typeKey);
        return linkRepository.findBySourceIdAndTargetTypeKey(id, typeKey);
    }

    /**
     * Get all link by target ID and link typeKey
     * @param id target entity ID
     * @param typeKey link type key
     * @return list of links
     */
    @Transactional(readOnly = true)
    public List<Link> findByTargetIdAndTypeKey(Long id, String typeKey) {
        log.debug("Request to get link by targetId={} and typeKey={}", id, typeKey);
        return linkRepository.findByTargetIdAndTypeKey(id, typeKey);
    }

    @FindWithPermission("LINK.SOURCE.GET_LIST")
    @Transactional(readOnly = true)
    public Page<Link> findSourceByTargetIdAndTypeKey(Pageable pageable, Long id, Set<String> typeKey, String
        privilegeKey) {
        return permittedRepository.findAllByTargetIdAndTypeKeyIn(pageable, id, typeKey, privilegeKey);
    }

    /**
     *  Delete the  link by id.
     *
     *  @param id the id of the entity
     */
    @LogicExtensionPoint("Delete")
    public void delete(Long id) {
        linkRepository.delete(id);
    }

    /**
     * Search for the link corresponding to the query.
     *
     *  @param query the query of the search
     *  @param pageable the pagination information
     *  @return the list of entities
     */
    @Transactional(readOnly = true)
    @FindWithPermission("LINK.SEARCH")
    public Page<Link> search(String query, Pageable pageable, String privilegeKey) {
        return permittedSearchRepository.search(query, pageable, Link.class, privilegeKey);
    }

    @Transactional(readOnly = true)
    public List<Link> findAll(Specification<Link> spec) {
        return linkRepository.findAll(spec);
    }

}
