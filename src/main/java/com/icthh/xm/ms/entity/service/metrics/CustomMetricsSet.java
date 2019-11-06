package com.icthh.xm.ms.entity.service.metrics;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CustomMetricsSet implements MetricSet {

    private final CustomMetricsConfiguration configuration;
    private final CustomMetricsService customMetricsService;

    @Override
    public Map<String, Metric> getMetrics() {
        Map<String, Metric> metrics = new HashMap<>();
        configuration.getMetrics().forEach(it -> {
            metrics.put(it.getName(), (Gauge<?>) () -> customMetricsService.getMetric(it.getName(), it));
        });
        return metrics;
    }

}
