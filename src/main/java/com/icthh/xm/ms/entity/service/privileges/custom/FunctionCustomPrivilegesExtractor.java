package com.icthh.xm.ms.entity.service.privileges.custom;

import static com.google.common.base.Predicates.not;
import static com.icthh.xm.ms.entity.service.privileges.custom.CustomPrivilegesExtractor.DefaultPrivilegesValue.NONE;
import static java.util.stream.Collectors.toList;

import com.icthh.xm.ms.entity.config.XmEntityTenantConfigService;
import com.icthh.xm.ms.entity.config.XmEntityTenantConfigService.XmEntityTenantConfig;
import com.icthh.xm.ms.entity.domain.spec.FunctionSpec;
import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FunctionCustomPrivilegesExtractor implements CustomPrivilegesExtractor {

    private static final String SECTION_NAME = "entity-functions";
    private static final String PRIVILEGE_PREFIX = "FUNCTION.CALL.";

    private final XmEntityTenantConfigService tenantConfigService;

    @Override
    public String getSectionName() {
        return SECTION_NAME;
    }

    @Override
    public String getPrivilegePrefix() {
        return PRIVILEGE_PREFIX;
    }

    @Override
    public DefaultPrivilegesValue getDefaultValue() {
        return NONE;
    }

    @Override
    public List<String> toPrivilegesList(Map<String, TypeSpec> specs) {
        return specs.values().stream()
                             .map(TypeSpec::getFunctions)
                             .filter(Objects::nonNull)
                             .flatMap(List::stream)
                             .distinct()// avoid duplication function key after extends entity specs
                             .filter(this::filterFunctions)
                             .map(FunctionSpec::getKey)
                             .collect(toList());
    }

    protected boolean filterFunctions(FunctionSpec functionSpec) {
        return !functionSpec.getWithEntityId();
    }

    @Override
    public boolean isEnabled(String tenantKey) {
        XmEntityTenantConfig tenantConfig = tenantConfigService.getXmEntityTenantConfig(tenantKey);
        return tenantConfig.getEntityFunctions().getDynamicPermissionCheckEnabled();
    }
}
