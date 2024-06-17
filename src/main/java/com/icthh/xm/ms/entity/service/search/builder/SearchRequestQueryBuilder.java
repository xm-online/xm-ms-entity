package com.icthh.xm.ms.entity.service.search.builder;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.icthh.xm.ms.entity.service.search.mapper.QueryTypeBuilderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SearchRequestQueryBuilder {

    private final QueryTypeBuilderMapper queryTypeBuilderMapper;

    public Query buildQuery(QueryBuilder queryBuilder) {
        if (queryBuilder instanceof BoolQueryBuilder) {
            BoolQueryBuilder boolQueryBuilder = (BoolQueryBuilder) queryBuilder;
            BoolQuery boolQuery = createBoolQuery(boolQueryBuilder);
            return new Query(boolQuery);
        }
        return buildRelatedQuery(queryBuilder);
    }

    public Query buildRelatedQuery(QueryBuilder queryBuilder) {
        if (queryBuilder instanceof QueryStringQueryBuilder) {
            QueryStringQueryBuilder queryStringQueryBuilder = (QueryStringQueryBuilder) queryBuilder;
            return new Query(queryTypeBuilderMapper.toSimpleQueryStringQueryBuilder(queryStringQueryBuilder).build());
        } else if (queryBuilder instanceof CommonTermsQueryBuilder) {
            CommonTermsQueryBuilder commonTermsQueryBuilder = (CommonTermsQueryBuilder) queryBuilder;
            return new Query(queryTypeBuilderMapper.toCommonTermsQueryBuilder(commonTermsQueryBuilder).build());
        }
        return null;
    }

    private BoolQuery createBoolQuery(BoolQueryBuilder boolQueryBuilder) {
        List<Query> mustClauses = buildRelatedQuery(boolQueryBuilder.must());
        return new BoolQuery.Builder()
            .must(mustClauses)
            .build();
    }

    private List<Query> buildRelatedQuery(List<QueryBuilder> queryBuilders) {
        return queryBuilders.stream()
            .map(this::buildRelatedQuery)
            .collect(Collectors.toList());
    }
}
