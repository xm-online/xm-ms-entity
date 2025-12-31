package com.icthh.xm.ms.entity.service.wrapper;

import com.icthh.xm.ms.entity.service.IElasticsearchIndexService;
import com.icthh.xm.ms.entity.service.TransactionPropagationService;
import jakarta.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@ConditionalOnProperty(name = "application.elastic-enabled", havingValue = "false")
public class NoOpElasticsearchIndexService extends TransactionPropagationService<NoOpElasticsearchIndexService> implements IElasticsearchIndexService {

    /**
     * Reindex all entities asynchronously (stub implementation - returns 0).
     */
    public CompletableFuture<Long> reindexAllAsync() {
        log.warn("Elasticsearch is disabled. Skipping reindexAllAsync()");
        return CompletableFuture.completedFuture(0L);
    }

    /**
     * Reindex by type key asynchronously (stub implementation - returns 0).
     */
    public CompletableFuture<Long> reindexByTypeKeyAsync(@Nonnull String typeKey) {
        Objects.requireNonNull(typeKey, "typeKey should not be null");
        log.warn("Elasticsearch is disabled. Skipping reindexByTypeKeyAsync() for typeKey: {}", typeKey);
        return CompletableFuture.completedFuture(0L);
    }

    /**
     * Reindex by IDs asynchronously (stub implementation - returns 0).
     */
    public CompletableFuture<Long> reindexByIdsAsync(@Nonnull Iterable<Long> ids) {
        Objects.requireNonNull(ids, "ids should not be null");
        log.warn("Elasticsearch is disabled. Skipping reindexByIdsAsync()");
        return CompletableFuture.completedFuture(0L);
    }

    /**
     * Reindex all entities (stub implementation - returns 0).
     */
    public long reindexAll() {
        log.warn("Elasticsearch is disabled. Skipping reindexAll()");
        return 0L;
    }

    /**
     * Reindex by type key (stub implementation - returns 0).
     */
    public long reindexByTypeKey(@Nonnull String typeKey) {
        Objects.requireNonNull(typeKey, "typeKey should not be null");
        log.warn("Elasticsearch is disabled. Skipping reindexByTypeKey() for typeKey: {}", typeKey);
        return 0L;
    }

    /**
     * Reindex by type key with startFrom (stub implementation - returns 0).
     */
    public long reindexByTypeKey(@Nonnull String typeKey, Integer startFrom) {
        Objects.requireNonNull(typeKey, "typeKey should not be null");
        log.warn("Elasticsearch is disabled. Skipping reindexByTypeKey() for typeKey: {}, startFrom: {}", typeKey, startFrom);
        return 0L;
    }

    /**
     * Reindex by IDs (stub implementation - returns 0).
     */
    public long reindexByIds(@Nonnull Iterable<Long> ids) {
        Objects.requireNonNull(ids, "ids should not be null");
        log.warn("Elasticsearch is disabled. Skipping reindexByIds()");
        return 0L;
    }
}
