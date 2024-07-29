package com.icthh.xm.ms.entity.service.search.mapper;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch.core.ScrollResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.core.search.InnerHitsResult;
import co.elastic.clients.json.JsonData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.ms.entity.service.search.aggregation.Aggregations;
import com.icthh.xm.ms.entity.service.search.dto.response.SearchHit;
import com.icthh.xm.ms.entity.service.search.dto.response.SearchHits;
import com.icthh.xm.ms.entity.service.search.page.aggregation.AggregatedPage;
import com.icthh.xm.ms.entity.service.search.page.aggregation.impl.AggregatedPageImpl;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SearchResultMapper {

    private final AggregationSearchResultMapper aggregationSearchResultMapper;
    private final ObjectMapper objectMapper;

    public <T> AggregatedPage<T> mapSearchResults(SearchResponse<T> response, Pageable pageable) {
        return mapResults(response.scrollId(), response.maxScore(), response.hits(), response.aggregations(), pageable);
    }

    public <T> AggregatedPage<T> mapScrollSearchResults(ScrollResponse<T> response, Pageable pageable) {
        return mapResults(response.scrollId(), response.maxScore(), response.hits(), response.aggregations(), pageable);
    }

    public <T> List<T> mapListSearchResults(HitsMetadata<T> hitsMetadata) {
        return hitsMetadata.hits().stream()
            .filter(it -> it.source() != null)
            .map(Hit::source)
            .collect(Collectors.toList());
    }

    public <T> List<String> mapIdsSearchResults(SearchResponse<T> response) {
        return response.hits().hits().stream()
            .filter(it -> it.source() != null)
            .map(Hit::id)
            .collect(Collectors.toList());
    }

    public <T> com.icthh.xm.ms.entity.service.search.dto.response.SearchResponse mapSearchResponse(SearchResponse<T> response) {
        SearchHits hits = mapSearchHits(response.hits().hits());
        return new com.icthh.xm.ms.entity.service.search.dto.response.SearchResponse(hits);
    }

    private <T> AggregatedPage<T> mapResults(String scrollId, Double score, HitsMetadata<T> hitsMetadata,
                                             Map<String, Aggregate> aggregationMap, Pageable pageable) {
        float maxScore = score != null ? score.floatValue() : 0;
        long totalValues = hitsMetadata.total() != null ? hitsMetadata.total().value() : 0;

        List<T> results = mapListSearchResults(hitsMetadata);

        Aggregations aggregations = aggregationSearchResultMapper.mapInternalAggregations(aggregationMap);

        return new AggregatedPageImpl<T>(results, pageable, totalValues, aggregations, scrollId, maxScore);
    }

    private <T> SearchHits mapSearchHits(List<Hit<T>> hits) {
        if (CollectionUtils.isEmpty(hits)) {
            return SearchHits.empty();
        }

        SearchHit[] array = hits.stream()
            .filter(it -> it.source() != null)
            .map(this::mapHitSource)
            .toArray(SearchHit[]::new);

        return new SearchHits(array);
    }

    private <T> SearchHit mapHitSource(Hit<T> hit) {
        String hitSource = convertHitSourceToString(hit);
        Map<String, SearchHits> innerHits = mapInnerHits(hit.innerHits());

        return new SearchHit(hitSource, innerHits);
    }

    private Map<String, SearchHits> mapInnerHits(Map<String, InnerHitsResult> innerHitsResult) {
        return innerHitsResult.entrySet()
            .stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> mapSearchHits(entry.getValue().hits().hits())
            ));
    }

    private <T> String convertHitSourceToString(Hit<T> hit) {
        T source = hit.source();

        if (source instanceof JsonData) {
            return ((JsonData) source).toJson().toString();
        }

        try {
            return objectMapper.writeValueAsString(source);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Exception while converting hit source to string", e);
        }
    }
}
