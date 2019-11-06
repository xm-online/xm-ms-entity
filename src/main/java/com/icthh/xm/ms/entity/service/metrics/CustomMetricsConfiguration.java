package com.icthh.xm.ms.entity.service.metrics;

import static com.icthh.xm.commons.tenant.TenantContextUtils.getRequiredTenantKeyValue;
import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

@Slf4j
@Component
public class CustomMetricsConfiguration implements RefreshableConfiguration {

    private static final String METRICS_SUFFIX = ".metrics";

    private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    private final ConcurrentHashMap<String, List<CustomMetric>> configuration = new ConcurrentHashMap<>();
    private final AntPathMatcher matcher = new AntPathMatcher();

    private final MetricRegistry metricRegistry;
    private final CustomMetricsService customMetricsService;
    private final TenantContextHolder tenantContextHolder;
    private final PeriodMetricsService periodMetricsService;
    private final String mappingPath;

    public CustomMetricsConfiguration(TenantContextHolder tenantContextHolder, MetricRegistry metricRegistry,
                                      CustomMetricsService customMetricsService,
                                      PeriodMetricsService periodMetricsService,
                                      @Value("${spring.application.name}") String appName) {

        this.tenantContextHolder = tenantContextHolder;
        this.metricRegistry = metricRegistry;
        this.customMetricsService = customMetricsService;
        this.periodMetricsService = periodMetricsService;
        this.mappingPath = "/config/tenants/{tenantName}/" + appName + "/metrics.yml";
    }

    public void onRefresh(final String updatedKey, final String config) {
        try {
            String tenant = this.matcher.extractUriTemplateVariables(mappingPath, updatedKey).get("tenantName").toLowerCase();
            if (isBlank(config)) {
                this.configuration.remove(tenant);
                periodMetricsService.scheduleCustomMetric(emptyList(), tenant);
                return;
            }

            List<CustomMetric> metrics = mapper.readValue(config, new TypeReference<List<CustomMetric>>() {});
            this.configuration.put(tenant, metrics);
            log.info("Metric configuration was updated for tenant [{}] by key [{}]", tenant, updatedKey);
            String metricsName = tenant + METRICS_SUFFIX;
            metricRegistry.remove(metricsName);
            metricRegistry.register(metricsName, new CustomMetricsSet(this, customMetricsService, tenant));
            periodMetricsService.scheduleCustomMetric(metrics, tenant);
        } catch (Exception e) {
            log.error("Error read metric configuration from path " + updatedKey, e);
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

    public List<CustomMetric> getMetrics() {
        return configuration.getOrDefault(getTenantKeyValue(), emptyList());
    }

    private String getTenantKeyValue() {
        return tenantContextHolder.getTenantKey();
    }

    @Data
    public static class CustomMetric {
        private String name;
        private Integer updatePeriodSeconds;
    }

}
