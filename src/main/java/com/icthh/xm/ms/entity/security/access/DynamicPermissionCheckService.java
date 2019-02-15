package com.icthh.xm.ms.entity.security.access;
import com.icthh.xm.commons.config.client.service.TenantConfigService;
import com.icthh.xm.commons.permission.service.PermissionCheckService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

@Slf4j
@Service("dynamicPermissionCheckerService")
public class DynamicPermissionCheckService {

    /**
     * Feature switcher implementation
     */
    public enum FeatureContext {

        FUNCTION(DynamicPermissionCheckService::isDynamicFunctionPermissionEnabled),
        CHANGE_STATE(DynamicPermissionCheckService::isDynamicChangeStatePermissionEnabled);

        private final Function<DynamicPermissionCheckService, Boolean> featureContextResolver;

        FeatureContext (Function<DynamicPermissionCheckService, Boolean> featureContextResolver) {
            this.featureContextResolver = featureContextResolver;
        }
    }

    private final TenantConfigService tenantConfigService;
    private final PermissionCheckService permissionCheckService;

    public DynamicPermissionCheckService(TenantConfigService tenantConfigService, PermissionCheckService permissionCheckService) {
        this.tenantConfigService = tenantConfigService;
        this.permissionCheckService = permissionCheckService;
    }

    private BiFunction<PermissionCheckService, String, Boolean> assertPermission =
        (srv, perm) -> srv.hasPermission(SecurityContextHolder.getContext().getAuthentication(), perm);

    /**
     * Checks if user has permission with dynamic key feature
     * if some feature defined by FeatureContext in tenantConfigService enabled TRUE,
     * then check by @checkContextPermission applied P('XXX'.'YYY') otherwise only basePermission evaluated
     * @param featureContext FUNCTION|CHANGE_STATE
     * @param basePermission base permission 'XXX'
     * @param suffix context permission 'YYY'
     * @return result from PermissionCheckService.hasPermission
     */
    public boolean checkContextPermission(FeatureContext featureContext, String basePermission, String suffix) {
        if (featureContext.featureContextResolver.apply(this)) {
            return checkContextPermission(basePermission, suffix);
        }
        return assertPermission.apply(permissionCheckService, basePermission).booleanValue();
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
        return assertPermission.apply(permissionCheckService, permission).booleanValue();
    }

    /**
     * Checks if feature tenant-config -> functions -> dynamic enabled
     * @return true if feature enabled
     */
    private boolean isDynamicFunctionPermissionEnabled() {
        return getTenantConfigBooleanParameterValue("xxx", "")
            .map(it -> (Boolean)it).orElse(Boolean.FALSE).booleanValue();
    }

    /**
     * Checks if feature tenant-config -> stateChange -> dynamic enabled
     * @return true if feature enabled
     */
    private Boolean isDynamicChangeStatePermissionEnabled() {
        return getTenantConfigBooleanParameterValue("xxx", "")
            .map(it -> (Boolean)it).orElse(Boolean.FALSE).booleanValue();
    }

    // TODO should be in Commons.TenantConfigService as utility
    private Optional<Object> getTenantConfigBooleanParameterValue(final String configSection, String parameter) {
        Objects.requireNonNull(configSection, "configSection can't be null");
        Objects.requireNonNull(parameter, "parameter can't be null");
        return Optional.ofNullable(tenantConfigService.getConfig().get(configSection))
            .filter(it -> it instanceof Map).map(Map.class::cast)
            .map(it -> it.get(parameter));
    }

}
