package com.icthh.xm.ms.entity.security.access;
import com.google.common.base.Preconditions;
import com.icthh.xm.commons.config.client.service.TenantConfigService;
import com.icthh.xm.commons.permission.service.PermissionCheckService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

@Slf4j
@Validated
@Service("dynamicPermissionCheckService")
public class DynamicPermissionCheckService {

    public static final String CONFIG_SECTION = "entity-functions";
    public static final String DYNAMIC_FUNCTION_PERMISSION_FEATURE = "dynamicPermissionCheckEnabled";

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

    private BiFunction<PermissionCheckService, String, Boolean> assertPermission =
        (service, permission) -> service.hasPermission(SecurityContextHolder.getContext().getAuthentication(), permission);

    public DynamicPermissionCheckService(TenantConfigService tenantConfigService, PermissionCheckService permissionCheckService) {
        this.tenantConfigService = tenantConfigService;
        this.permissionCheckService = permissionCheckService;
    }

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
        Preconditions.checkArgument(StringUtils.isNotEmpty(basePermission));
        Preconditions.checkArgument(StringUtils.isNotEmpty(suffix));
        if (featureContext.featureContextResolver.apply(this)) {
            return checkContextPermission(basePermission, suffix);
        }
        return assertPermission(basePermission);
    }

    /**
     * Checks if user has permission with dynamic key feature permission = basePermission + "." + suffix
     * @param basePermission - base permission
     * @param suffix - suffix
     * @return result result from PermissionCheckService.hasPermission(permission) from assertPermission
     */
    public boolean checkContextPermission(String basePermission, String suffix) {
        Preconditions.checkArgument(StringUtils.isNotEmpty(basePermission));
        Preconditions.checkArgument(StringUtils.isNotEmpty(suffix));
        final String permission = basePermission + "." + suffix;
        return assertPermission(permission);
    }

    /**
     * Assert permission via permissionCheckService.hasPermission
     * @param permission
     * @return
     */
    protected boolean assertPermission(final String permission) {
        Preconditions.checkArgument(StringUtils.isNotEmpty(permission));
        return assertPermission.apply(permissionCheckService, permission).booleanValue();
    }

    /**
     * Checks if feature tenant-config -> functions -> dynamic enabled
     * @return true if feature enabled
     */
    private boolean isDynamicFunctionPermissionEnabled() {
        return getTenantConfigBooleanParameterValue(CONFIG_SECTION, DYNAMIC_FUNCTION_PERMISSION_FEATURE)
            .map(it -> (Boolean)it).orElse(Boolean.FALSE).booleanValue();
    }

    /**
     * Checks if feature tenant-config -> stateChange -> dynamic enabled
     * @return true if feature enabled
     */
    private boolean isDynamicChangeStatePermissionEnabled() {
        throw new UnsupportedOperationException(this + " Not implementer");
        /*return getTenantConfigBooleanParameterValue("xxx", "")
            .map(it -> (Boolean)it).orElse(Boolean.FALSE).booleanValue();*/
    }

    // TODO should be in Commons.TenantConfigService as utility method
    private Optional<Object> getTenantConfigBooleanParameterValue(final String configSection, String parameter) {
        return Optional.ofNullable(tenantConfigService.getConfig().get(configSection))
            .filter(it -> it instanceof Map).map(Map.class::cast)
            .map(it -> it.get(parameter));
    }

}
