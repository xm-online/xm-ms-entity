package com.icthh.xm.ms.entity.config;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.aop.ObservedAspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(value = "spring.kafka.template.observation-enabled", havingValue = "true")
public class TracingConfig {

    @Bean
    ObservedAspect observedAspect(ObservationRegistry observationRegistry) {
        return new ObservedAspect(observationRegistry);
    }

    // To propagate traceId across third services with RestTemplate client,
    // rest template should be created using builder
    @Bean
    RestTemplate tracingRestTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }
}
