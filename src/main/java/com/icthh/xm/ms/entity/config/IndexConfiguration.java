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
public class IndexConfiguration implements RefreshableConfiguration {

    private final ConcurrentHashMap<String, String> configuration = new ConcurrentHashMap<>();
    private final AntPathMatcher matcher = new AntPathMatcher();
    private final TenantContextHolder tenantContextHolder;
    private final String mappingPath;

    public IndexConfiguration(TenantContextHolder tenantContextHolder,
                             @Value("${spring.application.name}") String appName) {

        this.tenantContextHolder = tenantContextHolder;
        this.mappingPath = "/config/tenants/{tenantName}/" + appName + "/index_config.json";
    }

    public void onRefresh(final String updatedKey, final String config) {
        try {
            String tenant = this.matcher.extractUriTemplateVariables(mappingPath, updatedKey).get("tenantName");
            if (isBlank(config)) {
                this.configuration.remove(tenant);
                return;
            }

            this.configuration.put(tenant, config);
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

    public boolean isConfigExists() {
        return configuration.containsKey(getRequiredTenantKeyValue(this.tenantContextHolder));
    }

    public String getConfiguration() {
        return configuration.get(getRequiredTenantKeyValue(this.tenantContextHolder));
    }
}
