package com.icthh.xm.ms.entity.service;

import jakarta.annotation.Nonnull;

import java.util.concurrent.CompletableFuture;

public interface IElasticsearchIndexService {

    CompletableFuture<Long> reindexAllAsync();

    CompletableFuture<Long> reindexByTypeKeyAsync(@Nonnull String typeKey);

    CompletableFuture<Long> reindexByIdsAsync(@Nonnull Iterable<Long> ids);

    long reindexAll();

    long reindexByTypeKey(@Nonnull String typeKey);

    long reindexByTypeKey(@Nonnull String typeKey, Integer startFrom);

    long reindexByIds(@Nonnull Iterable<Long> ids);
}
