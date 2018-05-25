package com.icthh.xm.ms.entity.repository;

import com.icthh.xm.commons.config.client.service.TenantConfigService;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.projection.XmEntityIdKeyTypeKey;
import com.icthh.xm.ms.entity.projection.XmEntityStateProjection;
import com.icthh.xm.ms.entity.projection.XmEntityVersion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Component
@NoRepositoryBean
@RequiredArgsConstructor
public class XmEntityRepository implements SpringXmEntityRepository {

    private final SpringXmEntityRepository springXmEntityRepository;
    private final TenantConfigService tenantConfigService;

    public XmEntity findOneByIdForUpdate(@Param("id") Long id) {
        return springXmEntityRepository.findOneByIdForUpdate(id);
    }

    public XmEntity findOneById(Long id) {
        return springXmEntityRepository.findOneById(id);
    }

    public Page<XmEntity> findAllByTypeKeyIn(Pageable pageable, Set<String> typeKeys) {
        return springXmEntityRepository.findAllByTypeKeyIn(pageable, typeKeys);
    }

    public XmEntityIdKeyTypeKey findOneIdKeyTypeKeyById(Long id) {
        return springXmEntityRepository.findOneIdKeyTypeKeyById(id);
    }

    public XmEntityIdKeyTypeKey findOneIdKeyTypeKeyByKey(String key) {
        return springXmEntityRepository.findOneIdKeyTypeKeyByKey(key);
    }

    public XmEntityStateProjection findStateProjectionById(Long id) {
        return springXmEntityRepository.findStateProjectionById(id);
    }

    public XmEntityStateProjection findStateProjectionByKey(String key) {
        return springXmEntityRepository.findStateProjectionByKey(key);
    }

    public Optional<XmEntityVersion> findVersionById(Long id) {
        return springXmEntityRepository.findVersionById(id);
    }

    public boolean existsByTypeKeyAndNameIgnoreCase(String typeKey, String name) {
        return springXmEntityRepository.existsByTypeKeyAndNameIgnoreCase(typeKey, name);
    }

    public List<XmEntity> findAll() {
        return springXmEntityRepository.findAll();
    }

    public List<XmEntity> findAll(Sort sort) {
        return springXmEntityRepository.findAll(sort);
    }

    public List<XmEntity> findAll(Iterable<Long> longs) {
        return springXmEntityRepository.findAll(longs);
    }

    public void flush() {
        springXmEntityRepository.flush();
    }

    public void deleteInBatch(Iterable<XmEntity> entities) {
        springXmEntityRepository.deleteInBatch(entities);
    }

    public void deleteAllInBatch() {
        springXmEntityRepository.deleteAllInBatch();
    }

    public XmEntity getOne(Long aLong) {
        return springXmEntityRepository.getOne(aLong);
    }

    public <S extends XmEntity> List<S> findAll(Example<S> example) {
        return springXmEntityRepository.findAll(example);
    }

    public <S extends XmEntity> List<S> findAll(Example<S> example, Sort sort) {
        return springXmEntityRepository.findAll(example, sort);
    }

    public Page<XmEntity> findAll(Pageable pageable) {
        return springXmEntityRepository.findAll(pageable);
    }

    public XmEntity findOne(Long aLong) {
        return springXmEntityRepository.findOne(aLong);
    }

    public boolean exists(Long aLong) {
        return springXmEntityRepository.exists(aLong);
    }

    public long count() {
        return springXmEntityRepository.count();
    }

    public void delete(Long aLong) {
        springXmEntityRepository.delete(aLong);
    }

    public void delete(XmEntity entity) {
        springXmEntityRepository.delete(entity);
    }

    public void delete(Iterable<? extends XmEntity> entities) {
        springXmEntityRepository.delete(entities);
    }

    public void deleteAll() {
        springXmEntityRepository.deleteAll();
    }

    public <S extends XmEntity> S findOne(Example<S> example) {
        return springXmEntityRepository.findOne(example);
    }

    public <S extends XmEntity> Page<S> findAll(Example<S> example, Pageable pageable) {
        return springXmEntityRepository.findAll(example, pageable);
    }

    public <S extends XmEntity> long count(Example<S> example) {
        return springXmEntityRepository.count(example);
    }

    public <S extends XmEntity> boolean exists(Example<S> example) {
        return springXmEntityRepository.exists(example);
    }

    public XmEntity findOne(Specification<XmEntity> spec) {
        return springXmEntityRepository.findOne(spec);
    }

    public List<XmEntity> findAll(Specification<XmEntity> spec) {
        return springXmEntityRepository.findAll(spec);
    }

    public Page<XmEntity> findAll(Specification<XmEntity> spec, Pageable pageable) {
        return springXmEntityRepository.findAll(spec, pageable);
    }

    public List<XmEntity> findAll(Specification<XmEntity> spec, Sort sort) {
        return springXmEntityRepository.findAll(spec, sort);
    }

    public long count(Specification<XmEntity> spec) {
        return springXmEntityRepository.count(spec);
    }

    public XmEntity findOne(Long aLong, List<String> embed) {
        return springXmEntityRepository.findOne(aLong, embed);
    }

    public <S extends XmEntity> List<S> save(Iterable<S> entities) {
        if (!isEntityVersionEnabled()) {
            entities.forEach(this::updateVersion);
        }

        return springXmEntityRepository.save(entities);
    }

    public <S extends XmEntity> S saveAndFlush(S entity) {
        if (!isEntityVersionEnabled()) {
            updateVersion(entity);
        }

        return springXmEntityRepository.saveAndFlush(entity);
    }

    private <S extends XmEntity> void updateVersion(S entity) {
        if (!entity.isNew() && entity.getVersion() == null) {
            entity.setVersion(findVersionById(entity.getId()).map(XmEntityVersion::getVersion).orElse(null));
        }
    }

    public <S extends XmEntity> S save(S entity) {
        if (!isEntityVersionEnabled()) {
            updateVersion(entity);
        }

        return springXmEntityRepository.save(entity);
    }

    private boolean isEntityVersionEnabled() {
        return Optional.ofNullable(tenantConfigService.getConfig().get("entityVersionControl"))
            .filter(it -> it instanceof Map).map(Map.class::cast)
            .map(it -> it.get("enabled")).map(it -> (boolean)it).orElse(false);
    }
}
