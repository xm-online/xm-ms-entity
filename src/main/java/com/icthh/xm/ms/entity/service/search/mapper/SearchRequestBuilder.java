package com.icthh.xm.ms.entity.service.search.mapper;

import co.elastic.clients.elasticsearch._types.FieldSort;
import co.elastic.clients.elasticsearch._types.ScoreSort;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.Time;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.search.SourceConfig;
import com.icthh.xm.ms.entity.service.search.builder.QueryBuilder;
import com.icthh.xm.ms.entity.service.search.builder.SearchRequestQueryBuilder;
import com.icthh.xm.ms.entity.service.search.builder.aggregation.SearchRequestAggregationBuilder;
import com.icthh.xm.ms.entity.service.search.filter.SourceFilter;
import com.icthh.xm.ms.entity.service.search.query.SearchQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SearchRequestBuilder {

    private static final String FIELD_SCORE = "_score";

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

        if (searchQuery.getSort() != null) {
            builder.sort(getSortOptions(searchQuery.getSort()));
        }

        builder.from(startRecord);

        Query query = searchRequestQueryBuilder.buildQuery(searchQuery.getQuery());
        builder.query(query);

        Map<String, Aggregation> aggregationMap = searchRequestAggregationBuilder.buildAggregationMap(searchQuery.getAggregations());
        builder.aggregations(aggregationMap);

        return builder.build();
    }

    public SearchRequest buildScrollSearchRequest(SearchQuery searchQuery, long scrollTimeInMillis) {
        SearchRequest.Builder builder = new SearchRequest.Builder()
            .index(searchQuery.getIndices())
            .from(0)
            .scroll(Time.of(time -> time.time(scrollTimeInMillis + "ms")))
            .version(true);

        if (searchQuery.getPageable().isPaged()) {
            builder.size(searchQuery.getPageable().getPageSize());
        }

        Query query = searchRequestQueryBuilder.buildQuery(searchQuery.getQuery());
        builder.query(query);

        return builder.build();
    }

    public Query buildSearchQuery(QueryBuilder queryBuilder) {
        return searchRequestQueryBuilder.buildQuery(queryBuilder);
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

    private List<SortOptions> getSortOptions(Sort sort) {
        List<SortOptions> sortOptions = new ArrayList<>();

        for (Sort.Order order : sort) {
            SortOptions.Builder sortOptionsBuilder = new SortOptions.Builder();
            SortOrder sortOrder = order.getDirection().isDescending() ? SortOrder.Desc : SortOrder.Asc;

            if (FIELD_SCORE.equals(order.getProperty())) {
                ScoreSort scoreSort = ScoreSort.of(s -> s.order(sortOrder));
                sortOptionsBuilder.score(scoreSort);
            } else {
                FieldSort.Builder fieldSortBuilder = new FieldSort.Builder()
                    .field(order.getProperty())
                    .order(sortOrder);

                if (order.getNullHandling() == Sort.NullHandling.NULLS_FIRST) {
                    fieldSortBuilder.missing("_first");
                } else if (order.getNullHandling() == Sort.NullHandling.NULLS_LAST) {
                    fieldSortBuilder.missing("_last");
                }
                sortOptionsBuilder.field(fieldSortBuilder.build());
            }

            sortOptions.add(sortOptionsBuilder.build());
        }
        return sortOptions;
    }
}
