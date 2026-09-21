package com.icthh.xm.ms.entity.config;

import com.codahale.metrics.MetricRegistry;
import com.icthh.xm.commons.scheduler.metric.SchedulerMetricsSet;
import com.ryantenney.metrics.spring.config.annotation.EnableMetrics;
import com.ryantenney.metrics.spring.config.annotation.MetricsConfigurerAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Slf4j
@Configuration
@EnableMetrics(proxyTargetClass = true)
@RequiredArgsConstructor
public class EntityMetricsConfiguration extends MetricsConfigurerAdapter {

    private static final String SCHEDULER = "scheduler";

    private final MetricRegistry metricRegistry;
    private final SchedulerMetricsSet schedulerMetricsSet;

    @PostConstruct
    public void init() {
        // Note: Hikari pool metrics are intentionally left to Spring Boot's default
        // Micrometer HikariDataSourceMetricsBinder, so they are exported to
        // /management/prometheus (hikaricp_connections_active, etc.) instead of
        // being rerouted to this legacy Dropwizard MetricRegistry.
        metricRegistry.register(SCHEDULER, schedulerMetricsSet);
    }
}
