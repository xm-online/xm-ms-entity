package com.icthh.xm.ms.entity.security.access;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.icthh.xm.commons.config.client.service.TenantConfigService;
import com.icthh.xm.commons.permission.domain.Permission;
import com.icthh.xm.commons.permission.service.PermissionCheckService;
import com.icthh.xm.commons.permission.service.PermissionService;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.ms.entity.security.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.icthh.xm.ms.entity.util.CustomCollectionUtils.nullSafe;

@Slf4j
@Validated
@Service("dynamicPermissionCheckService")
public class DynamicPermissionCheckService {

    /**
     * Permission aggregation section in tenant config
     */
    public static final String CONFIG_SECTION = "entity-functions";
    /**
     * Feature name for DynamicFunctionPermission validation
     */
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

        /**
         * Checks if feature FUNCTION|CHANGE_STATE enabled in tenant config
         * @param service - DynamicPermissionCheckService instance
         * @return true if enabled
         */
        private boolean isEnabled(DynamicPermissionCheckService service) {
            return this.featureContextResolver.apply(service).booleanValue();
        }

    }

    private final TenantConfigService tenantConfigService;
    private final PermissionCheckService permissionCheckService;
    private final PermissionService permissionService;
    private final TenantContextHolder tenantContextHolder;

    private BiFunction<PermissionCheckService, String, Boolean> assertPermission =
        (service, permission) -> service.hasPermission(SecurityContextHolder.getContext().getAuthentication(), permission);

    public DynamicPermissionCheckService(TenantConfigService tenantConfigService,
                                         PermissionCheckService permissionCheckService,
                                         PermissionService permissionService,
                                         TenantContextHolder tenantContextHolder) {
        this.tenantConfigService = tenantConfigService;
        this.permissionCheckService = permissionCheckService;
        this.permissionService = permissionService;
        this.tenantContextHolder = tenantContextHolder;
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
        if (featureContext.isEnabled(this)) {
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
     * Performs object field adjustment if feature is enabled. Function is generified, as it could be re used with other mappers.
     * @param mapper - adjustment implementation
     * @param <T> - collection element
     * @return same collection or adjusted one
     */
    public <T> Function<List<T>, List<T>> dynamicFunctionListFilter(BiFunction<T, Set<String>, T> mapper) {
        if (FeatureContext.FUNCTION.isEnabled(this)) {
            final Set<String> rolePermissionSet = getRoleFunctionPermissions();
            return list -> list.stream().map(item -> mapper.apply(item, rolePermissionSet)).collect(Collectors.toList());
        }
        return list -> list;
    }

    /**
     * Performs object field adjustment if feature is enabled. Function is generified, as it could be re used with other mappers.
     * @param mapper - adjustment implementation
     * @param <T> - collection element
     * @return same collection or adjusted one
     */
    public <T> Function<T, T> dynamicFunctionFilter(BiFunction<T, Set<String>, T> mapper) {
        if (FeatureContext.FUNCTION.isEnabled(this)) {
            final Set<String> rolePermissionSet = getRoleFunctionPermissions();
            return item -> mapper.apply(item, rolePermissionSet);
        }
        return item -> item;
    }

    /**
     * Function should return set of custom.dynamicFunctionFeature permissions assigned to role in current security scope
     * @return set
     */
    Set<String> getRoleFunctionPermissions() {

        //TODO throw error here, after migration to new test paradigm
        final Optional<String> userRole = SecurityUtils.getCurrentUserRole();
        if (!userRole.isPresent()) {
            return Sets.newHashSet();
        }

        Map<String, Permission> permissions = permissionService.getPermissions(
            TenantContextUtils.getRequiredTenantKeyValue(tenantContextHolder.getContext()));
        return nullSafe(permissions).values().stream()
            .filter(functionPermissionMatcher(userRole.get()))
            .map(Permission::getPrivilegeKey)
            .collect(Collectors.toSet());
    }

    /**
     * is permission belongs to functional or not?
     * @param roleKey
     * @return
     */
    Predicate<Permission> functionPermissionMatcher(String roleKey) {
        Predicate<Permission> isConfigSection = permission -> StringUtils.equals(CONFIG_SECTION, permission.getMsName());
        Predicate<Permission> isAssignedToRole = permission -> StringUtils.equals(roleKey, permission.getRoleKey());
        return isConfigSection.and(isAssignedToRole);
    }

    /**
     * Assert permission via permissionCheckService.hasPermission
     * @param permission
     * @return
     */
    protected boolean assertPermission(final String permission) {
        Preconditions.checkArgument(StringUtils.isNotEmpty(permission));
        final boolean permitted = assertPermission.apply(permissionCheckService, permission).booleanValue();
        if (!permitted) {
            //TODO place correct error here
            throw new IllegalStateException("Throw correct exception here");
        }
        return Boolean.TRUE.booleanValue();
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
        //TODO feature discussion needed
        throw new UnsupportedOperationException("isDynamicChangeStatePermissionEnabled Not implementer");
    }

    // TODO should be in Commons.TenantConfigService as utility method
    private Optional<Object> getTenantConfigBooleanParameterValue(final String configSection, String parameter) {
        return Optional.ofNullable(tenantConfigService.getConfig().get(configSection))
            .filter(it -> it instanceof Map).map(Map.class::cast)
            .map(it -> it.get(parameter));
    }

}
