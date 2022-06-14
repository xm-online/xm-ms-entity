package com.icthh.xm.ms.entity.config;

import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CustomMetricConfiguration {

    private static final String LIVENESS_PROBE_TIMEOUT_METRIC_NAME = "liveness_probe_timeout_seconds";
    private static final String READINESS_PROBE_TIMEOUT_METRIC_NAME = "readiness_probe_timeout_seconds";

    @Bean
    public MeterBinder healthCheckTimeoutMetrics(@Value("${application.liveness-probe-timeout-seconds}") String livenessProbeTimeoutSeconds,
                                                 @Value("${application.readiness-probe-timeout-seconds}") String readinessProbeTimeoutSeconds
    ) {
        return registry -> {
            registry.gauge(LIVENESS_PROBE_TIMEOUT_METRIC_NAME, Long.parseLong(livenessProbeTimeoutSeconds));
            registry.gauge(READINESS_PROBE_TIMEOUT_METRIC_NAME, Long.parseLong(readinessProbeTimeoutSeconds));
        };
    }
}
