package com.icthh.xm.ms.entity.service.metrics;

import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.isBlank;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import com.icthh.xm.commons.tenant.YamlMapperUtils;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
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

    private static final String METRICS_PREFIX = "custom.metrics.";

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
            String tenant = this.matcher.extractUriTemplateVariables(mappingPath, updatedKey).get("tenantName");
            if (isBlank(config)) {
                this.configuration.remove(tenant);
                periodMetricsService.scheduleCustomMetric(emptyList(), tenant);
                return;
            }

            List<CustomMetric> metrics = mapper.readValue(config, new TypeReference<>() {});
            this.configuration.put(tenant, metrics);

            log.info("Metric configuration was updated for tenant [{}] by key [{}]", tenant, updatedKey);
            String metricsNamePrefix = METRICS_PREFIX + tenant.toLowerCase();

            removeMetrics(metricsNamePrefix);
            registerMetrics(metrics, metricsNamePrefix, tenant);

            periodMetricsService.scheduleCustomMetric(metrics, tenant);
        } catch (Exception e) {
            log.error("Error read metric configuration from path {}", updatedKey, e);
        }
    }

    private void removeMetrics(String metricsNamePrefix) {
        meterRegistry.getMeters().stream()
            .filter(meter -> meter.getId().getName().startsWith(metricsNamePrefix))
            .forEach(meterRegistry::remove);
    }

    private void registerMetrics(List<CustomMetric> metrics, String metricsNamePrefix, String tenant) {
        metrics.forEach(metric -> {
            String gaugeName = metricsNamePrefix + "." + metric.getName();
            Gauge.builder(gaugeName, customMetricsService, cms -> toDouble(cms.getMetric(metric.getName(), metric, tenant)))
                 .description("Custom metric for tenant: " + tenant)
                 .register(meterRegistry);
        });
    }

    private Double toDouble(Object value) {
        if (value instanceof Number num) {
            return num.doubleValue();
        }
        return Double.NaN;
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