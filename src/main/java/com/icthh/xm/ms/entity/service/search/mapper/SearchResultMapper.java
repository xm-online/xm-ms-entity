package com.icthh.xm.ms.entity.service.search.mapper;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.icthh.xm.ms.entity.service.search.aggregation.Aggregations;
import com.icthh.xm.ms.entity.service.search.page.aggregation.AggregatedPage;
import com.icthh.xm.ms.entity.service.search.page.aggregation.impl.AggregatedPageImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SearchResultMapper {

    private final AggregationSearchResultMapper aggregationSearchResultMapper;

    public <T> AggregatedPage<T> mapSearchResults(SearchResponse<T> response, Pageable pageable) {
        String scrollId = response.scrollId();
        float maxScore = response.maxScore() != null ? response.maxScore().floatValue() : 0;
        long totalValues = response.hits().total() != null ? response.hits().total().value() : 0;

        List<T> results = response.hits().hits().stream()
            .filter(it -> it.source() != null)
            .map(Hit::source)
            .collect(Collectors.toList());

        Aggregations aggregations = aggregationSearchResultMapper.mapInternalAggregations(response.aggregations());

        return new AggregatedPageImpl<T>(results, pageable, totalValues, aggregations, scrollId, maxScore);
    }
}
