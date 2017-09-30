package com.icthh.xm.ms.entity.config.tenant;

import static com.icthh.xm.ms.entity.config.TenantConfigMockConfiguration.getXmEntitySpec;

import com.icthh.xm.commons.config.client.repository.TenantConfigRepository;
import com.icthh.xm.ms.entity.config.ApplicationProperties;
import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import com.icthh.xm.ms.entity.service.XmEntitySpecService;

import java.util.Map;

public class LocalXmEntitySpecService extends XmEntitySpecService {

    private ApplicationProperties applicationProperties;

    public LocalXmEntitySpecService(TenantConfigRepository tenantConfigRepository, ApplicationProperties applicationProperties) {
        super(tenantConfigRepository, applicationProperties);
        this.applicationProperties = applicationProperties;
    }

    @Override
        protected Map<String, TypeSpec> getTypeSpecs() {
            String tenant = TenantContext.getCurrent().getTenant();
            String config = getXmEntitySpec(tenant);
            String key = applicationProperties.getSpecificationPathPattern().replace("{tenantName}", tenant);
            this.onRefresh(key, config);
            return super.getTypeSpecs();
        }

}
