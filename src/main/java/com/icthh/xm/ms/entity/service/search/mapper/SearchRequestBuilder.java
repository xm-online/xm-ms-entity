package com.icthh.xm.ms.entity.service.search.mapper;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import com.icthh.xm.ms.entity.service.search.builder.SearchRequestQueryBuilder;
import com.icthh.xm.ms.entity.service.search.query.SearchQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SearchRequestBuilder {

    private final SearchRequestQueryBuilder searchRequestQueryBuilder;

    public SearchRequest buildSearchRequest(SearchQuery searchQuery) {
        SearchRequest.Builder builder = new SearchRequest.Builder();
        builder.index(searchQuery.getIndices());

        Query query = searchRequestQueryBuilder.buildQuery(searchQuery.getQuery());
        builder.query(query);

        return builder.build();
    }
}
