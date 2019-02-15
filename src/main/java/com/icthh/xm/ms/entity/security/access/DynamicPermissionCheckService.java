package com.icthh.xm.ms.entity.security.access;
import com.icthh.xm.commons.config.client.service.TenantConfigService;
import com.icthh.xm.commons.permission.service.PermissionCheckService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

@Slf4j
@Service("dynamicPermissionCheckerService")
public class DynamicPermissionCheckService {

    private final TenantConfigService tenantConfigService;
    private final PermissionCheckService permissionCheckService;

    public DynamicPermissionCheckService(TenantConfigService tenantConfigService, PermissionCheckService permissionCheckService) {
        this.tenantConfigService = tenantConfigService;
        this.permissionCheckService = permissionCheckService;
    }

    /**
     * Checks if user has permission with dynamic key feature
     * if some feature defined by contextSwitch in tenantConfigService TRUE then check by @checkContextPermission applied P('XXX'.'YYY') otherwise only basePermission evaluated
     * @param contextSwitch on/off switch
     * @param basePermission base permission 'XXX'
     * @param suffix context permission 'YYY'
     * @return result from PermissionCheckService.hasPermission
     */
    public boolean checkContextPermission(Function<Map<String, Object>, Boolean> contextSwitch, String basePermission, String suffix) {
        Objects.requireNonNull(contextSwitch, "contextSwitch can't be null");
        if (contextSwitch.apply(tenantConfigService.getConfig())) {
            return checkContextPermission(basePermission, suffix);
        }
        return permissionCheckService.hasPermission(SecurityContextHolder.getContext().getAuthentication(), basePermission);
    }

    /**
     * Checks if user has permission with dynamic key feature
     * @param basePermission - base permission
     * @param suffix - suffix
     * @return result result from PermissionCheckService.hasPermission(basePermission + '.' + suffix) from PermissionCheckService.hasPermission
     */
    public boolean checkContextPermission(String basePermission, String suffix) {
        Objects.requireNonNull(basePermission, "basePermission can't be null");
        Objects.requireNonNull(suffix, "suffix can't be null");
        final String permission = basePermission + "." + suffix;
        return permissionCheckService.hasPermission(SecurityContextHolder.getContext().getAuthentication(), permission);
    }

    // TODO create general purpose function to handle String, Integer, Boolean result
    public Function<Map<String, Object>, Boolean> getTenantConfigBooleanParameterValue(final String configSection, String parameter, boolean defaultValue) {
        Objects.requireNonNull(configSection, "configSection can't be null");
        Objects.requireNonNull(parameter, "parameter can't be null");
        return (map) -> Optional.ofNullable(tenantConfigService.getConfig().get(configSection))
            .filter(it -> it instanceof Map).map(Map.class::cast)
            .map(it -> it.get(parameter)).map(it -> (boolean)it).orElse(defaultValue);
    }

    public Function<Map<String, Object>, Boolean> getTenantConfigBooleanParameterValue(final String configSection, String parameter) {
        return getTenantConfigBooleanParameterValue(configSection, parameter, false);
    }

}
