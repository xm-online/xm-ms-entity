package com.icthh.xm.ms.entity.service;

import static com.google.common.collect.ImmutableMap.of;
import static com.icthh.xm.commons.config.client.repository.TenantConfigRepository.TENANT_NAME;
import static com.icthh.xm.commons.config.client.repository.TenantConfigRepository.URL;
import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.empty;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
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
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

@Slf4j
@Service
@RequiredArgsConstructor
public class EntityCustomPrivilegeService {

    private static final String CUSTOMER_PRIVILEGES_PATH = URL + "custom-privileges.yml";
    public static final String SECTION_NAME = "applications";

    private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    private final CommonConfigRepository commonConfigRepository;
    private final PermissionProperties permissionProperties;
    private final RoleService roleService;


    @SneakyThrows
    public void updateApplicationPermission(Map<String, TypeSpec> specs, String tenantKey) {
        List<Map<String, Object>> applicationPrivileges = specs.values().stream().filter(TypeSpec::getIsApp).map(this::toPrivilege).collect(toList());

        String privilegesPath = CUSTOMER_PRIVILEGES_PATH.replace("{" + TENANT_NAME + "}", tenantKey);
        String permissionsSpecPath = permissionProperties.getPermissionsSpecPath().replace("{" + TENANT_NAME + "}", tenantKey);

        log.info("Get config from {} and {}", privilegesPath, permissionsSpecPath);
        Map<String, Configuration> configs = commonConfigRepository.getConfig(null, asList(privilegesPath, permissionsSpecPath));

        Configuration customPrivileges = configs.get(privilegesPath);
        val privileges = addApplicationPrivileges(customPrivileges, applicationPrivileges);
        String content = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(privileges);
        commonConfigRepository.updateConfigFullPath(new Configuration(privilegesPath, content), sha1Hex(customPrivileges));

        Configuration permissionsSpec = configs.get(permissionsSpecPath);



    }

    private String sha1Hex(Configuration configuration) {
        return ofNullable(configuration).map(Configuration::getContent).map(DigestUtils::sha1Hex).orElse(null);
    }

    @SneakyThrows
    private Map<String, List<Map<String, Object>>> addApplicationPrivileges(Configuration customPrivileges, List<Map<String, Object>> applicationPrivileges) {
        if (isConfigExists(customPrivileges)) {
            Map<String, List<Map<String, Object>>>  privileges = mapper.readValue(customPrivileges.getContent(), new TypeReference<Map<String, List<Object>>>() {});
            privileges.put(SECTION_NAME, applicationPrivileges);
            return privileges;
        } else {
            return new HashMap<>();
        }
    }

    private boolean isConfigExists(Configuration configuration) {
        return ofNullable(configuration).map(Configuration::getContent).map(StringUtils::isNotBlank).orElse(false);
    }

    private Map<String, Object> toPrivilege(TypeSpec spec) {
        return of("key", "APPLICATION." + spec.getKey(), "description", "{}");
    }

    @SneakyThrows
    private Map<String, ?> addApplicationPermissions(Configuration permission, List<String> applications, String tenantKey) {
        if (isConfigExists(permission)) {
            TypeFactory typeFactory = mapper.getTypeFactory();
            JavaType permissionType = typeFactory.constructCollectionType(TreeSet.class, Permission.class);
            JavaType stringClass  = typeFactory.constructType(String.class);
            JavaType sectionType = typeFactory.constructMapType(TreeMap.class, stringClass, permissionType);
            JavaType type = typeFactory.constructMapType(TreeMap.class, stringClass, sectionType);
            TreeMap<String, TreeMap<String, TreeSet<Permission>>> permissions = mapper.readValue(permission.getContent(), type);

            TreeMap<String, TreeSet<Permission>> applicationPermission = permissions.getOrDefault(SECTION_NAME, new TreeMap<>());

            for (String role: roleService.getRoles(tenantKey).keySet()) {
                TreeSet<Permission> rolePermissions = applicationPermission.get(role);

                applications.stream().filter(it -> !rolePermissions.contains(it)).forEach(it ->
                    rolePermissions.add();
                );
            }

            //TODO add test

            return permissions;
        } else {
            return new HashMap<>();
        }
    }




}
