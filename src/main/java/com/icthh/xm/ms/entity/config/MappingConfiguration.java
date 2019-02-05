package com.icthh.xm.ms.entity.config;

import static com.icthh.xm.commons.tenant.TenantContextUtils.getRequiredTenantKeyValue;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.tenant.TenantContextHolder;
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
    private final AntPathMatcher matcher = new AntPathMatcher();

    private final TenantContextHolder tenantContextHolder;
    private final String mappingPath;

    public MappingConfiguration(TenantContextHolder tenantContextHolder,
                                @Value("${spring.application.name}") String appName) {
        this.tenantContextHolder = tenantContextHolder;
        this.mappingPath = "/config/tenants/{tenantName}/" + appName + "/mapping.json";
    }

    public void onRefresh(final String updatedKey, final String config) {
        try {
            String tenant = this.matcher.extractUriTemplateVariables(mappingPath, updatedKey).get("tenantName");
            if (isBlank(config)) {
                this.mapping.remove(tenant);
                return;
            }

            this.mapping.put(tenant, config);
            log.info("Tenant configuration was updated for tenant [{}] by key [{}]", tenant, updatedKey);
        } catch (Exception var6) {
            log.error("Error read tenant configuration from path " + updatedKey, var6);
        }
    }

    public boolean isListeningConfiguration(final String updatedKey) {
        return this.matcher.match(mappingPath, updatedKey);
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
}
