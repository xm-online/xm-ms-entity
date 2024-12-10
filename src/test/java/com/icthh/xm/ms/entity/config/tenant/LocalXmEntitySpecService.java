package com.icthh.xm.ms.entity.config.tenant;

import com.icthh.xm.commons.config.client.repository.TenantConfigRepository;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.ms.entity.config.ApplicationProperties;
import com.icthh.xm.ms.entity.config.XmEntityTenantConfigService;
import com.icthh.xm.ms.entity.domain.spec.FunctionSpec;
import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import com.icthh.xm.ms.entity.security.access.XmEntityDynamicPermissionCheckService;
import com.icthh.xm.ms.entity.service.XmEntitySpecService;
import com.icthh.xm.ms.entity.service.privileges.custom.EntityCustomPrivilegeService;
import com.icthh.xm.ms.entity.service.spec.DataSpecJsonSchemaService;
import com.icthh.xm.ms.entity.service.spec.XmEntitySpecContextService;

import com.icthh.xm.ms.entity.service.spec.XmEntitySpecCustomizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    public LocalXmEntitySpecService(TenantConfigRepository tenantConfigRepository,
                                    ApplicationProperties applicationProperties,
                                    TenantContextHolder tenantContextHolder,
                                    EntityCustomPrivilegeService entityCustomPrivilegeService,
                                    XmEntityDynamicPermissionCheckService dynamicPermissionCheckService,
                                    XmEntityTenantConfigService tenantConfigService,
                                    XmEntitySpecCustomizer xmEntitySpecCustomizer,
                                    DataSpecJsonSchemaService dataSpecJsonSchemaService,
                                    @Value("${spring.servlet.multipart.max-file-size:1MB}") String maxFileSize) {
        super(tenantConfigRepository, applicationProperties, tenantContextHolder,
            buildSpecService(tenantConfigService, xmEntitySpecCustomizer, dataSpecJsonSchemaService, maxFileSize),
            List.of(entityCustomPrivilegeService), dynamicPermissionCheckService);

        this.applicationProperties = applicationProperties;
        this.tenantContextHolder = tenantContextHolder;
    }

    private static XmEntitySpecContextService buildSpecService(XmEntityTenantConfigService tenantConfigService,
                                                               XmEntitySpecCustomizer xmEntitySpecCustomizer,
                                                               DataSpecJsonSchemaService dataSpecJsonSchemaService, String maxFileSize) {
        XmEntitySpecContextService xmEntitySpecContextService = new XmEntitySpecContextService(dataSpecJsonSchemaService,
            xmEntitySpecCustomizer, tenantConfigService, maxFileSize);
        xmEntitySpecContextService.onApplicationEvent(null);
        return xmEntitySpecContextService;
    }

    @Override
    protected Map<String, TypeSpec> getTypeSpecs() {
        String tenantName = tenantContextHolder.getTenantKey();
        try {
            String config = getXmEntitySpec(tenantName);
            String key = applicationProperties.getSpecificationPathPattern().replace("{tenantName}", tenantName);
            this.onRefresh(key, config);
            this.refreshFinished(List.of(key));
        } catch (Exception e) {
            // For case when entity spec refreshed manually or using XmEntitySpecTestUtils
            log.error("Error during read spec for tenant {} {}", tenantName, e);
        }
        return super.getTypeSpecs();
    }

    @Override
    public Optional<FunctionSpec> findFunction(String functionKey, String httpMethod) {
        return super.findFunction(functionKey, httpMethod)
                .or(() -> {
                    getTypeSpecs(); // trigger refresh config which populates functionsByTenant cache
                    return super.findFunction(functionKey, httpMethod);
                });
    }
}
