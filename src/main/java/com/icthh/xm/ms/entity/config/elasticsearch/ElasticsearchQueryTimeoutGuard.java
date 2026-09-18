package com.icthh.xm.ms.entity.config.elasticsearch;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.ms.entity.config.ApplicationProperties;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Runs Elasticsearch calls with a hard, application-side timeout.
 *
 * <p>The legacy {@code TransportClient} used by entity's Elasticsearch integration has no
 * built-in bound on {@code actionGet()}: if a node becomes unreachable/reconnecting
 * (e.g. {@code NodeNotConnectedException}) the calling thread can block indefinitely
 * (observed for ~50 minutes during the 2026-09-18 incident). That single stuck call then
 * pins one Undertow worker thread and, if it holds a JPA transaction, a Hikari connection,
 * which cascades into fleet-wide pool exhaustion.
 *
 * <p>This guard executes the ES call on a small, dedicated, bounded executor (isolated from
 * both the Undertow worker pool and the general async executor) and enforces
 * {@link com.icthh.xm.ms.entity.config.ApplicationProperties.Elasticsearch#getQueryTimeout()}.
 * On timeout the underlying task is interrupted (unblocking the ES client's internal
 * {@code CountDownLatch.await()}) and a fast {@link BusinessException} is thrown so the caller
 * fails quickly instead of hanging.
 */
@Slf4j
@Component
public class ElasticsearchQueryTimeoutGuard {

    public static final String ERROR_ELASTICSEARCH_TIMEOUT = "error.elasticsearch.timeout";

    private final ExecutorService elasticsearchQueryExecutor;
    private final Duration queryTimeout;

    public ElasticsearchQueryTimeoutGuard(ExecutorService elasticsearchQueryExecutor,
                                          ApplicationProperties applicationProperties) {
        this.elasticsearchQueryExecutor = elasticsearchQueryExecutor;
        this.queryTimeout = applicationProperties.getElasticsearch().getQueryTimeout();
    }

    public <T> T runWithTimeout(Supplier<T> elasticsearchCall) {
        Future<T> future = elasticsearchQueryExecutor.submit(elasticsearchCall::get);
        try {
            return future.get(queryTimeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            log.error("Elasticsearch query exceeded {} timeout, cancelling and failing fast", queryTimeout);
            throw new BusinessException(ERROR_ELASTICSEARCH_TIMEOUT,
                "Elasticsearch query did not complete within " + queryTimeout);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ERROR_ELASTICSEARCH_TIMEOUT, "Elasticsearch query was interrupted");
        } catch (java.util.concurrent.ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new BusinessException(ERROR_ELASTICSEARCH_TIMEOUT, "Elasticsearch query failed: " + cause);
        }
    }
}
