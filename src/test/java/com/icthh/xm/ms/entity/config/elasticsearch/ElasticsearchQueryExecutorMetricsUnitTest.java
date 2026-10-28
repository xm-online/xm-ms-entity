package com.icthh.xm.ms.entity.config.elasticsearch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.icthh.xm.ms.entity.config.ApplicationProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

public class ElasticsearchQueryExecutorMetricsUnitTest {

    @Test
    public void exposesPoolAndQueueMetricsForEsQueryExecutor() throws Exception {
        ApplicationProperties properties = new ApplicationProperties();
        properties.getElasticsearch().setQueryExecutorPoolSize(1);
        properties.getElasticsearch().setQueryExecutorQueueCapacity(1);
        SimpleMeterRegistry registry = new SimpleMeterRegistry();

        ExecutorService executor = new ElasticsearchConfiguration().elasticsearchQueryExecutor(properties, registry);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch started = new CountDownLatch(1);
        try {
            executor.submit(() -> {
                started.countDown();
                release.await();
                return null;
            });
            assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
            executor.submit(() -> null);

            assertThat(registry.get("executor.pool.size").tag("name", "es-query").gauge().value()).isEqualTo(1);
            assertThat(registry.get("executor.active").tag("name", "es-query").gauge().value()).isEqualTo(1);
            assertThat(registry.get("executor.queued").tag("name", "es-query").gauge().value()).isEqualTo(1);
            assertThat(registry.get("executor.queue.remaining").tag("name", "es-query").gauge().value()).isEqualTo(0);

            // saturated pool keeps rejecting, behaviour must be unchanged by metrics binding
            assertThatThrownBy(() -> executor.submit(() -> null))
                .isInstanceOf(RejectedExecutionException.class);
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }
}
