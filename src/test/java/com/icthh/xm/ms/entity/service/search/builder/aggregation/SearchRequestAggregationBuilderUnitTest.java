package com.icthh.xm.ms.entity.service.search.builder.aggregation;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.MaxAggregation;
import co.elastic.clients.elasticsearch._types.aggregations.StatsAggregation;
import co.elastic.clients.elasticsearch._types.aggregations.TermsAggregation;
import com.icthh.xm.ms.entity.AbstractUnitTest;
import com.icthh.xm.ms.entity.service.search.mapper.AggregationTypeBuilderMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mapstruct.factory.Mappers;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class SearchRequestAggregationBuilderUnitTest extends AbstractUnitTest {

    private AggregationTypeBuilderMapper aggregationTypeBuilderMapper = Mappers.getMapper(AggregationTypeBuilderMapper.class);

    private SearchRequestAggregationBuilder subject;

    @Before
    public void setUp() {
        subject =  new SearchRequestAggregationBuilder(aggregationTypeBuilderMapper);
    }

    @Test
    public void buildAggregationMapShouldBuildTermsAggregation() {
        String fieldName = "stateKey";
        String value = "ACTIVE";
        int size = 10;

        TermsAggregationBuilder termsAggregationBuilder = AggregationBuilders.terms(fieldName).field(value).size(size);

        Map<String, Aggregation> aggregationMap = subject.buildAggregationMap(List.of(termsAggregationBuilder));

        assertThat(aggregationMap).isNotEmpty();
        assertThat(aggregationMap).containsKey(fieldName);

        Aggregation aggregation = aggregationMap.get(fieldName);

        assertThat(aggregation).isNotNull();
        assertThat(aggregation._kind()).isEqualTo(Aggregation.Kind.Terms);
        assertThat(aggregation.aggregations()).isEmpty();

        TermsAggregation actualTerms = aggregation.terms();

        assertTermsAggregation(actualTerms, termsAggregationBuilder);
    }

    @Test
    public void buildAggregationMapShouldBuildTermsAggregationWithSubAggregations() {
        String fieldName = "state_field";
        String field = "stateKey";
        String subAggregationFieldName = "name_field";
        String subAggregationField = "name";
        int size = 10;

        TermsAggregationBuilder subAggregationTermsBuilder = AggregationBuilders.terms(subAggregationFieldName).field(subAggregationField);
        TermsAggregationBuilder termsAggregationBuilder = AggregationBuilders.terms(fieldName).field(field).size(size)
            .subAggregation(subAggregationTermsBuilder);

        Map<String, Aggregation> aggregationMap = subject.buildAggregationMap(List.of(termsAggregationBuilder));

        assertThat(aggregationMap).isNotEmpty();
        assertThat(aggregationMap).containsKey(fieldName);

        Aggregation aggregation = aggregationMap.get(fieldName);

        assertThat(aggregation).isNotNull();
        assertThat(aggregation._kind()).isEqualTo(Aggregation.Kind.Terms);
        assertThat(aggregation.aggregations()).containsKey(subAggregationFieldName);

        TermsAggregation actualTerms = aggregation.terms();
        assertTermsAggregation(actualTerms, termsAggregationBuilder);

        TermsAggregation actualTermsSubAggregation = aggregation.aggregations().get(subAggregationFieldName).terms();
        assertTermsAggregation(actualTermsSubAggregation, subAggregationTermsBuilder);
    }

    @Test
    public void buildAggregationMapShouldBuildStatsAggregation() {
        String fieldName = "id_field";
        String value = "id";

        StatsAggregationBuilder statsAggregationBuilder = AggregationBuilders.stats(fieldName).field(value);

        Map<String, Aggregation> aggregationMap = subject.buildAggregationMap(List.of(statsAggregationBuilder));

        assertThat(aggregationMap).isNotEmpty();
        assertThat(aggregationMap).containsKey(fieldName);

        Aggregation aggregation = aggregationMap.get(fieldName);

        assertThat(aggregation).isNotNull();
        assertThat(aggregation._kind()).isEqualTo(Aggregation.Kind.Stats);
        assertThat(aggregation.aggregations()).isEmpty();

        StatsAggregation actualStats = aggregation.stats();

        assertThat(actualStats.field()).isEqualTo(statsAggregationBuilder.field());
        assertThat(actualStats.format()).isNull();
        assertThat(actualStats.script()).isNull();
        assertThat(actualStats.missing()).isNull();
    }

    @Test
    public void buildAggregationMapShouldBuildMaxAggregation() {
        String fieldName = "age_field";
        String value = "age";

        MaxAggregationBuilder maxAggregationBuilder = AggregationBuilders.max(fieldName).field(value);

        Map<String, Aggregation> aggregationMap = subject.buildAggregationMap(List.of(maxAggregationBuilder));

        assertThat(aggregationMap).isNotEmpty();
        assertThat(aggregationMap).containsKey(fieldName);

        Aggregation aggregation = aggregationMap.get(fieldName);

        assertThat(aggregation).isNotNull();
        assertThat(aggregation._kind()).isEqualTo(Aggregation.Kind.Max);
        assertThat(aggregation.aggregations()).isEmpty();

        MaxAggregation actualMax = aggregation.max();

        assertThat(actualMax.field()).isEqualTo(maxAggregationBuilder.field());
        assertThat(actualMax.format()).isNull();
        assertThat(actualMax.script()).isNull();
        assertThat(actualMax.missing()).isNull();
    }

    private static void assertTermsAggregation(TermsAggregation actualTerms, TermsAggregationBuilder expected) {
        assertThat(actualTerms.field()).isEqualTo(expected.field());
        assertThat(actualTerms.minDocCount()).isEqualTo(Long.valueOf(expected.minDocCount()).intValue());
        assertThat(actualTerms.size()).isEqualTo(expected.size());
        assertThat(actualTerms.showTermDocCountError()).isEqualTo(expected.showTermDocCountError());
        assertThat(actualTerms.valueType()).isNull();
        assertThat(actualTerms.shardSize()).isNull();
        assertThat(actualTerms.order()).isEmpty();
        assertThat(actualTerms.format()).isNull();
        assertThat(actualTerms.script()).isNull();
        assertThat(actualTerms.missing()).isNull();
        assertThat(actualTerms.missingOrder()).isNull();
        assertThat(actualTerms.missingBucket()).isNull();
        assertThat(actualTerms.collectMode()).isNull();
        assertThat(actualTerms.executionHint()).isNull();
        assertThat(actualTerms.exclude()).isNull();
        assertThat(actualTerms.include()).isNull();
    }
}
