package com.icthh.xm.ms.entity.service.search.mapper;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.icthh.xm.ms.entity.service.search.page.aggregation.AggregatedPage;
import com.icthh.xm.ms.entity.service.search.page.aggregation.impl.AggregatedPageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class SearchResultMapper {

    public <T> AggregatedPage<T> mapSearchResults(SearchResponse<T> response, Pageable pageable) {
        // TODO: map aggregate
        Map<String, Aggregate> aggregations = response.aggregations();

        String scrollId = response.scrollId();
        float maxScore = response.maxScore() != null ? response.maxScore().floatValue() : 0;
        long totalValues = response.hits().total() != null ? response.hits().total().value() : 0;

        List<T> results = mapListSearchResults(response);

        return new AggregatedPageImpl<T>(results, pageable, totalValues, null, scrollId, maxScore);
    }

    public <T> List<T> mapListSearchResults(SearchResponse<T> response) {
        return response.hits().hits().stream()
            .filter(it -> it.source() != null)
            .map(Hit::source)
            .collect(Collectors.toList());
    }
}
