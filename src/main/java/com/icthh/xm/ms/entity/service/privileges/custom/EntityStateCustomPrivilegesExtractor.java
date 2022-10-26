package com.icthh.xm.ms.entity.service.privileges.custom;

import com.icthh.xm.ms.entity.config.XmEntityTenantConfigService;
import com.icthh.xm.ms.entity.domain.spec.NextSpec;
import com.icthh.xm.ms.entity.domain.spec.StateSpec;
import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Component
@RequiredArgsConstructor
public class EntityStateCustomPrivilegesExtractor implements CustomPrivilegesExtractor {

    protected static final String SECTION_NAME = "entity-states";
    protected static final String PRIVILEGE_PREFIX = "XMENTITY.STATE.";

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
    public List<String> toPrivilegesList(Map<String, TypeSpec> specs) {
        if (MapUtils.isEmpty(specs)) {
            return List.of();
        }
        return specs.values().stream()
            .flatMap(this::flatSpec)
            .sorted(String::compareTo)
            .collect(Collectors.toList());
    }

    @Override
    public boolean isEnabled(String tenantKey) {
        XmEntityTenantConfigService.XmEntityTenantConfig tenantConfig = tenantConfigService.getXmEntityTenantConfig(tenantKey);
        return tenantConfig.getEntityStates().getDynamicPermissionCheckEnabled();
    }

    protected Stream<String> flatSpec(TypeSpec typeSpec) {
        if (CollectionUtils.isEmpty(typeSpec.getStates())) {
            return Stream.empty();
        }
        final String specKey = typeSpec.getKey();
        return typeSpec
            .getStates()
            .stream()
            .flatMap(spec -> flatState(specKey, spec));
    }

    protected Stream<String> flatState(String specKey, StateSpec stateSpec) {
        if (CollectionUtils.isEmpty(stateSpec.getNext())) {
            return Stream.empty();
        }
        return stateSpec.getNext()
            .stream()
            .map(NextSpec::getStateKey)
            .filter(StringUtils::isNotEmpty)
            .map(nextState -> PRIVILEGE_PREFIX + specKey + "." + stateSpec.getKey() + "." + nextState);
    }

}
