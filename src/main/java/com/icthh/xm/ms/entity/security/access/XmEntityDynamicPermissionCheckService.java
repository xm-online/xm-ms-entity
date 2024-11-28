package com.icthh.xm.ms.entity.security.access;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.icthh.xm.commons.logging.aop.IgnoreLogginAspect;
import com.icthh.xm.commons.permission.domain.Permission;
import com.icthh.xm.commons.permission.service.AbstractDynamicPermissionCheckService;
import com.icthh.xm.commons.permission.service.PermissionCheckService;
import com.icthh.xm.commons.permission.service.PermissionService;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.ms.entity.config.XmEntityTenantConfigService;
import com.icthh.xm.ms.entity.security.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.icthh.xm.commons.permission.constants.RoleConstant.SUPER_ADMIN;
import static com.icthh.xm.ms.entity.util.CustomCollectionUtils.nullSafe;

@Slf4j
@Validated
@Service
public class XmEntityDynamicPermissionCheckService extends AbstractDynamicPermissionCheckService {

    /**
     * Permission aggregation section in custom privileges
     */
    public static final String CONFIG_SECTION = "entity-functions";

    private final XmEntityTenantConfigService tenantConfigService;
    private final PermissionService permissionService;
    private final TenantContextHolder tenantContextHolder;

    public XmEntityDynamicPermissionCheckService(PermissionCheckService permissionCheckService,
                                                 XmAuthenticationContextHolder xmAuthenticationContextHolder,
                                                 XmEntityTenantConfigService tenantConfigService,
                                                 PermissionService permissionService,
                                                 TenantContextHolder tenantContextHolder) {
        super(permissionCheckService, xmAuthenticationContextHolder);
        this.tenantConfigService = tenantConfigService;
        this.permissionService = permissionService;
        this.tenantContextHolder = tenantContextHolder;
    }

    @IgnoreLogginAspect
    public <T, I> T filterInnerListByPermission(final T outterType,
                                                Supplier<List<I>> innerGetter,
                                                Consumer<List<I>> innerSetter,
                                                Function<I, String> innerKeyGetter) {

        if (!FeatureContext.FUNCTION.isEnabled(this)) {
            return outterType;
        }

        if (isSuperAdmin()) {
            return outterType;
        }

        List<I> filteredList = Lists.newArrayList();

        Set<String> lPermissions = getRoleFunctionPermissions();

        if (!lPermissions.isEmpty()) {
            filteredList = nullSafe(innerGetter.get())
                                      .stream()
                                      .filter(item -> lPermissions.contains(innerKeyGetter.apply(item)))
                                      .collect(Collectors.toList());
        }

        innerSetter.accept(filteredList);

        return outterType;

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
     */
    Predicate<Permission> functionPermissionMatcher(String roleKey) {

        Predicate<Permission> isConfigSection = permission -> StringUtils.equals(CONFIG_SECTION, permission.getMsName());
        Predicate<Permission> isAssignedToRole = permission -> StringUtils.equals(roleKey, permission.getRoleKey());
        Predicate<Permission> isEnabled = permission -> !permission.isDisabled();
        Predicate<Permission> isIsNotDeleted = permission -> !permission.isDeleted();

        if (SUPER_ADMIN.equals(roleKey)) {
            return isConfigSection.and(isEnabled).and(isIsNotDeleted);
        }

        return isConfigSection
            .and(isAssignedToRole)
            .and(isEnabled)
            .and(isIsNotDeleted);
    }

    /**
     * Checks if feature tenant-config -> functions -> dynamic enabled
     * @return true if feature enabled
     */
    public boolean isDynamicFunctionPermissionEnabled() {
        return tenantConfigService.getXmEntityTenantConfig().getEntityFunctions().getDynamicPermissionCheckEnabled();
    }

    public boolean isDynamicLinkDeletePermissionEnabled() {
        return tenantConfigService.getXmEntityTenantConfig().getDynamicTypeKeyPermission().getLinkDeletion();
    }

    /**
     * Checks if feature tenant-config -> stateChange -> dynamic enabled
     * @return true if feature enabled
     */
    public boolean isDynamicChangeStatePermissionEnabled() {
        //TODO feature discussion needed
        throw new UnsupportedOperationException("isDynamicChangeStatePermissionEnabled Not implementer");
    }

    private boolean isSuperAdmin() {
        return SecurityUtils.getCurrentUserRole().filter(SUPER_ADMIN::equals).isPresent();
    }

}

