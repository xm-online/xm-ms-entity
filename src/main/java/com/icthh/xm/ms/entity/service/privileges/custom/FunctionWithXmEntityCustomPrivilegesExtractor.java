package com.icthh.xm.ms.entity.service.privileges.custom;

import com.icthh.xm.ms.entity.config.XmEntityTenantConfigService;
import com.icthh.xm.ms.entity.domain.spec.FunctionSpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FunctionWithXmEntityCustomPrivilegesExtractor extends FunctionCustomPrivilegesExtractor {

    private static final String PRIVILEGE_PREFIX = "XMENTITY.FUNCTION.EXECUTE.";

    public FunctionWithXmEntityCustomPrivilegesExtractor(XmEntityTenantConfigService tenantConfigService) {
        super(tenantConfigService);
    }

    @Override
    public String getPrivilegePrefix() {
        return PRIVILEGE_PREFIX;
    }

    @Override
    protected boolean filterFunctions(FunctionSpec functionSpec) {
        return functionSpec.getWithEntityId();
    }
}
