package com.icthh.xm.ms.entity.service.privileges.custom;

import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

/**
 * PermissionExtractor for dynamic permission based on different typeKey-s from entity spec.
 */
@Slf4j
@Component
public class DynamicEntityLinkPrivilegesExtractor implements CustomPrivilegesExtractor {

    private static final String DYNAMIC_ENTITY_PRIVILEGES_SECTION_NAME = "entity-dynamic-privileges";
    private static final String LINK_PRIVILEGE_PREFIX = "LINK.DELETE.";

    @Override
    public String getSectionName() {
        return DYNAMIC_ENTITY_PRIVILEGES_SECTION_NAME;
    }

    @Override
    public String getPrivilegePrefix() {
        return "";
    }

    @Override
    public List<String> toPrivilegesList(Map<String, TypeSpec> specs) {
        return specs.values().stream()
                .flatMap(it -> Stream.ofNullable(it.getLinks()))
                .flatMap(Collection::stream)
                .map(it -> LINK_PRIVILEGE_PREFIX + it.getTypeKey())
                .collect(toList());
    }

}
