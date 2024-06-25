package com.icthh.xm.ms.entity.service.search.mapper;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import com.icthh.xm.ms.entity.service.search.builder.SearchRequestQueryBuilder;
import com.icthh.xm.ms.entity.service.search.builder.aggregation.SearchRequestAggregationBuilder;
import com.icthh.xm.ms.entity.service.search.query.SearchQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class SearchRequestBuilder {

    private final SearchRequestQueryBuilder searchRequestQueryBuilder;
    private final SearchRequestAggregationBuilder searchRequestAggregationBuilder;

    public SearchRequest buildSearchRequest(SearchQuery searchQuery) {
        SearchRequest.Builder builder = new SearchRequest.Builder();
        builder.index(searchQuery.getIndices());

        Query query = searchRequestQueryBuilder.buildQuery(searchQuery.getQuery());
        builder.query(query);

        Map<String, Aggregation> aggregationMap = searchRequestAggregationBuilder.buildAggregationMap(searchQuery.getAggregations());
        builder.aggregations(aggregationMap);

        return builder.build();
    }
}
