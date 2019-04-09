package com.icthh.xm.ms.entity.repository.impl;

import com.icthh.xm.commons.config.client.service.TenantConfigService;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.projection.XmEntityIdKeyTypeKey;
import com.icthh.xm.ms.entity.projection.XmEntityStateProjection;
import com.icthh.xm.ms.entity.projection.XmEntityVersion;
import com.icthh.xm.ms.entity.repository.SpringXmEntityRepository;
import com.icthh.xm.ms.entity.repository.XmEntityRepositoryInternal;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class XmEntityRepositoryInternalImpl implements XmEntityRepositoryInternal {

    private final SpringXmEntityRepository springXmEntityRepository;
    private final TenantConfigService tenantConfigService;

    @Override
    public XmEntity findOneByIdForUpdate(@Param("id") Long id) {
        return springXmEntityRepository.findOneByIdForUpdate(id);
    }

    /**
     * Returns entity by ID with using xmEntityGraph.
     *
     * @param id identifier of the entity
     * @return xmEntity instance
     */
    @Override
    public XmEntity findOneById(Long id) {
        return springXmEntityRepository.findOneById(id);
    }

    @Override
    public Page<XmEntity> findAllByTypeKeyIn(Pageable pageable, Set<String> typeKeys) {
        return springXmEntityRepository.findAllByTypeKeyIn(pageable, typeKeys);
    }

    @Override
    public XmEntityIdKeyTypeKey findOneIdKeyTypeKeyById(Long id) {
        return springXmEntityRepository.findOneIdKeyTypeKeyById(id);
    }

    @Override
    public XmEntityIdKeyTypeKey findOneIdKeyTypeKeyByKey(String key) {
        return springXmEntityRepository.findOneIdKeyTypeKeyByKey(key);
    }

    @Override
    public XmEntityStateProjection findStateProjectionById(Long id) {
        return springXmEntityRepository.findStateProjectionById(id);
    }

    @Override
    public List<XmEntityStateProjection> findAllStateProjectionByIdIn(Iterable<Long> longs) {
        return springXmEntityRepository.findAllStateProjectionByIdIn(longs);
    }

    @Override
    public XmEntityStateProjection findStateProjectionByKey(String key) {
        return springXmEntityRepository.findStateProjectionByKey(key);
    }

    @Override
    public boolean existsByTypeKeyAndNameIgnoreCase(String typeKey, String name) {
        return springXmEntityRepository.existsByTypeKeyAndNameIgnoreCase(typeKey, name);
    }

    @Override
    public List<XmEntity> findAll() {
        return springXmEntityRepository.findAll();
    }

    @Override
    public Page<XmEntity> findAll(Pageable pageable) {
        return springXmEntityRepository.findAll(pageable);
    }

    @Override
    public List<XmEntity> findAll(Specification<XmEntity> spec) {
        return springXmEntityRepository.findAll(spec);
    }

    /**
     * For backward compatibility in LEPs.
     * <p>
     * Deprecated: use findAllById(Iterable&lt;Long&gt; longs) instead
     * </p>
     */
    @Deprecated
    @Override
    public List<XmEntity> findAll(Iterable<Long> longs) {
        return findAllById(longs);
    }

    @Override
    public Page<XmEntity> findAll(Specification<XmEntity> spec, Pageable pageable) {
        return springXmEntityRepository.findAll(spec, pageable);
    }

    @Override
    public List<XmEntity> findAll(Specification<XmEntity> spec, Sort sort) {
        return springXmEntityRepository.findAll(spec, sort);
    }

    @Override
    public List<XmEntity> findAll(String jpql, Map<String, Object> args, List<String> embed) {
        return springXmEntityRepository.findAll(jpql, args, embed);
    }

    @Override
    public List<XmEntity> findAllById(Iterable<Long> longs) {
        return springXmEntityRepository.findAllById(longs);
    }

    @Override
    public void flush() {
        springXmEntityRepository.flush();
    }

    /**
     * Returns a reference to the entity with the given identifier.
     *
     * @param id must not be {@literal null}.
     * @return a reference to the entity with the given identifier.
     * @throws javax.persistence.EntityNotFoundException if no entity exists for given {@code id}.
     */
    @Override
    public XmEntity getOne(Long id) {
        return springXmEntityRepository.getOne(id);
    }


    @Override
    public Optional<XmEntity> findById(Long id) {
        return springXmEntityRepository.findById(id);
    }

    /**
     * For backward compatibility in LEPs.
     * <p>
     * Deprecated: use findById(Long aLong) instead.
     * </p>
     */
    @Deprecated
    @Override
    public XmEntity findOne(Long id) {
        return findById(id).orElse(null);
    }

    public XmEntity findOne(Long id, List<String> embed) {
        return springXmEntityRepository.findOne(id, embed);
    }

    @Override
    public <S extends XmEntity> S findOne(Example<S> example) {
        return springXmEntityRepository.findOne(example).orElse(null);
    }

    @Override
    public XmEntity findOne(Specification<XmEntity> spec) {
        return springXmEntityRepository.findOne(spec).orElse(null);
    }

    @Override
    public boolean existsById(Long id) {
        return springXmEntityRepository.existsById(id);
    }

    /**
     * For backward compatibility in LEPs.
     * <p>
     * Deprecated: use existsById(Long id) instead.
     * </p>
     */
    @Deprecated
    @Override
    public boolean exists(Long id) {
        return existsById(id);
    }

    @Override
    public long count() {
        return springXmEntityRepository.count();
    }

    @Override
    public long count(Specification<XmEntity> spec) {
        return springXmEntityRepository.count(spec);
    }

    @Override
    public void deleteById(Long id) {
        springXmEntityRepository.deleteById(id);
    }

    @Override
    public void delete(XmEntity entity) {
        springXmEntityRepository.delete(entity);
    }

    @Override
    public void delete(Long id) {
        deleteById(id);
    }

    @Override
    public void delete(Iterable<? extends XmEntity> entities) {
        deleteAll(entities);
    }

    @Override
    public void deleteAll(Iterable<? extends XmEntity> entities) {
        springXmEntityRepository.deleteAll(entities);
    }

    @Override
    public void deleteAll() {
        springXmEntityRepository.deleteAll();
    }

    @Override
    public <S extends XmEntity> List<S> saveAll(Iterable<S> entities) {
        if (!isEntityVersionEnabled()) {
            entities.forEach(this::updateVersion);
        }

        return springXmEntityRepository.saveAll(entities);
    }

    /**
     * For backward compatibility in LEPs.
     * <p>
     * Deprecated: use saveAll(Iterable<? extends XmEntity> entities) instead.
     * </p>
     */
    @Deprecated
    @Override
    public <S extends XmEntity> List<S> save(Iterable<S> entities) {
        return saveAll(entities);
    }

    @Override
    public <S extends XmEntity> S save(S entity) {
        if (!isEntityVersionEnabled()) {
            updateVersion(entity);
        }

        return springXmEntityRepository.save(entity);
    }

    @Override
    public <S extends XmEntity> S saveAndFlush(S entity) {
        if (!isEntityVersionEnabled()) {
            updateVersion(entity);
        }

        return springXmEntityRepository.saveAndFlush(entity);
    }

    @Override
    public Optional<XmEntityVersion> findVersionById(Long id) {
        return springXmEntityRepository.findVersionById(id);
    }

    private <S extends XmEntity> void updateVersion(S entity) {
        if (!entity.isNew() && entity.getVersion() == null) {
            entity.setVersion(findVersionById(entity.getId()).map(XmEntityVersion::getVersion).orElse(null));
        }
    }

    private boolean isEntityVersionEnabled() {
        return Optional.ofNullable(tenantConfigService.getConfig().get("entityVersionControl"))
            .filter(it -> it instanceof Map).map(Map.class::cast)
            .map(it -> it.get("enabled")).map(it -> (boolean) it).orElse(false);
    }
}
