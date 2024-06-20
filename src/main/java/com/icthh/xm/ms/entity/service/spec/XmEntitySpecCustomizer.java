package com.icthh.xm.ms.entity.service.spec;

import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.api.LepManagementService;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Map;

import static com.icthh.xm.commons.tenant.TenantContextUtils.buildTenant;

@Component
@LepService(group = "service.spec")
public class XmEntitySpecCustomizer {

    private final TenantContextHolder tenantContextHolder;
    private final LepManagementService lepManagementService;
    private final XmEntitySpecCustomizer self;

    public XmEntitySpecCustomizer(TenantContextHolder tenantContextHolder,
                                  LepManagementService lepManagementService,
                                  @Lazy XmEntitySpecCustomizer self) {
        this.tenantContextHolder = tenantContextHolder;
        this.lepManagementService = lepManagementService;
        this.self = self;
    }

    public void customize(String tenant, Map<String, TypeSpec> entitySpec) {
        tenantContextHolder.getPrivilegedContext().execute(buildTenant(tenant), () -> {
            try(var context = lepManagementService.beginThreadContext()) {
                self.customize(entitySpec);
            }
        });
    }

    @LogicExtensionPoint("CustomizeEntitySpec")
    public void customize(Map<String, TypeSpec> entitySpec) {
        // do nothing
    }

}
