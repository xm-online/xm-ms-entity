package com.icthh.xm.ms.entity.service.search.mapper;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.MaxAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StatsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import com.icthh.xm.ms.entity.AbstractUnitTest;
import com.icthh.xm.ms.entity.service.search.aggregation.internal.InternalAggregations;
import com.icthh.xm.ms.entity.service.search.aggregation.stats.Max;
import com.icthh.xm.ms.entity.service.search.aggregation.stats.Stats;
import com.icthh.xm.ms.entity.service.search.aggregation.terms.StringTerms;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class AggregationSearchResultMapperUnitTest extends AbstractUnitTest {

    private AggregationSearchResultMapper subject;

    @Before
    public void setUp() {
        subject = new AggregationSearchResultMapper();
    }

    @Test
    public void mapInternalAggregationsShouldReturnEmptyAggregations() {
        Map<String, Aggregate> emptyAggregations = Collections.emptyMap();

        InternalAggregations result = subject.mapInternalAggregations(emptyAggregations);

        assertThat(result.asList()).isEmpty();
    }

    @Test
    public void mapInternalAggregationsShouldReturnStringTermsAggregation() {
        String name = "terms_agg";
        StringTermsBucket bucket = new StringTermsBucket.Builder().key("key").docCount(10).build();
        StringTermsAggregate stringTermsAggregate = new StringTermsAggregate.Builder()
            .buckets(t -> t.array(List.of(bucket)))
            .build();
        Aggregate aggregate = new Aggregate.Builder().sterms(stringTermsAggregate).build();

        Map<String, Aggregate> aggregations = Map.of(name, aggregate);

        InternalAggregations actual = subject.mapInternalAggregations(aggregations);


        assertThat(actual.asMap()).hasSize(1);

        StringTerms actualStringTerms = actual.get(name);

        assertThat(actualStringTerms.getName()).isEqualTo(name);
        assertThat(actualStringTerms.getBuckets()).hasSize(1);

        StringTerms.Bucket actualBucket = actualStringTerms.getBuckets().get(0);

        assertThat(actualBucket.getKey()).isEqualTo(bucket.key().stringValue());
        assertThat(actualBucket.getDocCount()).isEqualTo(bucket.docCount());
    }

    @Test
    public void mapInternalAggregationsShouldReturnStatsAggregation() {
        String name = "stats_agg";
        StatsAggregate statsAggregate = new StatsAggregate.Builder().count(100).sum(200.0).min(1.0).max(50.0).avg(10.0).build();
        Aggregate aggregate = new Aggregate.Builder().stats(statsAggregate).build();

        Map<String, Aggregate> aggregations = Map.of(name, aggregate);

        InternalAggregations actual = subject.mapInternalAggregations(aggregations);

        assertThat(actual.asMap()).hasSize(1);

        Stats actualStats = actual.get(name);

        assertThat(actualStats.getName()).isEqualTo(name);
        assertThat(actualStats.getCount()).isEqualTo(statsAggregate.count());
        assertThat(actualStats.getSum()).isEqualTo(statsAggregate.sum());
        assertThat(actualStats.getMin()).isEqualTo(statsAggregate.min());
        assertThat(actualStats.getMax()).isEqualTo(statsAggregate.max());
    }

    @Test
    public void mapInternalAggregationsShouldReturnMaxAggregation() {
        String name = "max_agg";
        MaxAggregate maxAggregate = new MaxAggregate.Builder().value(123.45).build();
        Aggregate aggregate = new Aggregate.Builder().max(maxAggregate).build();

        Map<String, Aggregate> aggregations = Map.of(name, aggregate);

        InternalAggregations actual = subject.mapInternalAggregations(aggregations);

        assertThat(actual.asMap()).hasSize(1);

        Max actualMax = actual.get(name);

        assertThat(actualMax.getName()).isEqualTo(name);
        assertThat(actualMax.getValue()).isEqualTo(maxAggregate.value());
    }
}
