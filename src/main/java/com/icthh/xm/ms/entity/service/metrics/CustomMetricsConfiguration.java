package com.icthh.xm.ms.entity.service.metrics;

import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.icthh.xm.commons.tenant.YamlMapperUtils;
import tools.jackson.core.type.TypeReference;
import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
public class CustomMetricsConfiguration implements RefreshableConfiguration {

    private static final String METRICS_PREFIX = "custom.metrics";
    private static final String TENANT_NAME = "tenantName";

    private final ObjectMapper mapper = YamlMapperUtils.yamlDefaultMapper();
    private final ConcurrentHashMap<String, List<CustomMetric>> configuration = new ConcurrentHashMap<>();
    private final AntPathMatcher matcher = new AntPathMatcher();

    private final MeterRegistry meterRegistry;
    private final CustomMetricsService customMetricsService;
    private final PeriodicMetricsService periodMetricsService;
    private final String mappingPath;

    public CustomMetricsConfiguration(MeterRegistry meterRegistry,
                                      CustomMetricsService customMetricsService,
                                      PeriodicMetricsService periodMetricsService,
                                      @Value("${spring.application.name}") String appName) {

        this.meterRegistry = meterRegistry;
        this.customMetricsService = customMetricsService;
        this.periodMetricsService = periodMetricsService;
        this.mappingPath = "/config/tenants/{tenantName}/" + appName + "/metrics.yml";
    }

    public void onRefresh(final String updatedKey, final String config) {
        try {
            String tenant = this.matcher.extractUriTemplateVariables(mappingPath, updatedKey).get(TENANT_NAME);
            if (isBlank(config)) {
                this.configuration.remove(tenant);
                cleanupTenant(tenant);
                return;
            }

            List<CustomMetric> metrics = mapper.readValue(config, new TypeReference<>() {});
            this.configuration.put(tenant, metrics);
            log.info("Metric configuration updated for tenant [{}]: {} metrics", tenant, metrics.size());
            cleanupTenant(tenant);
            metrics.forEach(metric -> registerMetric(metric, tenant));

            periodMetricsService.scheduleCustomMetric(metrics, tenant);
        } catch (Exception e) {
            log.error("Error reading metric configuration from {}", updatedKey, e);
        }
    }

    private void registerMetric(CustomMetric metric, String tenantKey) {
        String metricName = METRICS_PREFIX + "." + tenantKey.toLowerCase() + "." + metric.getName();
        
        Gauge.builder(metricName,
                customMetricsService,
                svc -> toDouble(svc.getMetric(metric.getName(), metric, tenantKey)))
            .register(meterRegistry);
    }

    private void cleanupTenant(String tenant) {
        String tenantLower = tenant.toLowerCase();
        String metricPrefix = METRICS_PREFIX + "." + tenantLower + ".";

        periodMetricsService.scheduleCustomMetric(emptyList(), tenant);

        meterRegistry.getMeters().stream()
            .filter(m -> m.getId().getName().startsWith(metricPrefix))
            .forEach(meterRegistry::remove);
    }

    private double toDouble(Object value) {
        if (value instanceof Number num) {
            return num.doubleValue();
        }
        return 0d;
    }

    public boolean isListeningConfiguration(final String updatedKey) {
        return this.matcher.match(mappingPath, updatedKey);
    }

    public void onInit(final String configKey, final String configValue) {
        if (this.isListeningConfiguration(configKey)) {
            this.onRefresh(configKey, configValue);
        }
    }

    @Data
    public static class CustomMetric {
        private String name;
        private Integer updatePeriodSeconds;
    }

}
