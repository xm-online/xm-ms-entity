package com.icthh.xm.ms.entity.service;

import static com.google.common.collect.ImmutableMap.of;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.exceptions.EntityNotFoundException;
import com.icthh.xm.commons.exceptions.ErrorConstants;
import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.commons.permission.annotation.FindWithPermission;
import com.icthh.xm.commons.permission.annotation.PrivilegeDescription;
import com.icthh.xm.ms.entity.domain.Link;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.lep.keyresolver.LinkTypeKeyResolver;
import com.icthh.xm.ms.entity.lep.keyresolver.TypeKeyResolver;
import com.icthh.xm.ms.entity.projection.LinkProjection;
import com.icthh.xm.ms.entity.repository.LinkPermittedRepository;
import com.icthh.xm.ms.entity.repository.LinkRepository;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
import com.icthh.xm.ms.entity.security.access.XmEntityDynamicPermissionCheckService;
import com.icthh.xm.ms.entity.service.impl.StartUpdateDateGenerationStrategy;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.icthh.xm.ms.entity.security.access.FeatureContext.LINK_DELETE;

/**
 * Service Implementation for managing Link.
 */
@Slf4j
@Service
@LepService(group = "service.link")
@Transactional
@RequiredArgsConstructor
public class LinkService extends TransactionPropagationService<LinkService> {

    private final LinkRepository linkRepository;

    private final LinkPermittedRepository permittedRepository;

    private final StartUpdateDateGenerationStrategy startUpdateDateGenerationStrategy;

    private final XmEntityRepository xmEntityRepository;

    private final XmEntityDynamicPermissionCheckService permissionCheckService;

    private final EntityManager entityManager;

    /**
     * Save a link.
     *
     * <p>NOTE: Method triggers LEP method which resolved by {@link Link#getTypeKey()} value.
     * @param link the entity to save
     * @return the persisted entity
     */
    @LogicExtensionPoint(value = "Save", resolver = LinkTypeKeyResolver.class)
    public Link save(Link link) {
        return self.saveLink(link);
    }

    /**
     * Save a link.
     *
     * @param link the entity to save
     * @return the persisted entity
     */
    @LogicExtensionPoint("Save")
    public Link saveLink(Link link) {

        startUpdateDateGenerationStrategy.preProcessStartDate(link,
                                                              link.getId(),
                                                              linkRepository,
                                                              Link::setStartDate,
                                                              Link::getStartDate);
        link.setTarget(xmEntityRepository.getOne(entityId(link.getTarget())));
        link.setSource(xmEntityRepository.getOne(entityId(link.getSource())));
        Link result = linkRepository.save(link);
        Hibernate.initialize(result.getTarget());
        return result;
    }

    public List<Link> saveAll(List<Link> list) {
        return linkRepository.saveAll(list);
    }

    /**
     * Create new links in bulk. Targets without id are created transitively (single saveAll),
     * existing source/target entities are resolved with a single query, links are persisted with
     * a single saveAll. No per-link LEP is triggered.
     *
     * @param links new links; every source must reference an existing entity by id
     * @return persisted links with initialized source and target
     */
    public List<Link> createAll(List<Link> links) {
        List<XmEntity> newTargets = links.stream()
            .map(Link::getTarget)
            .filter(target -> target != null && target.getId() == null)
            .toList();
        xmEntityRepository.saveAll(newTargets);

        Set<Long> entityIds = links.stream()
            .flatMap(link -> Stream.of(link.getSource(), link.getTarget()))
            .map(entity -> {
                Long id = entity == null ? null : entity.getId();
                if (id == null) {
                    throw new BusinessException(ErrorConstants.ERR_VALIDATION,
                        "Link source must reference an existing entity by id");
                }
                return id;
            })
            .collect(Collectors.toSet());

        Map<Long, XmEntity> entitiesById = xmEntityRepository.findAllById(entityIds).stream()
            .collect(Collectors.toMap(XmEntity::getId, Function.identity()));
        entityIds.stream()
            .filter(id -> !entitiesById.containsKey(id))
            .findFirst()
            .ifPresent(id -> {
                throw new EntityNotFoundException("XmEntity by id " + id + " not found");
            });

        links.forEach(link -> {
            link.setStartDate(Objects.requireNonNullElseGet(link.getStartDate(),
                startUpdateDateGenerationStrategy::generateStartDate));
            link.setSource(entitiesById.get(link.getSource().getId()));
            link.setTarget(entitiesById.get(link.getTarget().getId()));
        });
        return linkRepository.saveAll(links);
    }

    /**
     * Deletes links using bulk operation and detaches deleted entities
     * from the persistence context to prevent stale Link entities
     * from participating in subsequent flush operations.
     */
    public void deleteInBatch(List<Link> links) {
        linkRepository.deleteAllInBatch(links);
        links.forEach(entityManager::detach);
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
    @PrivilegeDescription("Privilege to get all the links")
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
        return linkRepository.findById(id).orElse(null);
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
     * Get all link by source ID and link typeKey
     *
     * @param id      source entity ID
     * @param typeKey target type key
     * @return list of links
     */
    @Transactional(readOnly = true)
    public List<Link> findBySourceIdAndTypeKey(Long id, String typeKey) {
        log.debug("Request to get link by sourceId={} and typeKey={}", id, typeKey);
        return linkRepository.findBySourceIdAndTypeKey(id, typeKey);
    }

    @Transactional(readOnly = true)
    public List<LinkProjection> findLinkProjectionsBySourceIdAndTypeKey(Long id, String typeKey) {
        log.debug("Request to get link by sourceId={} and typeKey={}", id, typeKey);
        return linkRepository.findLinkProjectionsBySourceIdAndTypeKey(id, typeKey);
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
    @PrivilegeDescription("Privilege to get all the source links by target entity id and typeKeys")
    public Page<Link> findSourceByTargetIdAndTypeKey(Pageable pageable, Long id, Set<String> typeKey, String
        privilegeKey) {
        return permittedRepository.findAllByTargetIdAndTypeKeyIn(pageable, id, typeKey, privilegeKey);
    }

    /**
     * Get all links where the given xmEntity is the target (i.e. the incoming/"sources" side).
     *
     * @param id the xmEntity id
     * @param typeKey the xmEntity typeKey
     * @param pageable the pagination information
     * @param privilegeKey the privilege key
     * @return the page of entities
     */
    @Transactional(readOnly = true)
    @FindWithPermission("LINK.SOURCES.GET_LIST.BY_XM_ENTITY")
    @LogicExtensionPoint(value = "FindSourcesByXmEntity", resolver = TypeKeyResolver.class)
    @PrivilegeDescription("Privilege to search for the source links by xmEntity id and typeKey")
    public Page<Link> findSourcesByXmEntity(Long id, String typeKey, Pageable pageable, String privilegeKey) {
        return permittedRepository.findByCondition("returnObject.target.id = :id and returnObject.typeKey = :typeKey",
            of("id", id, "typeKey", typeKey), pageable, Link.class, privilegeKey);
    }

    /**
     * Get all links where the given xmEntity is the source (i.e. the outgoing/"targets" side).
     *
     * @param id the xmEntity id
     * @param typeKey the xmEntity typeKey
     * @param pageable the pagination information
     * @param privilegeKey the privilege key
     * @return the page of entities
     */
    @Transactional(readOnly = true)
    @FindWithPermission("LINK.TARGETS.GET_LIST.BY_XM_ENTITY")
    @LogicExtensionPoint(value = "FindTargetsByXmEntity", resolver = TypeKeyResolver.class)
    @PrivilegeDescription("Privilege to search for the target links by xmEntity id and typeKey")
    public Page<Link> findTargetsByXmEntity(Long id, String typeKey, Pageable pageable, String privilegeKey) {
        return permittedRepository.findByCondition("returnObject.source.id = :id and returnObject.typeKey = :typeKey",
            of("id", id, "typeKey", typeKey), pageable, Link.class, privilegeKey);
    }

    /**
     *  Delete the  link by id.
     *
     *  @param id the id of the entity
     */
    @LogicExtensionPoint("Delete")
    public void delete(Long id) {
        if (permissionCheckService.isDynamicLinkDeletePermissionEnabled()) {
            Link link = linkRepository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Link by id " + id + " not found"));
            self.deleteByTypeKey(id, link);
        } else {
            linkRepository.deleteById(id);
        }
    }

    @LogicExtensionPoint(value = "Delete", resolver = LinkTypeKeyResolver.class)
    public void deleteByTypeKey(Long id, Link link) {
        permissionCheckService.checkContextPermission(LINK_DELETE, "LINK.DELETE", link.getTypeKey());
        linkRepository.delete(link);
    }

    @Transactional(readOnly = true)
    public List<Link> findAll(Specification<Link> spec) {
        return linkRepository.findAll(spec);
    }

    @Transactional(readOnly = true)
    public Page<Link> findAll(Specification<Link> spec, Pageable pageable) {
        return linkRepository.findAll(spec, pageable);
    }

}
