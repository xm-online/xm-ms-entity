package com.icthh.xm.ms.entity.config.tenant;

import static com.icthh.xm.commons.tenant.TenantContextUtils.getRequiredTenantKeyValue;
import static com.icthh.xm.ms.entity.config.TenantConfigMockConfiguration.getXmEntitySpec;

import com.icthh.xm.commons.config.client.repository.TenantConfigRepository;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.ms.entity.config.ApplicationProperties;
import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import com.icthh.xm.ms.entity.service.EntityCustomPrivilegeService;
import com.icthh.xm.ms.entity.service.XmEntitySpecService;

import java.util.Map;

public class LocalXmEntitySpecService extends XmEntitySpecService {

    private final ApplicationProperties applicationProperties;
    private final TenantContextHolder tenantContextHolder;
    private final EntityCustomPrivilegeService entityCustomPrivilegeService;

    public LocalXmEntitySpecService(TenantConfigRepository tenantConfigRepository,
                                    ApplicationProperties applicationProperties,
                                    TenantContextHolder tenantContextHolder,
                                    EntityCustomPrivilegeService entityCustomPrivilegeService) {
        super(tenantConfigRepository, applicationProperties, tenantContextHolder, entityCustomPrivilegeService);

        this.applicationProperties = applicationProperties;
        this.tenantContextHolder = tenantContextHolder;
        this.entityCustomPrivilegeService = entityCustomPrivilegeService;
    }

    @Override
    protected Map<String, TypeSpec> getTypeSpecs() {
        String tenantName = getRequiredTenantKeyValue(tenantContextHolder);
        String config = getXmEntitySpec(tenantName);
        String key = applicationProperties.getSpecificationPathPattern().replace("{tenantName}", tenantName);
        this.onRefresh(key, config);
        return super.getTypeSpecs();
    }

}
