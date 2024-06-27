package com.icthh.xm.ms.entity.service.search;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.service.search.aggregation.Aggregations;
import com.icthh.xm.ms.entity.service.search.aggregation.terms.StringTerms;
import com.icthh.xm.ms.entity.service.search.builder.InnerHitBuilder;
import com.icthh.xm.ms.entity.service.search.builder.NativeSearchQueryBuilder;
import com.icthh.xm.ms.entity.service.search.builder.NestedQueryBuilder;
import com.icthh.xm.ms.entity.service.search.builder.QueryBuilder;
import com.icthh.xm.ms.entity.service.search.builder.QueryBuilders;
import com.icthh.xm.ms.entity.service.search.builder.aggregation.AggregationBuilders;
import com.icthh.xm.ms.entity.service.search.builder.aggregation.SearchRequestAggregationBuilder;
import com.icthh.xm.ms.entity.service.search.builder.aggregation.TermsAggregationBuilder;
import com.icthh.xm.ms.entity.service.search.enums.ScoreMode;
import com.icthh.xm.ms.entity.service.search.query.SearchQuery;
import com.icthh.xm.ms.entity.service.search.query.dto.NativeSearchQuery;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

public class AggregationSearchMapperUnitTest {

    private SearchRequestAggregationBuilder searchRequestAggregationBuilder;
    private ElasticsearchTemplateWrapper elasticTemplate; //TODO init

    @Test
    void testAggregationQuery(){
        SearchQuery searchQuery = new NativeSearchQueryBuilder()
            .addAggregation(
                AggregationBuilders.terms("TERM").field("TERM_FIELD").size(10)
                    .subAggregation(AggregationBuilders.stats("WORK_STATS").field("WORK_STATS_FIELD"))
                    .subAggregation(AggregationBuilders.stats("TOTAL_STATS").field("TOTAL_STATS_FIELD"))
            )
            .build();

        Aggregations aggregations = elasticTemplate.queryForPage(searchQuery, XmEntity.class)
            .getAggregations();

        StringTerms terms = aggregations.get("TERM");
        List<StringTerms.Bucket> buckets = terms.getBuckets();
//        for (final def bucket in buckets) {
//            result.put(bucket.keyAsString, getData(bucket));
//        }

        QueryBuilder queryBuilder = QueryBuilders.queryStringQuery("query");
        NativeSearchQuery searchQuery2 = new NativeSearchQuery(queryBuilder);
//        for (final String field in fields) {
            searchQuery2.addAggregation(AggregationBuilders.stats("field").field("field"));
//        }
        searchQuery2.setPageable(Pageable.unpaged());
    }

    @Test
    void testSearchQuery() {
        QueryBuilder queryBuilder = QueryBuilders.queryStringQuery("query");
        NestedQueryBuilder nestedQueryBuilder = QueryBuilders.nestedQuery("nestedQuery.path", queryBuilder, ScoreMode.None);
        nestedQueryBuilder.innerHit(new InnerHitBuilder());
    }

    @Test
    void testTermsSearchQuery() {
        String fieldName = "stateKey";
        String value = "ACTIVE";
        TermsAggregationBuilder termsAggregationBuilder = AggregationBuilders.terms(fieldName).field(value);

        Map<String, Aggregation> aggregationMap = searchRequestAggregationBuilder.buildAggregationMap(List.of(termsAggregationBuilder));

        assertThat(aggregationMap).isNotEmpty();
        assertThat(aggregationMap).containsKey(fieldName);

        Aggregation aggregation = aggregationMap.get(fieldName);

        assertThat(aggregation).isNotNull();
        assertThat(aggregation).isNotNull();
    }
}
