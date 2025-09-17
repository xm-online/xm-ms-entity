package com.icthh.xm.ms.entity.service.privileges.custom;

import com.icthh.xm.commons.config.client.repository.CommonConfigRepository;
import com.icthh.xm.commons.logging.aop.IgnoreLogginAspect;
import com.icthh.xm.commons.permission.service.custom.AbstractCustomPrivilegeSpecService;
import com.icthh.xm.commons.permission.service.custom.CustomPrivilegesExtractor;
import com.icthh.xm.ms.entity.config.XmEntityTenantConfigService;
import com.icthh.xm.ms.entity.config.XmEntityTenantConfigService.XmEntityTenantConfig;
import com.icthh.xm.ms.entity.domain.spec.TypeSpec;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import static com.icthh.xm.ms.entity.service.XmEntitySpecService.EntitySpecUpdateListener;

@Slf4j
@Service
public class EntityCustomPrivilegeService extends AbstractCustomPrivilegeSpecService implements EntitySpecUpdateListener {
    private final XmEntityTenantConfigService tenantConfigService;

    public EntityCustomPrivilegeService(CommonConfigRepository commonConfigRepository,
                                        List<CustomPrivilegesExtractor> privilegesExtractors,
                                        XmEntityTenantConfigService tenantConfigService) {
        super(commonConfigRepository, privilegesExtractors);
        this.tenantConfigService = tenantConfigService;
    }

    @IgnoreLogginAspect
    @Override
    public void onEntitySpecUpdate(Map<String, TypeSpec> specs, String tenantKey) {
        XmEntityTenantConfig xmEntityTenantConfig = tenantConfigService.getXmEntityTenantConfig(tenantKey);
        Boolean disableDynamicPrivilegesGeneration = xmEntityTenantConfig.getDisableDynamicPrivilegesGeneration();
        if (Boolean.TRUE.equals(disableDynamicPrivilegesGeneration)) {
            log.warn("Dynamic privilege generation disabled.");
            return;
        }

        onSpecificationUpdate(specs.values(), tenantKey);
    }
}
