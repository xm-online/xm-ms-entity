package com.icthh.xm.ms.entity.service.metrics;

import com.icthh.xm.ms.entity.config.ApplicationProperties;
import com.icthh.xm.ms.entity.service.metrics.CustomMetricsConfiguration.CustomMetric;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PeriodicMetricsService {

    private final Map<String, Map<String, ScheduledFuture>> metricsTasksByTenant = new ConcurrentHashMap<>();
    private final ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
    private final CustomMetricsService customMetricsService;

    public PeriodicMetricsService(CustomMetricsService customMetricsService, ApplicationProperties applicationProperties) {
        this.customMetricsService = customMetricsService;
        taskScheduler.initialize();
        taskScheduler.setPoolSize(applicationProperties.getPeriodicMetricPoolSize());
    }

    public void scheduleCustomMetric(List<CustomMetric> customMetrics, String tenantKey) {
        metricsTasksByTenant.computeIfAbsent(tenantKey, (key) -> new ConcurrentHashMap<>());
        Map<String, ScheduledFuture> scheduledTasks = metricsTasksByTenant.get(tenantKey);
        scheduledTasks.values().forEach(it -> it.cancel(false));

        Map<String, ScheduledFuture> tasks = new HashMap<>();
        customMetrics.stream().filter(it -> it.getUpdatePeriodSeconds() != null && it.getUpdatePeriodSeconds() > 0)
                     .forEach(customMetric -> {
                         Runnable task = () -> customMetricsService.updateMetric(customMetric.getName(), tenantKey);
                         Integer period = customMetric.getUpdatePeriodSeconds();
                         ScheduledFuture<?> future = taskScheduler.scheduleAtFixedRate(task, period * 1000);
                         tasks.put(customMetric.getName(), future);
        });

        metricsTasksByTenant.put(tenantKey, tasks);
    }

}
