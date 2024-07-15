package com.icthh.xm.ms.entity.service.spec;

import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.XmLepScriptConfigServerResourceLoader;
import com.icthh.xm.commons.lep.api.LepManagementService;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.commons.logging.aop.IgnoreLogginAspect;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

import static com.icthh.xm.commons.tenant.TenantContextUtils.buildTenant;

@Component
@LepService(group = "service.spec")
public class XmEntitySpecCustomizer {

    private final TenantContextHolder tenantContextHolder;
    private final LepManagementService lepManagementService;
    private final XmLepScriptConfigServerResourceLoader dependsOn; // to start after LEP
    private XmEntitySpecCustomizer self;

    public XmEntitySpecCustomizer(TenantContextHolder tenantContextHolder,
                                  LepManagementService lepManagementService,
                                  XmLepScriptConfigServerResourceLoader xmLepScriptConfigServerResourceLoader) {
        this.tenantContextHolder = tenantContextHolder;
        this.lepManagementService = lepManagementService;
        this.dependsOn = xmLepScriptConfigServerResourceLoader;
    }

    @IgnoreLogginAspect
    public void customize(String tenant, Map<String, TypeSpec> entitySpec) {
        if (lepManagementService.getCurrentLepExecutorResolver() == null) {
            tenantContextHolder.getPrivilegedContext().execute(buildTenant(tenant), () -> {
                try(var context = lepManagementService.beginThreadContext()) {
                    self.customize(entitySpec);
                }
            });
        } else {
            self.customize(entitySpec);
        }
    }

    @IgnoreLogginAspect
    @LogicExtensionPoint("CustomizeEntitySpec")
    public void customize(Map<String, TypeSpec> entitySpec) {
        // do nothing
    }

    @Autowired
    public void setXmEntitySpecCustomizer(XmEntitySpecCustomizer xmEntitySpecCustomizer) {
        this.self = xmEntitySpecCustomizer;
    }

}
