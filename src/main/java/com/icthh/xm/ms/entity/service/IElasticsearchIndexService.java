package com.icthh.xm.ms.entity.service;

import jakarta.annotation.Nonnull;

import java.util.concurrent.CompletableFuture;

public interface IElasticsearchIndexService {

    /**
     * Recreates index and then reindexes ALL entities from database asynchronously.
     * @return @{@link CompletableFuture<Long>} with a number of reindexed entities.
     */
    CompletableFuture<Long> reindexAllAsync();

    /**
     * Refreshes entities in elasticsearch index filtered by typeKey asynchronously.
     *
     * Does not recreate index.
     * @param typeKey typeKey to filter source entities.
     * @return @{@link CompletableFuture<Long>} with a number of reindexed entities.
     */
    CompletableFuture<Long> reindexByTypeKeyAsync(@Nonnull String typeKey);

    /**
     * Refreshes entities in elasticsearch index filtered by collection of IDs asynchronously.
     *
     * Does not recreate index.
     * @param ids - collection of IDs of entities to be reindexed.
     * @return @{@link CompletableFuture<Long>} with a number of reindexed entities.
     */
    CompletableFuture<Long> reindexByIdsAsync(@Nonnull Iterable<Long> ids);

    /**
     * Recreates index and then reindexes ALL entities from database.
     * @return number of reindexed entities.
     */
    long reindexAll();

    /**
     * Refreshes entities in elasticsearch index filtered by typeKey.
     *
     * Does not recreate index.
     * @param typeKey typeKey to filter source entities.
     * @return number of reindexed entities.
     */
    long reindexByTypeKey(@Nonnull String typeKey);

    /**
     * Refreshes entities in elasticsearch index filtered by typeKey with startFrom.
     *
     * Does not recreate index.
     * @param typeKey typeKey to filter source entities.
     * @param startFrom starting ID
     * @return number of reindexed entities.
     */
    long reindexByTypeKey(@Nonnull String typeKey, Integer startFrom);

    /**
     * Refreshes entities in elasticsearch index filtered by collection of IDs.
     *
     * Does not recreate index.
     * @param ids - collection of IDs of entities to be reindexed.
     * @return number of reindexed entities.
     */
    long reindexByIds(@Nonnull Iterable<Long> ids);
}
