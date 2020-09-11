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
public class MappingConfiguration implements RefreshableConfiguration {

    private static final String TENANT_NAME = "tenantName";
    private final ConcurrentHashMap<String, String> mapping = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Map<String, String>> mappingsMap = new ConcurrentHashMap<>();
    private final AntPathMatcher matcher = new AntPathMatcher();

    private final TenantContextHolder tenantContextHolder;
    private final String mappingPath;
    private final String mappingPathPerIndex;

    public MappingConfiguration(TenantContextHolder tenantContextHolder,
                                @Value("${spring.application.name}") String appName) {
        this.tenantContextHolder = tenantContextHolder;
        this.mappingPath = "/config/tenants/{tenantName}/" + appName + "/mapping.json";
        this.mappingPathPerIndex = "/config/tenants/{tenantName}/" + appName + "/mappings/mapping_{index}.json";
    }

    public void onRefresh(final String updatedKey, final String config) {
        try {
            if (this.matcher.match(mappingPath, updatedKey)) {
                updateGlobalMapping(updatedKey, config);
            } else if (this.matcher.match(mappingPathPerIndex, updatedKey)) {
                updateMappingPerIndex(updatedKey, config);
            }
        } catch (Exception ex) {
            log.error("Error read tenant configuration from path " + updatedKey, ex);
        }
    }

    private void updateGlobalMapping(String updatedKey, String config) {
        String tenant = extractPathParam(updatedKey, mappingPath, "tenantName");
        if (isBlank(config)) {
            this.mapping.remove(tenant);
            return;
        }

        this.mapping.put(tenant, config);
        log.info("Tenant configuration was updated for tenant [{}] by key [{}]", tenant, updatedKey);
    }

    private void updateMappingPerIndex(String updatedKey, String config) {
        String tenant = extractPathParam(updatedKey, mappingPathPerIndex, "tenantName");
        String index = extractPathParam(updatedKey, mappingPathPerIndex, "index");
        if (isBlank(config)) {
            this.mappingsMap.getOrDefault(tenant, emptyMap()).remove(index);
            return;
        }
        this.mappingsMap
            .compute(tenant, (key, val) -> val == null ? new ConcurrentHashMap() : val)
            .put(index, config);
        log.info("Tenant configuration was updated for tenant [{}] for index [{}] by key [{}]", tenant, index, updatedKey);
    }

    private String extractPathParam(String updatedKey, String mapping, String paramToExtract) {
        return this.matcher.extractUriTemplateVariables(mapping, updatedKey).get(paramToExtract);
    }

    public boolean isListeningConfiguration(final String updatedKey) {
        return this.matcher.match(mappingPath, updatedKey) || this.matcher.match(mappingPathPerIndex, updatedKey);
    }

    public void onInit(final String configKey, final String configValue) {
        if (this.isListeningConfiguration(configKey)) {
            this.onRefresh(configKey, configValue);
        }
    }

    public boolean isMappingExists() {
        return mapping.containsKey(getRequiredTenantKeyValue(this.tenantContextHolder));
    }

    public String getMapping() {
        return mapping.get(getRequiredTenantKeyValue(this.tenantContextHolder));
    }

    public String getMapping(String index) {
        return mappingsMap
            .getOrDefault(getRequiredTenantKeyValue(this.tenantContextHolder), emptyMap())
            .get(index);
    }
}
