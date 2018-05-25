package com.icthh.xm.ms.entity.domain.listener;

import com.icthh.xm.commons.config.client.service.TenantConfigService;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
import com.icthh.xm.ms.entity.util.AutowireHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import javax.persistence.PostLoad;
import javax.persistence.PostPersist;
import javax.persistence.PostUpdate;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class VersionListener {

    @Autowired
    private TenantConfigService tenantConfigService;

    @Autowired
    private XmEntityRepository xmEntityRepository;

    private boolean isEntityVersionEnabled() {
        log.debug("{}", getTenantConfigService().getConfig());
        return Optional.ofNullable(getTenantConfigService().getConfig().get("entityVersionControl"))
            .filter(it -> it instanceof Map).map(Map.class::cast)
            .map(it -> it.get("enabled")).map(it -> (boolean)it).orElse(false);
    }

    @PostLoad
    @PrePersist
    @PreUpdate
    @PostPersist
    @PostUpdate
    public void process(XmEntity obj) {
        if (!isEntityVersionEnabled()) {
            obj.setVersion(null);
        }
    }

    private TenantConfigService getTenantConfigService() {
        AutowireHelper.autowire(this, tenantConfigService);
        return tenantConfigService;
    }
}
