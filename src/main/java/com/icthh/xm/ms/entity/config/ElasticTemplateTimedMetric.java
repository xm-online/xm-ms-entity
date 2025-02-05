package com.icthh.xm.ms.entity.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class ElasticTemplateTimedMetric {

    private final MeterRegistry meterRegistry;

    @Around("within(org.springframework.data.elasticsearch.core.ElasticsearchTemplate)")
    public Object measureMethodExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();

        Timer timer = Timer.builder("elasticsearch.template.method.execution")
            .tag("method", methodName)
            .description("Time taken to execute elastic search method")
            .register(meterRegistry);

        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            return joinPoint.proceed();
        } finally {
            sample.stop(timer);
        }
    }
}
