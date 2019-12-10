package com.icthh.xm.ms.entity.service.privileges.custom;

import static com.google.common.collect.ImmutableMap.of;
import static com.icthh.xm.ms.entity.service.privileges.custom.CustomPrivilegesExtractor.DefaultPrivilegesValue.NONE;
import static java.util.stream.Collectors.toList;

import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import java.util.List;
import java.util.Map;

public interface CustomPrivilegesExtractor {

    String getSectionName();

    String getPrivilegePrefix();

    default List<Map<String, Object>> toPrivileges(Map<String, TypeSpec> specs) {
        return toPrivilegesList(specs).stream()
                                      .map(this::toPrivilege)
                                      .collect(toList());
    }

    default Map<String, Object> toPrivilege(String key) {
        return of("key", getPrivilegePrefix() + key);
    }

    List<String> toPrivilegesList(Map<String, TypeSpec> specs);

    default DefaultPrivilegesValue getDefaultValue() {
        return NONE;
    }

    default boolean isEnabled(String tenantKey) {
        return true;
    }

    enum DefaultPrivilegesValue {
        NONE, ENABLED, DISABLED;
    }

}
