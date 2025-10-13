package com.icthh.xm.ms.entity.service.privileges.custom;

import com.icthh.xm.commons.permission.service.custom.CustomPrivilegesExtractor;
import com.icthh.xm.ms.entity.config.XmEntityTenantConfigService;
import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static java.util.stream.Collectors.toList;

/**
 * PermissionExtractor for dynamic permission based on different typeKey-s from entity spec.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicXmEntityPrivilegesExtractor implements CustomPrivilegesExtractor<TypeSpec> {

    private static final String DYNAMIC_ENTITY_PRIVILEGES_SECTION_NAME = "entity-dynamic-privileges";
    private static final String XM_ENTITY_PRIVILEGE_PREFIX = "XMENTITY.DELETE.";

    private final XmEntityTenantConfigService tenantConfigService;

    @Override
    public String getSectionName() {
        return DYNAMIC_ENTITY_PRIVILEGES_SECTION_NAME;
    }

    @Override
    public String getPrivilegePrefix() {
        return "";
    }

    @Override
    public List<String> toPrivilegesList(Collection<TypeSpec> specs) {
        return specs.stream()
            .filter(it -> Objects.nonNull(it.getKey()))
            .map(it -> XM_ENTITY_PRIVILEGE_PREFIX + it.getKey())
            .collect(toList());
    }

    @Override
    public boolean isEnabled(String tenantKey) {
        return tenantConfigService.getXmEntityTenantConfig().getDynamicTypeKeyPermission().getEntityDeletion();
    }
}
