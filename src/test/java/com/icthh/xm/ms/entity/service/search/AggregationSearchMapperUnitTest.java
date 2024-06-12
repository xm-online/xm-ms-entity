package com.icthh.xm.ms.entity.service.search;

import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.service.search.aggregation.Aggregations;
import com.icthh.xm.ms.entity.service.search.aggregation.terms.StringTerms;
import com.icthh.xm.ms.entity.service.search.aggregation.terms.Terms;
import com.icthh.xm.ms.entity.service.search.builder.NativeSearchQueryBuilder;
import com.icthh.xm.ms.entity.service.search.builder.QueryBuilder;
import com.icthh.xm.ms.entity.service.search.builder.QueryBuilders;
import com.icthh.xm.ms.entity.service.search.builder.aggregation.AggregationBuilders;
import com.icthh.xm.ms.entity.service.search.query.SearchQuery;
import com.icthh.xm.ms.entity.service.search.query.dto.NativeSearchQuery;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import java.util.List;

public class AggregationSearchMapperUnitTest {

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
}
