package com.icthh.xm.ms.entity.service.metrics;

import com.icthh.xm.ms.entity.service.metrics.CustomMetricsConfiguration.CustomMetric;

import java.time.Duration;
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
    private final ThreadPoolTaskScheduler periodicMetricsTaskScheduler;
    private final CustomMetricsService customMetricsService;

    public PeriodicMetricsService(CustomMetricsService customMetricsService, ThreadPoolTaskScheduler periodicMetricsTaskScheduler) {
        this.customMetricsService = customMetricsService;
        this.periodicMetricsTaskScheduler = periodicMetricsTaskScheduler;
    }

    public void scheduleCustomMetric(List<CustomMetric> customMetrics, String tenantKey) {
        metricsTasksByTenant.computeIfAbsent(tenantKey, (key) -> new ConcurrentHashMap<>());
        Map<String, ScheduledFuture> scheduledTasks = metricsTasksByTenant.get(tenantKey);
        scheduledTasks.values().forEach(it -> it.cancel(false));

        Map<String, ScheduledFuture> tasks = new HashMap<>();
        customMetrics.stream().filter(it -> it.getUpdatePeriodSeconds() != null && it.getUpdatePeriodSeconds() > 0)
                     .forEach(customMetric -> schedulerCustomMetric(tenantKey, tasks, customMetric));

        metricsTasksByTenant.put(tenantKey, tasks);
    }

    private void schedulerCustomMetric(String tenantKey, Map<String, ScheduledFuture> tasks,
                                       CustomMetric customMetric) {
        Runnable task = () -> customMetricsService.updateMetric(customMetric.getName(), tenantKey);
        Integer period = customMetric.getUpdatePeriodSeconds();
        ScheduledFuture<?> future = periodicMetricsTaskScheduler.scheduleAtFixedRate(task, Duration.ofMillis(period * 1000));
        tasks.put(customMetric.getName(), future);
    }

}
