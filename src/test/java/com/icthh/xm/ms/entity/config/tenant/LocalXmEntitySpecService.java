package com.icthh.xm.ms.entity.config.tenant;

import com.icthh.xm.commons.config.client.repository.TenantConfigRepository;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.ms.entity.config.ApplicationProperties;
import com.icthh.xm.ms.entity.config.XmEntityTenantConfigService;
import com.icthh.xm.ms.entity.domain.spec.FunctionSpec;
import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import com.icthh.xm.ms.entity.security.access.DynamicPermissionCheckService;
import com.icthh.xm.ms.entity.service.XmEntitySpecService;
import com.icthh.xm.ms.entity.service.privileges.custom.EntityCustomPrivilegeService;
import com.icthh.xm.ms.entity.service.processor.DefinitionSpecProcessor;
import com.icthh.xm.ms.entity.service.processor.FormSpecProcessor;
import com.icthh.xm.ms.entity.service.spec.XmEntitySpecContextService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.icthh.xm.ms.entity.config.TenantConfigMockConfiguration.getXmEntitySpec;

@Slf4j
@Service
@Primary
public class LocalXmEntitySpecService extends XmEntitySpecService {

    private final ApplicationProperties applicationProperties;
    private final TenantContextHolder tenantContextHolder;
    private final EntityCustomPrivilegeService entityCustomPrivilegeService;
    private final DynamicPermissionCheckService dynamicPermissionCheckService;
    private final XmEntityTenantConfigService tenantConfigService;

    public LocalXmEntitySpecService(TenantConfigRepository tenantConfigRepository,
                                    ApplicationProperties applicationProperties,
                                    TenantContextHolder tenantContextHolder,
                                    EntityCustomPrivilegeService entityCustomPrivilegeService,
                                    DynamicPermissionCheckService dynamicPermissionCheckService,
                                    XmEntityTenantConfigService tenantConfigService,
                                    DefinitionSpecProcessor definitionSpecProcessor,
                                    FormSpecProcessor formSpecProcessor) {

        super(tenantConfigRepository,
            new XmEntitySpecContextService(definitionSpecProcessor, formSpecProcessor, tenantConfigService, dynamicPermissionCheckService),
            applicationProperties, tenantContextHolder, List.of(entityCustomPrivilegeService));

        this.applicationProperties = applicationProperties;
        this.tenantContextHolder = tenantContextHolder;
        this.entityCustomPrivilegeService = entityCustomPrivilegeService;
        this.dynamicPermissionCheckService = dynamicPermissionCheckService;
        this.tenantConfigService = tenantConfigService;

    }

    @Override
    protected Map<String, TypeSpec> getTypeSpecs() {
        log.warn("OVERRIDEN FUNCTION IS USED");
        String tenantName = tenantContextHolder.getTenantKey();
        String config = getXmEntitySpec(tenantName);
        String key = applicationProperties.getSpecificationPathPattern().replace("{tenantName}", tenantName);
        this.onRefresh(key, config);
        this.refreshFinished(List.of(key));
        return super.getTypeSpecs();
    }

    @Override
    public Optional<FunctionSpec> findFunction(String functionKey) {
        log.warn("OVERRIDEN FUNCTION IS USED");
        return super.findFunction(functionKey)
                .or(() -> {
                    getTypeSpecs(); // trigger refresh config which populates functionsByTenant cache
                    return super.findFunction(functionKey);
                });
    }
}
