package com.icthh.xm.ms.entity.service;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.ms.entity.config.ApplicationProperties;
import com.icthh.xm.ms.entity.domain.template.Template;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class XmEntityTemplatesSpecService implements RefreshableConfiguration {

    private static final String TENANT_NAME = "tenantName";
    private static final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    private final AntPathMatcher matcher = new AntPathMatcher();
    private final ConcurrentHashMap<String, Map<String, Template>> templates = new ConcurrentHashMap<>();
    private final ApplicationProperties applicationProperties;
    private final TenantContextHolder tenantContextHolder;

    public String findTemplate(String key) {
        Template template = getTemplates().get(key);
        if (template != null) {
            String query = template.getQuery();
            if (isNotBlank(query)) {
                return query;
            }
        }
        throw new IllegalArgumentException("Template query not found: " + key);
    }

    @Override
    @SneakyThrows
    public void onRefresh(String key, String config) {
        try {
            String tenant = matcher.extractUriTemplateVariables(applicationProperties
                .getSpecificationTemplatesPathPattern(), key).get(TENANT_NAME);
            if (StringUtils.isBlank(config)) {
                templates.remove(tenant);
                log.info("Template specification for tenant {} was removed", tenant);
            } else {
                templates.put(tenant, ymlToTemplates(config));
                log.info("Template specification for tenant {} was updated", tenant);
            }
        } catch (Exception e) {
            log.error("Error read template specification from path " + key, e);
        }
    }

    @Override
    public boolean isListeningConfiguration(String updatedKey) {
        String specificationPathPattern = applicationProperties.getSpecificationTemplatesPathPattern();
        return matcher.match(specificationPathPattern, updatedKey);
    }

    @Override
    public void onInit(String key, String config) {
        if (isListeningConfiguration(key)) {
            onRefresh(key, config);
        }
    }

    private Map<String, Template> getTemplates() {
        String tenantKeyValue = getTenantKeyValue();
        if (!templates.containsKey(tenantKeyValue)) {
            throw new IllegalArgumentException("Tenant configuration not found");
        }
        return templates.get(tenantKeyValue);
    }

    private String getTenantKeyValue() {
        return TenantContextUtils.getRequiredTenantKeyValue(tenantContextHolder);
    }

    private static Map<String, Template> ymlToTemplates(String yml) {
        try {
            Map<String, Template> map = mapper
                .readValue(yml, new TypeReference<TreeMap<String, Template>>() {
                });
            map.forEach((templateKey, template) -> template.setKey(templateKey));
            return map;
        } catch (Exception e) {
            log.error("Failed to create template collection from YML file, error: {}", e.getMessage(), e);
        }
        return Collections.emptyMap();
    }
}
