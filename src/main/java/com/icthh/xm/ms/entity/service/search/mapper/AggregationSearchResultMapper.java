package com.icthh.xm.ms.entity.service.search.mapper;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.MaxAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StatsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsAggregate;
import com.icthh.xm.ms.entity.service.search.aggregation.internal.InternalAggregation;
import com.icthh.xm.ms.entity.service.search.aggregation.internal.InternalAggregations;
import com.icthh.xm.ms.entity.service.search.aggregation.internal.InternalMax;
import com.icthh.xm.ms.entity.service.search.aggregation.internal.InternalStats;
import com.icthh.xm.ms.entity.service.search.aggregation.terms.StringTerms;
import com.icthh.xm.ms.entity.service.search.builder.aggregation.support.DocValueFormat;
import org.apache.lucene.util.BytesRef;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class AggregationSearchResultMapper {

    private static final DocValueFormat DEFAULT_DOC_FORMAT = DocValueFormat.RAW;

    public InternalAggregations mapInternalAggregations(Map<String, Aggregate> aggregations) {
        if (aggregations.isEmpty()) {
            return new InternalAggregations(Collections.emptyList());
        }

        List<InternalAggregation> convertedAggregations = aggregations.entrySet()
            .stream()
            .map(entry -> convertAggregate(entry.getKey(), entry.getValue()))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        return new InternalAggregations(convertedAggregations);
    }

    public InternalAggregation convertAggregate(String name, Aggregate aggregate) {
        if (aggregate._kind() == Aggregate.Kind.Sterms) {
            return convertStringTermsAggregate(name, aggregate.sterms());
        } else if (aggregate._kind() == Aggregate.Kind.Stats) {
            return convertStatsAggregate(name, aggregate.stats());
        } else if (aggregate._kind() == Aggregate.Kind.Max) {
            return convertMaxAggregate(name, aggregate.max());
        }
        return null;
    }

    private InternalAggregation convertStringTermsAggregate(String name, StringTermsAggregate stringTermsAggregate) {
        List<StringTerms.Bucket> buckets = stringTermsAggregate.buckets()
            .array()
            .stream()
            .map(bucket -> {
                InternalAggregations subAggregations = mapInternalAggregations(bucket.aggregations());

                return new StringTerms.Bucket(
                    new BytesRef(bucket.key().stringValue()),
                    bucket.docCount(),
                    subAggregations,
                    false,
                    0, // todo: check mapping fields
                    DEFAULT_DOC_FORMAT
                );
            })
            .collect(Collectors.toList());

        return new StringTerms(name, buckets);
    }

    private InternalAggregation convertStatsAggregate(String name, StatsAggregate statsAggregate) {
        return new InternalStats(name, statsAggregate.count(), statsAggregate.sum(), statsAggregate.min(), statsAggregate.max(), DEFAULT_DOC_FORMAT);
    }

    private InternalAggregation convertMaxAggregate(String name, MaxAggregate maxAggregate) {
        return new InternalMax(name, maxAggregate.value(), DEFAULT_DOC_FORMAT);
    }
}
