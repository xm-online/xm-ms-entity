package com.icthh.xm.ms.entity.service.privileges.custom;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.icthh.xm.commons.config.client.repository.CommonConfigRepository;
import com.icthh.xm.commons.config.domain.Configuration;
import com.icthh.xm.commons.logging.aop.IgnoreLogginAspect;
import com.icthh.xm.ms.entity.config.XmEntityTenantConfigService;
import com.icthh.xm.ms.entity.config.XmEntityTenantConfigService.XmEntityTenantConfig;
import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import com.icthh.xm.ms.entity.service.XmEntitySpecService.EntitySpecUpdateListener;
import java.util.ArrayList;
import java.util.Comparator;
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

import static com.icthh.xm.commons.config.client.repository.TenantConfigRepository.PATH_CONFIG_TENANT;
import static java.util.Arrays.asList;
import static java.util.Comparator.comparing;
import static java.util.Optional.ofNullable;

@Slf4j
@Service
@RequiredArgsConstructor
public class EntityCustomPrivilegeService implements EntitySpecUpdateListener {

    private static final String TENANT_NAME = "tenantName";
    private static final String CUSTOMER_PRIVILEGES_PATH = PATH_CONFIG_TENANT + "custom-privileges.yml";

    private final YAMLFactory yamlFactory = new YAMLFactory()
        .enable(YAMLGenerator.Feature.USE_PLATFORM_LINE_BREAKS);

    private final ObjectMapper mapper = new ObjectMapper(yamlFactory);

    private final CommonConfigRepository commonConfigRepository;

    private final List<CustomPrivilegesExtractor> privilegesExtractors;

    private final XmEntityTenantConfigService tenantConfigService;

    @IgnoreLogginAspect
    @Override
    public void onEntitySpecUpdate(Map<String, TypeSpec> specs, String tenantKey) {
        XmEntityTenantConfig xmEntityTenantConfig = tenantConfigService.getXmEntityTenantConfig(tenantKey);
        Boolean disableDynamicPrivilegesGeneration = xmEntityTenantConfig.getDisableDynamicPrivilegesGeneration();
        if (Boolean.TRUE.equals(disableDynamicPrivilegesGeneration)) {
            log.warn("Dynamic privilege generation disabled.");
            return;
        }

        String privilegesPath = resolvePathWithTenant(tenantKey, CUSTOMER_PRIVILEGES_PATH);

        log.info("Get config from {}", privilegesPath);
        List<String> paths = asList(privilegesPath);
        Map<String, Configuration> configs = commonConfigRepository.getConfig(null, paths);
        configs = configs == null ? new HashMap<>() : configs;

        updateCustomPrivileges(specs, privilegesPath, configs.get(privilegesPath), tenantKey);
    }

    private String resolvePathWithTenant(String tenantKey, String specPath) {
        return specPath.replace("{" + TENANT_NAME + "}", tenantKey);
    }

    @SneakyThrows
    private void updateCustomPrivileges(Map<String, TypeSpec> specs, String privilegesPath,
                                        Configuration customPrivileges, String tenantKey) {

        val privileges = getPrivilegesConfig(customPrivileges);

        addCustomPrivileges(specs, privileges, tenantKey);

        String content = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(privileges);
        if (DigestUtils.sha1Hex(content).equals(sha1Hex(customPrivileges))) {
            log.info("Privileges configuration not changed");
            return;
        }
        commonConfigRepository.updateConfigFullPath(new Configuration(privilegesPath, content), sha1Hex(customPrivileges));
    }

    private void addCustomPrivileges(Map<String, TypeSpec> specs,
                                     Map<String, List<Map<String, Object>>> privileges,
                                     String tenantKey) {
        Map<String, List<Map<String, Object>>> customPrivileges = new HashMap<>();
        for (CustomPrivilegesExtractor privilegesExtractor : privilegesExtractors) {
            if (!privilegesExtractor.isEnabled(tenantKey)) {
                continue;
            }

            List<Map<String, Object>> value = privilegesExtractor.toPrivileges(specs);
            if (customPrivileges.containsKey(privilegesExtractor.getSectionName())) {
                customPrivileges.get(privilegesExtractor.getSectionName()).addAll(value);
            } else {
                customPrivileges.put(privilegesExtractor.getSectionName(), new ArrayList<>(value));
            }

            value.sort(comparing(it -> it.get("key").toString()));
        }
        privileges.putAll(customPrivileges);
    }

    private Map<String, List<Map<String, Object>>> getPrivilegesConfig(
        Configuration customPrivileges) throws java.io.IOException {
        Map<String, List<Map<String, Object>>> privileges = new HashMap<>();
        if (isConfigExists(customPrivileges)) {
            privileges = mapper.readValue(customPrivileges.getContent(),
                new TypeReference<Map<String, List<Map<String, Object>>>>() {
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

}
