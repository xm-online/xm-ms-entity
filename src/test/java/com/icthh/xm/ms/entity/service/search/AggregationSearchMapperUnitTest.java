package com.icthh.xm.ms.entity.service.search;

import com.icthh.xm.ms.entity.domain.XmEntity;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;

import java.util.List;

public class AggregationSearchMapperUnitTest {

    private ElasticsearchTemplateWrapper wrapper; //TODO init

    @Test
    void testAggregationQuery(){
        SearchQuery searchQuery = new NativeSearchQueryBuilder()

            .addAggregation(
                AggregationBuilders.terms(TERM).field(TERM_FIELD).size(MAX_SIZE)
                    .subAggregation(AggregationBuilders.stats(WORK_STATS).field(WORK_STATS_FIELD))
                    .subAggregation(AggregationBuilders.stats(TOTAL_STATS).field(TOTAL_STATS_FIELD))
            )
            .build()

        Aggregations aggregations = elasticTemplate.queryForPage(searchQuery, XmEntity.class)
            .getAggregations()

        StringTerms terms = aggregations.get(TERM)
        List<Terms.Bucket> buckets = terms.getBuckets()
        for (final def bucket in buckets) {
            result.put(bucket.keyAsString, getData(bucket))
        }

        QueryBuilder queryBuilder = QueryBuilders.queryStringQuery(query)
        SearchQuery searchQuery = new NativeSearchQuery(queryBuilder)
        for (final String field in fields) {
            searchQuery.addAggregation(AggregationBuilders.stats(field).field(field))
        }
        searchQuery.setPageable(Pageable.unpaged())
    }
}
