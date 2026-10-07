package com.icthh.xm.ms.entity.repository.search;

import static java.util.Objects.nonNull;
import static org.elasticsearch.index.query.QueryBuilders.queryStringQuery;
import static org.springframework.data.elasticsearch.core.query.Query.DEFAULT_PAGE;

import com.icthh.xm.commons.permission.service.PermissionCheckService;
import com.icthh.xm.ms.entity.config.ApplicationProperties;
import com.icthh.xm.ms.entity.config.elasticsearch.ElasticsearchQueryTimeoutGuard;
import com.icthh.xm.ms.entity.repository.search.translator.SpelToElasticTranslator;
import com.icthh.xm.ms.entity.service.dto.SearchDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.output.CountingOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.ScrolledPage;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class PermittedSearchRepository {

    private static final String AND = " AND ";

    private final PermissionCheckService permissionCheckService;
    private final SpelToElasticTranslator spelToElasticTranslator;
    private final ElasticsearchTemplate elasticsearchTemplate;
    private final ElasticsearchQueryTimeoutGuard elasticsearchQueryTimeoutGuard;
    private final ApplicationProperties applicationProperties;
    private final ObjectMapper objectMapper;

    /**
     * Search permitted entities.
     * @param query the elastic query
     * @param entityClass the search entity class
     * @param privilegeKey the privilege key
     * @return permitted entities
     */
    public <T> List<T> search(String query, Class<T> entityClass, String privilegeKey) {
        String permittedQuery = buildPermittedQuery(query, privilegeKey);
        SearchQuery esQuery = buildQuery(permittedQuery, null, null);

        StopWatch stopWatch = StopWatch.createStarted();
        List<T> results = elasticsearchQueryTimeoutGuard.runWithTimeout(
            () -> getElasticsearchTemplate().queryForList(esQuery, entityClass));
        logSearchResult("search", permittedQuery, stopWatch.getTime(), results.size(), results);

        return results;
    }

    /**
     * Search permitted entities.
     * @param query the elastic query
     * @param pageable the page info
     * @param entityClass the search entity class
     * @return permitted entities
     * @deprecated use {@link #searchForPage(SearchDto, String)} instead
     */
    @Deprecated
    public <T> Page<T> search(String query, Pageable pageable, Class<T> entityClass, String privilegeKey) {
        return searchForPage(SearchDto.builder()
            .entityClass(entityClass)
            .pageable(pageable)
            .query(query)
            .build(), privilegeKey);
    }

    /**
     * Search permitted entities with scroll
     * @param scrollTimeInMillis The time in millisecond for scroll feature
     * @param query the elastic query
     * @param pageable the page info
     * @param entityClass the search entity class
     * @param privilegeKey the privilege key
     * @return permitted entities
     */
    public <T> Page<T> search(Long scrollTimeInMillis,
                              String query,
                              Pageable pageable,
                              Class<T> entityClass,
                              String privilegeKey) {

        String scrollId = null;
        List<T> resultList = new ArrayList<>();
        try {
            String permittedQuery = buildPermittedQuery(query, privilegeKey);
            SearchQuery searchQuery = buildQuery(permittedQuery, pageable, null);
            StopWatch stopWatch = StopWatch.createStarted();

            ScrolledPage<T> scrollResult = (ScrolledPage<T>) elasticsearchQueryTimeoutGuard.runWithTimeout(
                () -> getElasticsearchTemplate().startScroll(scrollTimeInMillis, searchQuery, entityClass));

            scrollId = scrollResult.getScrollId();

            while (scrollResult.hasContent()) {
                resultList.addAll(scrollResult.getContent());
                scrollId = scrollResult.getScrollId();

                String currentScrollId = scrollId;
                scrollResult = (ScrolledPage<T>) elasticsearchQueryTimeoutGuard.runWithTimeout(
                    () -> getElasticsearchTemplate().continueScroll(currentScrollId, scrollTimeInMillis, entityClass));
            }
            logSearchResult("searchWithScroll", permittedQuery, stopWatch.getTime(),
                resultList.size(), resultList);
        } finally {
            if (nonNull(scrollId)) {
                getElasticsearchTemplate().clearScroll(scrollId);
            }
        }
        return new PageImpl<>(resultList, pageable, resultList.size());
    }

    private SearchQuery buildQuery(String permittedQuery, Pageable pageable, FetchSourceFilter fetchSourceFilter) {
        log.debug("Executing DSL '{}'", permittedQuery);

        return new NativeSearchQueryBuilder()
            .withQuery(queryStringQuery(permittedQuery))
            .withSourceFilter(fetchSourceFilter)
            .withPageable(pageable == null ? DEFAULT_PAGE : pageable)
            .build();
    }

    String buildPermittedQuery(String query, String privilegeKey) {
        String permittedQuery = query;

        String permittedCondition = createPermissionCondition(privilegeKey);
        if (StringUtils.isNotBlank(permittedCondition)) {
            if (StringUtils.isBlank(query)) {
                permittedQuery = permittedCondition;
            } else {
                permittedQuery += AND + "(" + permittedCondition + ")";
            }
        }

        return permittedQuery;
    }

    private String createPermissionCondition(String privilegeKey) {
        return permissionCheckService.createCondition(
            SecurityContextHolder.getContext().getAuthentication(), privilegeKey,
            spelToElasticTranslator);
    }

    // do not renamed! called from lep for not simple string query
    public ElasticsearchTemplate getElasticsearchTemplate() {
        return elasticsearchTemplate;
    }

    public <T> Page<T> searchForPage(SearchDto searchDto, String privilegeKey) {
        String permittedQuery = buildPermittedQuery(searchDto.getQuery(), privilegeKey);
        SearchQuery query = buildQuery(permittedQuery, searchDto.getPageable(), searchDto.getFetchSourceFilter());

        StopWatch stopWatch = StopWatch.createStarted();
        AggregatedPage queryResult = elasticsearchQueryTimeoutGuard.runWithTimeout(
            () -> getElasticsearchTemplate().queryForPage(query, searchDto.getEntityClass()));
        logSearchResult("searchForPage", permittedQuery, stopWatch.getTime(),
            queryResult.getTotalElements(), queryResult.getContent());

        return queryResult;
    }

    /**
     * Logs search outcome (query, duration, total matched hits, hits actually returned, and an
     * estimated serialized response size). Logged at DEBUG unconditionally so it is available
     * without enabling TRACE, and escalated to WARN when either total matched hits reach
     * {@link ApplicationProperties.Elasticsearch#getLargeResultLogThresholdHits()} or the
     * returned content's estimated size reaches
     * {@link ApplicationProperties.Elasticsearch#getLargeResultLogThresholdBytes()}. The byte
     * threshold is what catches a response made large by a handful of oversized documents
     * (e.g. a product with a huge nested JSON payload), which a hit-count threshold alone misses.
     */
    void logSearchResult(String method, String query, long durationMs, long totalHits, List<?> content) {
        ApplicationProperties.Elasticsearch config = applicationProperties.getElasticsearch();
        int returnedHits = content.size();
        long responseSizeBytes = estimateSerializedSizeBytes(content);
        String sizeAsString = responseSizeBytes >= 0 ? String.valueOf(responseSizeBytes) : "n/a";

        boolean large = totalHits >= config.getLargeResultLogThresholdHits()
            || (responseSizeBytes >= 0 && responseSizeBytes >= config.getLargeResultLogThresholdBytes());

        if (large) {
            log.warn("{}: large ES result: query: '{}', totalHits: {}, returnedHits: {}, "
                    + "responseSizeBytes: {}, duration: {} ms",
                method, query, totalHits, returnedHits, sizeAsString, durationMs);
        } else {
            log.debug("{}: query: '{}', totalHits: {}, returnedHits: {}, responseSizeBytes: {}, duration: {} ms",
                method, query, totalHits, returnedHits, sizeAsString, durationMs);
        }
    }

    /**
     * Estimates the response payload size by serializing {@code content} with the same
     * {@link ObjectMapper} used for the HTTP response, counting bytes written to a null output
     * stream (no extra copy of the JSON is retained in memory). Returns -1 if disabled via
     * {@link ApplicationProperties.Elasticsearch#isLogResponseSizeEnabled()} or on failure.
     */
    private long estimateSerializedSizeBytes(List<?> content) {
        if (!applicationProperties.getElasticsearch().isLogResponseSizeEnabled() || content.isEmpty()) {
            return content.isEmpty() ? 0 : -1;
        }
        try (CountingOutputStream countingOutputStream = new CountingOutputStream(OutputStream.nullOutputStream())) {
            objectMapper.writeValue(countingOutputStream, content);
            return countingOutputStream.getByteCount();
        } catch (IOException e) {
            log.debug("Failed to estimate Elasticsearch response size", e);
            return -1;
        }
    }
}
