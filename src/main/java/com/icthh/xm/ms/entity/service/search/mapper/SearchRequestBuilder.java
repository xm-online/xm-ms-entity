package com.icthh.xm.ms.entity.service.search.mapper;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.search.SourceConfig;
import com.icthh.xm.ms.entity.service.search.builder.SearchRequestQueryBuilder;
import com.icthh.xm.ms.entity.service.search.builder.aggregation.SearchRequestAggregationBuilder;
import com.icthh.xm.ms.entity.service.search.filter.SourceFilter;
import com.icthh.xm.ms.entity.service.search.query.SearchQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SearchRequestBuilder {

    private final SearchRequestQueryBuilder searchRequestQueryBuilder;
    private final SearchRequestAggregationBuilder searchRequestAggregationBuilder;

    public SearchRequest buildSearchRequest(SearchQuery searchQuery) {
        SearchRequest.Builder builder = new SearchRequest.Builder();
        builder.index(searchQuery.getIndices());

        SourceFilter xmSourceFilter = searchQuery.getSourceFilter();
        if (xmSourceFilter != null) {
            co.elastic.clients.elasticsearch.core.search.SourceFilter sourceFilter =
                co.elastic.clients.elasticsearch.core.search.SourceFilter.of(sourceFilterBuilder ->
                    getBuilder(sourceFilterBuilder, xmSourceFilter));
            SourceConfig sourceConfig = SourceConfig.of(b -> b.filter(sourceFilter));
            builder.source(sourceConfig);
        }

        int startRecord = 0;
        if (searchQuery.getPageable().isPaged()) {
            startRecord = searchQuery.getPageable().getPageNumber() * searchQuery.getPageable().getPageSize();
            builder.size(searchQuery.getPageable().getPageSize());
        }
        builder.from(startRecord);

        Query query = searchRequestQueryBuilder.buildQuery(searchQuery.getQuery());
        builder.query(query);

        Map<String, Aggregation> aggregationMap = searchRequestAggregationBuilder.buildAggregationMap(searchQuery.getAggregations());
        builder.aggregations(aggregationMap);

        SearchRequest searchRequest = builder.build();
        return searchRequest;
    }

    private static co.elastic.clients.elasticsearch.core.search.SourceFilter.Builder getBuilder(
        co.elastic.clients.elasticsearch.core.search.SourceFilter.Builder sourceFilterBuilder,
        SourceFilter xmSourceFilter) {
        String[] excludes = xmSourceFilter.getExcludes();
        if (excludes != null) {
            sourceFilterBuilder.excludes(List.of(excludes));
        }
        String[] includes = xmSourceFilter.getIncludes();
        if (includes != null) {
            sourceFilterBuilder.includes(List.of(includes));
        }
        return sourceFilterBuilder;
    }

}
