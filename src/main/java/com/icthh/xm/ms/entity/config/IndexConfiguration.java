package com.icthh.xm.ms.entity.config;

import static com.icthh.xm.commons.tenant.TenantContextUtils.getRequiredTenantKeyValue;
import static java.util.Collections.emptyMap;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

@Slf4j
@Service
public class IndexConfiguration implements RefreshableConfiguration {

    private final ConcurrentHashMap<String, String> configuration = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Map<String, String>> indexMapConfiguration = new ConcurrentHashMap<>();
    private final AntPathMatcher matcher = new AntPathMatcher();
    private final TenantContextHolder tenantContextHolder;
    private final String mappingPath;
    private final String mappingPathPerIndex;



    public IndexConfiguration(TenantContextHolder tenantContextHolder,
                             @Value("${spring.application.name}") String appName) {

        this.tenantContextHolder = tenantContextHolder;
        this.mappingPath = "/config/tenants/{tenantName}/" + appName + "/index_config.json";
        this.mappingPathPerIndex = "/config/tenants/{tenantName}/" + appName + "/mappings/index_config_{index}.json";
    }

    public void onRefresh(final String updatedKey, final String config) {
        try {
            if (this.matcher.match(mappingPath, updatedKey)) {
                updateGlobalIndex(updatedKey, config);
            } else if (this.matcher.match(mappingPathPerIndex, updatedKey)) {
                updateIndex(updatedKey, config);
            }
        } catch (Exception ex) {
            log.error("Error read tenant configuration from path " + updatedKey, ex);
        }
    }

    private void updateGlobalIndex(String updatedKey, String config) {
        String tenant = extractPathParam(updatedKey, mappingPath, "tenantName");
        if (isBlank(config)) {
            this.configuration.remove(tenant);
            return;
        }

        this.configuration.put(tenant, config);
        log.info("Index configuration was updated for tenant [{}] by key [{}]", tenant, updatedKey);
    }

    private void updateIndex(String updatedKey, String config) {
        String tenant = extractPathParam(updatedKey, mappingPathPerIndex, "tenantName");
        String index = extractPathParam(updatedKey, mappingPathPerIndex, "index");
        if (isBlank(config)) {
            this.indexMapConfiguration.getOrDefault(tenant, emptyMap()).remove(index);
            return;
        }
        this.indexMapConfiguration
            .compute(tenant, (key, val) -> val == null ? new ConcurrentHashMap() : val)
            .put(index, config);
        log.info("Tenant configuration was updated for tenant [{}] for index [{}] by key [{}]", tenant, index, updatedKey);
    }

    public boolean isListeningConfiguration(final String updatedKey) {
        return this.matcher.match(mappingPath, updatedKey) || this.matcher.match(mappingPathPerIndex, updatedKey);
    }

    public void onInit(final String configKey, final String configValue) {
        if (this.isListeningConfiguration(configKey)) {
            this.onRefresh(configKey, configValue);
        }
    }

    public boolean isConfigExists() {
        return configuration.containsKey(getRequiredTenantKeyValue(this.tenantContextHolder));
    }

    public String getConfiguration() {
        return configuration.get(getRequiredTenantKeyValue(this.tenantContextHolder));
    }

    private String extractPathParam(String updatedKey, String mapping, String paramToExtract) {
        return this.matcher.extractUriTemplateVariables(mapping, updatedKey).get(paramToExtract);
    }

    public String getMapping(String index) {
        return indexMapConfiguration
            .getOrDefault(getRequiredTenantKeyValue(this.tenantContextHolder), emptyMap())
            .get(index);
    }
}
