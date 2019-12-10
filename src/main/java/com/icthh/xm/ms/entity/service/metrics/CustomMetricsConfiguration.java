package com.icthh.xm.ms.entity.service.metrics;

import static com.icthh.xm.commons.tenant.TenantContextUtils.getRequiredTenantKeyValue;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricSet;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

@Slf4j
@Component
public class CustomMetricsConfiguration implements RefreshableConfiguration {

    private static final String METRICS_PREFIX = "custom.metrics.";

    private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    private final ConcurrentHashMap<String, List<CustomMetric>> configuration = new ConcurrentHashMap<>();
    private final AntPathMatcher matcher = new AntPathMatcher();

    private final MetricRegistry metricRegistry;
    private final CustomMetricsService customMetricsService;
    private final PeriodicMetricsService periodMetricsService;
    private final String mappingPath;

    public CustomMetricsConfiguration(MetricRegistry metricRegistry,
                                      CustomMetricsService customMetricsService,
                                      PeriodicMetricsService periodMetricsService,
                                      @Value("${spring.application.name}") String appName) {

        this.metricRegistry = metricRegistry;
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

            List<CustomMetric> metrics = mapper.readValue(config, new TypeReference<List<CustomMetric>>() {});
            this.configuration.put(tenant, metrics);

            log.info("Metric configuration was updated for tenant [{}] by key [{}]", tenant, updatedKey);
            String metricsName = METRICS_PREFIX + tenant.toLowerCase();
            metricRegistry.removeMatching((name, metric) -> name.startsWith(metricsName));

            Map<String, Metric> metricsMap = metrics.stream().collect(toMap(CustomMetric::getName, toMetric(tenant)));
            metricRegistry.register(metricsName, (MetricSet) () -> metricsMap);

            periodMetricsService.scheduleCustomMetric(metrics, tenant);
        } catch (Exception e) {
            log.error("Error read metric configuration from path " + updatedKey, e);
        }
    }

    private Function<CustomMetric, Gauge<?>> toMetric(String tenantKey) {
        return (metric) -> () -> customMetricsService.getMetric(metric.getName(), metric, tenantKey);
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
