package com.icthh.xm.ms.entity.service.privileges.custom;

import static com.icthh.xm.commons.config.client.repository.TenantConfigRepository.TENANT_NAME;
import static com.icthh.xm.ms.entity.service.privileges.custom.CustomPrivilegesExtractor.DefaultPrivilegesValue.ENABLED;
import static com.icthh.xm.ms.entity.service.privileges.custom.CustomPrivilegesExtractor.DefaultPrivilegesValue.NONE;
import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.config.client.repository.CommonConfigRepository;
import com.icthh.xm.commons.config.domain.Configuration;
import com.icthh.xm.commons.logging.aop.IgnoreLogginAspect;
import com.icthh.xm.commons.permission.config.PermissionProperties;
import com.icthh.xm.commons.permission.domain.Permission;
import com.icthh.xm.commons.permission.domain.Role;
import com.icthh.xm.commons.permission.service.RoleService;
import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import com.icthh.xm.ms.entity.service.privileges.custom.CustomPrivilegesExtractor.DefaultPrivilegesValue;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EntityCustomPrivilegeService {

    private static final String CUSTOMER_PRIVILEGES_PATH = "/config/tenants/{" + TENANT_NAME + "}/custom-privileges.yml";

    private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    private final CommonConfigRepository commonConfigRepository;
    private final PermissionProperties permissionProperties;
    private final RoleService roleService;
    private final List<CustomPrivilegesExtractor> privilegesExtractors;

    @IgnoreLogginAspect
    public void updateCustomPermission(Map<String, TypeSpec> specs, String tenantKey) {

        String privilegesPath = resolvePathWithTenant(tenantKey, CUSTOMER_PRIVILEGES_PATH);
        String specPath = permissionProperties.getPermissionsSpecPath();
        String permissionsSpecPath = resolvePathWithTenant(tenantKey, specPath);

        log.info("Get config from {} and {}", privilegesPath, permissionsSpecPath);
        List<String> paths = asList(privilegesPath, permissionsSpecPath);
        Map<String, Configuration> configs = commonConfigRepository.getConfig(null, paths);
        configs = configs != null ? configs : new HashMap<>();

        updateCustomPrivileges(specs, privilegesPath, configs.get(privilegesPath));
        setNewPermissionsDefaultValue(specs, tenantKey, permissionsSpecPath, configs.get(permissionsSpecPath));
    }

    private String resolvePathWithTenant(String tenantKey, String specPath) {
        return specPath.replace("{" + TENANT_NAME + "}", tenantKey);
    }

    @SneakyThrows
    private void updateCustomPrivileges(Map<String, TypeSpec> specs,
                                        String privilegesPath,
                                        Configuration customPrivileges) {

        Map<String, List<Map<String, Object>>> privileges = getPrivilegesConfig(customPrivileges);

        addCustomPrivileges(specs, privileges);

        String content = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(privileges);
        if (DigestUtils.sha1Hex(content).equals(sha1Hex(customPrivileges))) {
            log.info("Privileges configuration not changed");
            return;
        }
        commonConfigRepository.updateConfigFullPath(new Configuration(privilegesPath, content), sha1Hex(customPrivileges));
    }

    private void addCustomPrivileges(Map<String, TypeSpec> specs,
                                     Map<String, List<Map<String, Object>>> privileges) {
        for(CustomPrivilegesExtractor privilegesExtractor: privilegesExtractors) {
            privileges.put(privilegesExtractor.getSectionName(), privilegesExtractor.toPrivileges(specs));
        }
    }

    private Map<String, List<Map<String, Object>>> getPrivilegesConfig(
        Configuration customPrivileges) throws java.io.IOException {
        Map<String, List<Map<String, Object>>> privileges = new HashMap<>();
        if (isConfigExists(customPrivileges)) {
            privileges = mapper.readValue(customPrivileges.getContent(), new TypeReference<Map<String, List<Object>>>() {
            });
        }
        return privileges;
    }

    private String sha1Hex(Configuration configuration) {
        return ofNullable(configuration).map(Configuration::getContent).map(DigestUtils::sha1Hex).orElse(null);
    }

    private boolean isConfigExists(Configuration configuration) {
        return ofNullable(configuration)
            .map(Configuration::getContent)
            .map(StringUtils::isNotBlank)
            .orElse(false);
    }

    @SneakyThrows
    private void setNewPermissionsDefaultValue(Map<String, TypeSpec> specs,
                                               String tenantKey,
                                               String permissionsSpecPath,
                                               Configuration permissionsSpec) {

        val permissions = updatePermissions(permissionsSpec, specs, tenantKey);
        String permissionsYml = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(permissions);
        Configuration configuration = new Configuration(permissionsSpecPath, permissionsYml);
        if (DigestUtils.sha1Hex(permissionsYml).equals(sha1Hex(permissionsSpec))) {
            log.info("Permissions configuration not changed");
            return;
        }
        commonConfigRepository.updateConfigFullPath(configuration, sha1Hex(permissionsSpec));
    }

    private Map<String, ?> updatePermissions(Configuration permissionConfiguration,
                                             Map<String, TypeSpec> spec,
                                             String tenantKey) {
        val permissions = getPermissionsConfig(permissionConfiguration);

        for(CustomPrivilegesExtractor privilegesExtractor: privilegesExtractors) {
            DefaultPrivilegesValue defaultValue = privilegesExtractor.getDefaultValue();
            if (defaultValue == NONE) {
                continue;
            }

            String sectionName = privilegesExtractor.getSectionName();
            permissions.computeIfAbsent(sectionName, key -> new TreeMap<>());
            val permission = permissions.getOrDefault(sectionName, new TreeMap<>());

            Map<String, Role> roles = roleService.getRoles(tenantKey);
            roles = roles != null ? roles : new HashMap<>();
            for (String role : roles.keySet()) {
                permission.computeIfAbsent(role, key -> new TreeSet<>());
                TreeSet<Permission> rolePermissions = permission.get(role);
                String privilegePrefix = privilegesExtractor.getPrivilegePrefix();

                privilegesExtractor.toPrivilegesList(spec)
                                   .stream()
                                   .map(toPermission(privilegePrefix, defaultValue == ENABLED))
                                   .filter(not(rolePermissions::contains))
                                   .forEach(rolePermissions::add);
            }
        }

        return permissions;
    }

    @SneakyThrows
    private TreeMap<String, TreeMap<String, TreeSet<Permission>>> getPermissionsConfig(
        Configuration permission) {
        TreeMap<String, TreeMap<String, TreeSet<Permission>>> permissions = new TreeMap<>();
        if (isConfigExists(permission)) {
            permissions = mapper.readValue(permission.getContent(),
                new TypeReference<TreeMap<String, TreeMap<String, TreeSet<Permission>>>>() {});
        }
        return permissions;
    }

    private Predicate<Permission> not(Predicate<Permission> o) {
        return o.negate();
    }

    private Function<String, Permission> toPermission(String privilegePrifex, boolean isEnabled) {
        return key -> {
            Permission permission = new Permission();
            permission.setPrivilegeKey(privilegePrifex + key);
            permission.setDisabled(!isEnabled);
            return permission;
        };
    }
}
