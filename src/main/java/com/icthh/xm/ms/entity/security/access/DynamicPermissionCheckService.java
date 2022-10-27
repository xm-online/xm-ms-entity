package com.icthh.xm.ms.entity.security.access;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.icthh.xm.commons.logging.aop.IgnoreLogginAspect;
import com.icthh.xm.commons.permission.domain.Permission;
import com.icthh.xm.commons.permission.service.PermissionCheckService;
import com.icthh.xm.commons.permission.service.PermissionService;
import com.icthh.xm.commons.security.XmAuthenticationContext;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.ms.entity.config.XmEntityTenantConfigService;
import com.icthh.xm.ms.entity.domain.spec.FunctionSpec;
import com.icthh.xm.ms.entity.domain.spec.NextSpec;
import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import com.icthh.xm.ms.entity.security.SecurityUtils;
import com.icthh.xm.ms.entity.service.privileges.custom.EntityStateCustomPrivilegesExtractor;
import com.icthh.xm.ms.entity.service.privileges.custom.FunctionCustomPrivilegesExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.icthh.xm.commons.permission.constants.RoleConstant.SUPER_ADMIN;
import static com.icthh.xm.ms.entity.util.CustomCollectionUtils.nullSafe;
import static java.lang.String.format;

@Slf4j
@Validated
@RequiredArgsConstructor
@Service
public class DynamicPermissionCheckService {

    protected static Set<String> PERMISSION_SECTIONS = Set.of(FunctionCustomPrivilegesExtractor.SECTION_NAME, EntityStateCustomPrivilegesExtractor.SECTION_NAME);

    /**
     * Feature switcher implementation
     */
    public enum FeatureContext {

        FUNCTION(DynamicPermissionCheckService::isDynamicFunctionPermissionEnabled),
        CHANGE_STATE(DynamicPermissionCheckService::isDynamicChangeStatePermissionEnabled),
        LINK_DELETE(DynamicPermissionCheckService::isDynamicLinkDeletePermissionEnabled);

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
            return this.featureContextResolver.apply(service);
        }

    }

    private final XmEntityTenantConfigService tenantConfigService;
    private final PermissionCheckService permissionCheckService;
    private final PermissionService permissionService;
    private final TenantContextHolder tenantContextHolder;
    private final XmAuthenticationContextHolder xmAuthenticationContextHolder;

    /**
     * Checks if user has permission with dynamic key feature
     * if some feature defined by FeatureContext in tenantConfigService enabled TRUE,
     * then check by @checkContextPermission applied P('XXX'.'YYY') otherwise only basePermission evaluated
     * @param featureContext FUNCTION|CHANGE_STATE
     * @param basePermission base permission 'XXX'
     * @param suffixSupplier supplier of permission suffix
     * @return result from PermissionCheckService.hasPermission
     */
    @IgnoreLogginAspect
    public boolean checkContextPermission(FeatureContext featureContext, String basePermission, Supplier<String> suffixSupplier) {
        Preconditions.checkArgument(StringUtils.isNotEmpty(basePermission));
        if (featureContext.isEnabled(this)) {
            String suffix = suffixSupplier.get();
            Preconditions.checkArgument(StringUtils.isNotEmpty(suffix));
            return checkContextPermission(basePermission, suffix);
        }
        return assertPermission(basePermission);
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
    @IgnoreLogginAspect
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
     * @return result value from PermissionCheckService.hasPermission(permission) from assertPermission
     */
    private boolean checkContextPermission(String basePermission, String suffix) {
        Preconditions.checkArgument(StringUtils.isNotEmpty(basePermission));
        Preconditions.checkArgument(StringUtils.isNotEmpty(suffix));
        final String permission = basePermission + "." + suffix;
        return assertPermission(permission);
    }

    public TypeSpec evaluateDynamicPermissions(TypeSpec spec) {

        TypeSpec specClone = spec.toBuilder().build();

        if (isSuperAdmin()) {
            return specClone;
        }

        Set<String> lPermissions = getRoleFunctionPermissions();

        specClone = filterInnerFunctionListByPermission(lPermissions, specClone,
            specClone::getFunctions,
            specClone::setFunctions,
            FunctionSpec::getDynamicPrivilegeKey);

        specClone = filterInnerStateListByPermission(lPermissions, specClone);

        return specClone;
    }

    @IgnoreLogginAspect
    protected  <T, I> T filterInnerFunctionListByPermission(
        final Set<String> permissions, final T outerType, Supplier<List<I>> innerGetter,
        Consumer<List<I>> innerSetter, Function<I, String> innerKeyGetter) {

        if (!FeatureContext.FUNCTION.isEnabled(this)) {
            return outerType;
        }

        List<I> filteredList = Lists.newArrayList();


        if (!permissions.isEmpty()) {
            filteredList = nullSafe(innerGetter.get())
                                      .stream()
                                      .filter(item -> permissions.contains(innerKeyGetter.apply(item)))
                                      .collect(Collectors.toList());
        }

        innerSetter.accept(filteredList);

        return outerType;

    }

    @IgnoreLogginAspect
    protected  TypeSpec filterInnerStateListByPermission(
        final Set<String> permissions, final TypeSpec typeSpec) {

        if (!FeatureContext.CHANGE_STATE.isEnabled(this)) {
            return typeSpec;
        }

        String key = typeSpec.getKey();

        nullSafe(typeSpec.getStates()).forEach(stateSpec -> {
            List<NextSpec> filteredNextStates = nullSafe(stateSpec.getNext()).stream()
                .filter(nextSpec -> permissions.contains(
                    EntityStateCustomPrivilegesExtractor.buildPermissionKey(key, stateSpec.getKey(), nextSpec.getStateKey())))
                .collect(Collectors.toList());
            stateSpec.setNext(filteredNextStates);
        });

        return typeSpec;
    }

    /**
     * Function should return set of custom.dynamicFunctionFeature permissions assigned to role in current security scope
     * @return set
     */
    Set<String> getRoleFunctionPermissions() {

        //TODO throw error here, after migration to new test paradigm
        final Optional<String> userRole = SecurityUtils.getCurrentUserRole();
        if (userRole.isEmpty()) {
            return Sets.newHashSet();
        }

        String tenantKey = TenantContextUtils.getRequiredTenantKeyValue(tenantContextHolder.getContext());
        Map<String, Permission> permissions = permissionService.getPermissions(tenantKey);

        return nullSafe(permissions).values().stream()
                                    .filter(functionPermissionMatcher(userRole.get()))
                                    .map(Permission::getPrivilegeKey)
                                    .collect(Collectors.toSet());
    }

    /**
     * is permission belongs to functional or not?
     * @param roleKey current user role key
     */
    Predicate<Permission> functionPermissionMatcher(String roleKey) {

        Predicate<Permission> isConfigSection = permission -> PERMISSION_SECTIONS.contains(permission.getMsName());
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
     * Assert permission via permissionCheckService.hasPermission
     * @param permission Permission
     */
    private boolean assertPermission(final String permission) {
        Preconditions.checkArgument(StringUtils.isNotEmpty(permission));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean permitted = permissionCheckService
            .hasPermission(authentication, permission);

        if (!permitted) {
            String msg = format("access denied: privilege=%s, roleKey=%s, user=%s due to privilege is not permitted",
                    permission, getRoleKey(authentication), getUserKey());
            throw new AccessDeniedException(msg);
        }
        return true;
    }

    private String getUserKey() {
        return Optional.ofNullable(xmAuthenticationContextHolder)
                .map(XmAuthenticationContextHolder::getContext)
                .flatMap(XmAuthenticationContext::getUserKey)
                .orElse(null);
    }

    private static String getRoleKey(Authentication authentication) {
        return Optional.ofNullable(authentication)
                .map(Authentication::getAuthorities)
                .map(Collection::stream)
                .flatMap(Stream::findFirst)
                .map(GrantedAuthority::getAuthority)
                .orElse(null);
    }

    /**
     * Checks if feature tenant-config -> functions -> dynamic enabled
     * @return true if feature enabled
     */
    private boolean isDynamicFunctionPermissionEnabled() {
        return tenantConfigService.getXmEntityTenantConfig().getEntityFunctions().getDynamicPermissionCheckEnabled();
    }

    public boolean isDynamicLinkDeletePermissionEnabled() {
        return tenantConfigService.getXmEntityTenantConfig().getDynamicTypeKeyPermission().getLinkDeletion();
    }

    /**
     * Checks if feature tenant-config -> stateChange -> dynamic enabled
     * @return true if feature enabled
     */
    private boolean isDynamicChangeStatePermissionEnabled() {
        return tenantConfigService.getXmEntityTenantConfig().getEntityStates().getDynamicPermissionCheckEnabled();
    }

    private boolean isSuperAdmin() {
        return SecurityUtils.getCurrentUserRole().filter(SUPER_ADMIN::equals).isPresent();
    }

}

