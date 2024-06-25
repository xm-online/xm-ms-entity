package com.icthh.xm.ms.entity.service.search.builder.aggregation;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.StatsAggregation;
import co.elastic.clients.elasticsearch._types.aggregations.TermsAggregation;
import com.icthh.xm.ms.entity.service.search.mapper.AggregationTypeBuilderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SearchRequestAggregationBuilder {

    private final AggregationTypeBuilderMapper aggregationTypeBuilderMapper;

    public Map<String, Aggregation> buildAggregationMap(List<AbstractAggregationBuilder> aggregations) {
        return aggregations.stream()
            .collect(Collectors.toMap(AbstractAggregationBuilder::getName, this::buildAggregation));
    }

    private Aggregation buildAggregation(AbstractAggregationBuilder aggregationBuilder) {
        Aggregation.Builder newAggregationBuilder = new Aggregation.Builder();

        if (aggregationBuilder instanceof TermsAggregationBuilder) {
            TermsAggregationBuilder termsAggregationBuilder = (TermsAggregationBuilder) aggregationBuilder;
            TermsAggregation termsAggregation = aggregationTypeBuilderMapper.toTermsAggregationBuilder(termsAggregationBuilder).build();
            newAggregationBuilder.terms(termsAggregation);
        } else if (aggregationBuilder instanceof StatsAggregationBuilder) {
            StatsAggregationBuilder statsAggregationBuilder = (StatsAggregationBuilder) aggregationBuilder;
            StatsAggregation statsAggregation = aggregationTypeBuilderMapper.toStatsAggregationBuilder(statsAggregationBuilder).build();
            newAggregationBuilder.stats(statsAggregation);
        }

        addSubAggregations(newAggregationBuilder, aggregationBuilder.getSubAggregations());

        return newAggregationBuilder.build();
    }

    private void addSubAggregations(Aggregation.Builder parentAggregationBuilder, List<AggregationBuilder> subAggregations) {
        subAggregations.forEach(subAggregation -> {
            AbstractAggregationBuilder abstractSubAggregationBuilder = (AbstractAggregationBuilder) subAggregation;
            Aggregation newSubAggregation = buildAggregation(abstractSubAggregationBuilder);
            parentAggregationBuilder.aggregations(abstractSubAggregationBuilder.getName(), newSubAggregation);
        });
    }
}
