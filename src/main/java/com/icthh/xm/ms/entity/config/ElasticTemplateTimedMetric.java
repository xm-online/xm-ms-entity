package com.icthh.xm.ms.entity.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.output.CountingOutputStream;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class ElasticTemplateTimedMetric {

    private final MeterRegistry meterRegistry;
    private final ApplicationProperties applicationProperties;
    private final ObjectMapper objectMapper;

    @Around("within(org.springframework.data.elasticsearch.core.ElasticsearchTemplate)")
    public Object measureMethodExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String entityClass = extractEntityClassName(joinPoint.getArgs());

        Timer timer = Timer.builder("elasticsearch.template.method.execution")
            .tag("method", methodName)
            .tag("entityClass", entityClass)
            .description("Time taken to execute elastic search method")
            .register(meterRegistry);

        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            Object result = joinPoint.proceed();
            recordResponseSize(methodName, entityClass, result);
            return result;
        } finally {
            sample.stop(timer);
        }
    }

    /**
     * Records an estimated serialized response size (bytes) per method/entity-class. Hit count
     * alone misses a response made huge by a handful of oversized documents (e.g. a product with
     * a megabytes-large nested JSON payload) — this metric is sized by content, not count, so it
     * catches that case too.
     */
    private void recordResponseSize(String methodName, String entityClass, Object result) {
        if (!applicationProperties.getElasticsearch().isLogResponseSizeEnabled()) {
            return;
        }
        Collection<?> content = null;
        if (result instanceof Page) {
            content = ((Page<?>) result).getContent();
        } else if (result instanceof Collection) {
            content = (Collection<?>) result;
        }
        if (content == null || content.isEmpty()) {
            return;
        }
        long sizeBytes = estimateSerializedSizeBytes(content);
        if (sizeBytes >= 0) {
            DistributionSummary.builder("elasticsearch.template.method.response.size.bytes")
                .tag("method", methodName)
                .tag("entityClass", entityClass)
                .description("Estimated serialized size of an elastic search method's returned content")
                .register(meterRegistry)
                .record(sizeBytes);
        }
    }

    private long estimateSerializedSizeBytes(Collection<?> content) {
        try (CountingOutputStream countingOutputStream = new CountingOutputStream(OutputStream.nullOutputStream())) {
            objectMapper.writeValue(countingOutputStream, content);
            return countingOutputStream.getByteCount();
        } catch (IOException e) {
            log.warn("Failed to estimate Elasticsearch response size", e);
            return -1;
        }
    }

    private String extractEntityClassName(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof Class) {
                return ((Class<?>) arg).getSimpleName();
            }
        }
        return "unknown";
    }
}
