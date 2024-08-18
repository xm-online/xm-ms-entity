package com.icthh.xm.ms.entity.service.swagger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.ms.entity.service.swagger.model.ServerObject;
import com.icthh.xm.ms.entity.service.swagger.model.SwaggerInfo;
import com.icthh.xm.ms.entity.service.swagger.model.TagObject;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.apache.commons.lang3.StringUtils.isBlank;

@Slf4j
@Component
public class DynamicSwaggerRefreshableConfiguration implements RefreshableConfiguration {

    private final ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
    private final AntPathMatcher matcher = new AntPathMatcher();
    private final String specPath;
    private final Map<String, DynamicSwaggerConfiguration> config = new ConcurrentHashMap<>();
    private final TenantContextHolder tenantContextHolder;

    public DynamicSwaggerRefreshableConfiguration(@Value("${spring.application.name}") String appName,
                                                  TenantContextHolder tenantContextHolder) {
        this.specPath = "/config/tenants/{tenantName}/" + appName + "/swagger.yml";
        this.tenantContextHolder = tenantContextHolder;
    }

    public void onRefresh(final String updatedKey, final String config) {
        try {
            String tenant = this.matcher.extractUriTemplateVariables(specPath, updatedKey).get("tenantName");
            if (isBlank(config)) {
                this.config.remove(tenant);
                return;
            }

            this.config.put(tenant, objectMapper.readValue(config, DynamicSwaggerConfiguration.class));
            log.info("Configuration was updated for tenant [{}] by key [{}]", tenant, updatedKey);
        } catch (Exception e) {
            log.error("Error read configuration from path {}", updatedKey, e);
        }
    }

    public boolean isListeningConfiguration(final String updatedKey) {
        return this.matcher.match(specPath, updatedKey);
    }

    public DynamicSwaggerConfiguration getConfiguration() {
        return this.config.get(tenantContextHolder.getTenantKey());
    }

    @Data
    public static class DynamicSwaggerConfiguration {

        private SwaggerInfo info;
        private List<ServerObject> servers;
        private List<TagObject> tags;

    }
}
