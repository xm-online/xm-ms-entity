package com.icthh.xm.ms.entity.config;

import com.codahale.metrics.MetricRegistry;
import com.icthh.xm.commons.scheduler.metric.SchedulerMetricsSet;
import com.ryantenney.metrics.spring.config.annotation.EnableMetrics;
import com.ryantenney.metrics.spring.config.annotation.MetricsConfigurerAdapter;
import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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

    private HikariDataSource hikariDataSource;

    @Autowired(required = false)
    public void setHikariDataSource(HikariDataSource hikariDataSource) {
        this.hikariDataSource = hikariDataSource;
    }

    @PostConstruct
    public void init() {
        if (hikariDataSource != null) {
            log.debug("Monitoring the datasource");
            // remove the factory created by HikariDataSourceMetricsPostProcessor until JHipster migrate to Micrometer
            hikariDataSource.setMetricsTrackerFactory(null);
            hikariDataSource.setMetricRegistry(metricRegistry);
        }

        metricRegistry.register(SCHEDULER, schedulerMetricsSet);
    }
}
