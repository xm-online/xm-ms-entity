package com.icthh.xm.ms.entity.service;

import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.ms.entity.config.ApplicationProperties;
import com.icthh.xm.ms.entity.domain.mapper.TemplateMapper;
import com.icthh.xm.ms.entity.domain.template.Template;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class XmEntityTemplatesSpecService implements RefreshableConfiguration {

    private static final String TENANT_NAME = "tenantName";
    private final AntPathMatcher matcher = new AntPathMatcher();
    private ConcurrentHashMap<String, Map<String, Template>> templates = new ConcurrentHashMap<>();
    private final ApplicationProperties applicationProperties;

    public Map<String, Template> getTemplates(String tenant) {
        if (!templates.containsKey(tenant)) {
            return new HashMap<>();
        }
        return templates.get(tenant);
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
                templates.put(tenant, TemplateMapper.ymlToTemplates(config));
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
}
