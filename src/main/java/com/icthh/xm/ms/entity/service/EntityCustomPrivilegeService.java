package com.icthh.xm.ms.entity.service;

import static com.google.common.collect.ImmutableMap.of;
import static com.icthh.xm.commons.config.client.repository.TenantConfigRepository.TENANT_NAME;
import static java.lang.Boolean.TRUE;
import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.config.client.repository.CommonConfigRepository;
import com.icthh.xm.commons.config.domain.Configuration;
import com.icthh.xm.commons.permission.config.PermissionProperties;
import com.icthh.xm.commons.permission.domain.Permission;
import com.icthh.xm.commons.permission.domain.Role;
import com.icthh.xm.commons.permission.service.RoleService;
import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Predicate;

@Slf4j
@Service
@RequiredArgsConstructor
public class EntityCustomPrivilegeService {

    private static final String CUSTOMER_PRIVILEGES_PATH = "/config/tenants/{" + TENANT_NAME + "}/custom-privileges.yml";
    private static final String SECTION_NAME = "applications";
    private static final String APPLICATION_PRIVILEGE_PREFIX = "APPLICATION.";

    private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    private final CommonConfigRepository commonConfigRepository;
    private final PermissionProperties permissionProperties;
    private final RoleService roleService;

    public void updateApplicationPermission(Map<String, TypeSpec> specs, String tenantKey) {

        String privilegesPath = resolvePathWithTenant(tenantKey, CUSTOMER_PRIVILEGES_PATH);
        String specPath = permissionProperties.getPermissionsSpecPath();
        String permissionsSpecPath = resolvePathWithTenant(tenantKey, specPath);

        log.info("Get config from {} and {}", privilegesPath, permissionsSpecPath);
        List<String> paths = asList(privilegesPath, permissionsSpecPath);
        Map<String, Configuration> configs = commonConfigRepository.getConfig(null, paths);
        configs = configs != null ? configs : new HashMap<>();

        updateApplicationPrivileges(specs, privilegesPath, configs.get(privilegesPath));
        enableApplicationPermissionByDefault(specs, tenantKey, permissionsSpecPath, configs.get(permissionsSpecPath));
    }

    private String resolvePathWithTenant(String tenantKey, String specPath) {
        return specPath.replace("{" + TENANT_NAME + "}", tenantKey);
    }

    @SneakyThrows
    private void enableApplicationPermissionByDefault(Map<String, TypeSpec> specs,
                                                      String tenantKey,
                                                      String permissionsSpecPath,
                                                      Configuration permissionsSpec) {

        List<String> applications = specs.values().stream()
            .filter(it -> TRUE.equals(it.getIsApp()))
            .map(TypeSpec::getKey).collect(toList());
        val permissions = addApplicationPermissions(permissionsSpec, applications, tenantKey);
        String permissionsYml = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(permissions);
        Configuration configuration = new Configuration(permissionsSpecPath, permissionsYml);
        commonConfigRepository.updateConfigFullPath(configuration, sha1Hex(permissionsSpec));
    }

    @SneakyThrows
    private void updateApplicationPrivileges(Map<String, TypeSpec> specs,
                                             String privilegesPath,
                                             Configuration customPrivileges) {

        List<Map<String, Object>> applicationPrivileges = specs.values().stream()
            .filter(it -> TRUE.equals(it.getIsApp()))
            .map(this::toPrivilege).collect(toList());
        val privileges = addApplicationPrivileges(customPrivileges, applicationPrivileges);
        String content = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(privileges);
        commonConfigRepository.updateConfigFullPath(new Configuration(privilegesPath, content), sha1Hex(customPrivileges));
    }

    private String sha1Hex(Configuration configuration) {
        return ofNullable(configuration).map(Configuration::getContent).map(DigestUtils::sha1Hex).orElse(null);
    }

    @SneakyThrows
    private Map<String, List<Map<String, Object>>> addApplicationPrivileges(Configuration customPrivileges,
                                                                            List<Map<String, Object>> applicationPrivileges) {

        Map<String, List<Map<String, Object>>>  privileges = new HashMap<>();
        if (isConfigExists(customPrivileges)) {
            privileges = mapper.readValue(customPrivileges.getContent(), new TypeReference<Map<String, List<Object>>>() {
            });
        }
        privileges.put(SECTION_NAME, applicationPrivileges);
        return privileges;
    }

    private Map<String, Object> toPrivilege(TypeSpec spec) {
        return of("key", APPLICATION_PRIVILEGE_PREFIX + spec.getKey(), "description", "{}");
    }

    private boolean isConfigExists(Configuration configuration) {
        return ofNullable(configuration)
            .map(Configuration::getContent)
            .map(StringUtils::isNotBlank)
            .orElse(false);
    }

    @SneakyThrows
    private Map<String, ?> addApplicationPermissions(Configuration permission, List<String> applications, String tenantKey) {
        TreeMap<String, TreeMap<String, TreeSet<Permission>>> permissions = new TreeMap<>();
        if (isConfigExists(permission)) {
            permissions = mapper.readValue(permission.getContent(),
                new TypeReference<TreeMap<String, TreeMap<String, TreeSet<Permission>>> >() {});
        }

        permissions.computeIfAbsent(SECTION_NAME, key -> new TreeMap<>());
        val applicationPermission = permissions.getOrDefault(SECTION_NAME, new TreeMap<>());

        Map<String, Role> roles = roleService.getRoles(tenantKey);
        roles = roles != null ? roles : new HashMap<>();
        for (String role: roles.keySet()) {
            applicationPermission.computeIfAbsent(role, key -> new TreeSet<>());
            TreeSet<Permission> rolePermissions = applicationPermission.get(role);

            applications.stream().map(this::toPermission)
                .filter(not(rolePermissions::contains))
                .forEach(rolePermissions::add);
        }

        return permissions;
    }

    private Predicate<Permission> not(Predicate<Permission> o) {
        return o.negate();
    }

    private Permission toPermission(String applicationKey) {
        Permission permission = new Permission();
        permission.setPrivilegeKey(APPLICATION_PRIVILEGE_PREFIX + applicationKey);
        permission.setDisabled(false);
        return permission;
    }

}
