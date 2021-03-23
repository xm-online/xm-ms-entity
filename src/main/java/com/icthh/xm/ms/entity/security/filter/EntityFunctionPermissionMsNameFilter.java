package com.icthh.xm.ms.entity.security.filter;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.startsWithIgnoreCase;

import com.icthh.xm.commons.permission.service.filter.PermissionMsNameFilter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component("permissionMsNameFilter")
public class EntityFunctionPermissionMsNameFilter implements PermissionMsNameFilter {

    private static final String CUSTOM_PRIVILEGES_SECTION_PREFIX = "entity-";

    @Value("${spring.application.name}")
    private String msName;

    @Override
    public boolean filterPermission(String permissionMsName) {
        return isBlank(msName) || StringUtils.equals(msName, permissionMsName)
            || (isNotBlank(permissionMsName) && startsWithIgnoreCase(permissionMsName, CUSTOM_PRIVILEGES_SECTION_PREFIX));
    }
}
