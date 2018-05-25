package com.icthh.xm.ms.entity.repository;

import com.icthh.xm.commons.config.client.service.TenantConfigService;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.projection.XmEntityVersion;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.stereotype.Component;

import javax.persistence.LockModeType;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@NoRepositoryBean
@RequiredArgsConstructor
public class XmEntityRepository implements SpringXmEntityRepository {

    @Delegate(excludes = Save.class)
    private final SpringXmEntityRepository springXmEntityRepository;
    private final TenantConfigService tenantConfigService;

    private interface Save {
        <S extends XmEntity> List<S> save(Iterable<S> entities);
        <S extends XmEntity> S saveAndFlush(S entity);
        <S extends XmEntity> S save(S entity);
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
