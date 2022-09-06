package com.icthh.xm.ms.entity.service;

import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Service
@Slf4j
public class JsonListenerService implements RefreshableConfiguration {
    private static final String TENANT_NAME = "tenantName";
    private final Map<String, Map<String, String>> tenantsSpecificationsByPath;
    private final String mappingPath;
    private final AntPathMatcher matcher;

    public JsonListenerService(@Value("${spring.application.name}") String appName) {
        this.tenantsSpecificationsByPath = new LinkedHashMap<>();
        this.matcher = new AntPathMatcher();
        this.mappingPath = "/config/tenants/{tenantName}/" + appName + "/xmentityspec/**/*.json";
    }

    @Override
    public void onRefresh(String updatedKey, String config) {
        String tenantName = matcher.extractUriTemplateVariables(mappingPath, updatedKey).get(TENANT_NAME);
        String relativePath = matcher.extractPathWithinPattern(mappingPath, updatedKey);

        if (isBlank(config)) {
            tenantsSpecificationsByPath.remove(tenantName);
            return;
        }

        tenantsSpecificationsByPath.putIfAbsent(tenantName, new ConcurrentHashMap<>());
        tenantsSpecificationsByPath.get(tenantName).put(format("%s/%s","xmentityspec",relativePath), config);
    }

    @Override
    public boolean isListeningConfiguration(String updatedKey) {
        return matcher.match(mappingPath, updatedKey);
    }

    public String getSpecificationByTenantRelativePath(String tenant, String relativePath) {
        return ofNullable(getSpecificationByTenant(tenant))
            .map(xm->xm.get(relativePath))
            .orElse(EMPTY);
    }

    protected Map<String,String> getSpecificationByTenant(String tenant){
        return tenantsSpecificationsByPath.get(tenant);
    }
}
