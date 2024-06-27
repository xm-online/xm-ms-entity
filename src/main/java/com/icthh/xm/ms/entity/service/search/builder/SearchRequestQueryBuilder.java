package com.icthh.xm.ms.entity.service.search.builder;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.NestedQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import com.icthh.xm.ms.entity.service.search.mapper.QueryTypeBuilderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SearchRequestQueryBuilder {

    private final QueryTypeBuilderMapper queryTypeBuilderMapper;

    public Query buildQuery(QueryBuilder queryBuilder) {
        if (queryBuilder instanceof QueryStringQueryBuilder) {
            QueryStringQueryBuilder queryStringQueryBuilder = (QueryStringQueryBuilder) queryBuilder;
            return new Query(queryTypeBuilderMapper.toSimpleQueryStringQueryBuilder(queryStringQueryBuilder).build());
        } else if (queryBuilder instanceof CommonTermsQueryBuilder) {
            CommonTermsQueryBuilder commonTermsQueryBuilder = (CommonTermsQueryBuilder) queryBuilder;
            return new Query(queryTypeBuilderMapper.toCommonTermsQueryBuilder(commonTermsQueryBuilder).build());
        } else if (queryBuilder instanceof NestedQueryBuilder) {
            NestedQueryBuilder nestedQueryBuilder = (NestedQueryBuilder) queryBuilder;
            NestedQuery nestedQuery = queryTypeBuilderMapper.toNestedQueryBuilder(nestedQueryBuilder).build();
            return new Query(nestedQuery);
        } else if (queryBuilder instanceof MatchQueryBuilder) {
            MatchQueryBuilder matchQueryBuilder = (MatchQueryBuilder) queryBuilder;
            FieldValue fieldValue = toFieldValue(matchQueryBuilder.getValue());
            MatchQuery matchQuery = queryTypeBuilderMapper.toMatchQueryBuilder(matchQueryBuilder).query(fieldValue).build();
            return new Query(matchQuery);
        } else if (queryBuilder instanceof TermQueryBuilder) {
            TermQueryBuilder termQueryBuilder = (TermQueryBuilder) queryBuilder;
            FieldValue fieldValue = toFieldValue(termQueryBuilder.getValue());
            TermQuery termQuery = queryTypeBuilderMapper.toTermQueryBuilder(termQueryBuilder).value(fieldValue).build();
            return new Query(termQuery);
        } else if (queryBuilder instanceof BoolQueryBuilder) {
            BoolQueryBuilder boolQueryBuilder = (BoolQueryBuilder) queryBuilder;
            return toBoolQuery(boolQueryBuilder);
        }
        return null;
    }

    private Query toBoolQuery(BoolQueryBuilder boolQueryBuilder) {
        BoolQuery.Builder boolQuery = new BoolQuery.Builder();

        boolQueryBuilder.must().forEach(queryBuilder -> boolQuery.must(buildQuery(queryBuilder)));
        boolQueryBuilder.should().forEach(queryBuilder -> boolQuery.should(buildQuery(queryBuilder)));
        boolQueryBuilder.mustNot().forEach(queryBuilder -> boolQuery.mustNot(buildQuery(queryBuilder)));
        boolQueryBuilder.filter().forEach(queryBuilder -> boolQuery.filter(buildQuery(queryBuilder)));

        return Query.of(query -> query.bool(boolQuery.build()));
    }

    private FieldValue toFieldValue(Object value) {
        if (value instanceof Long || value instanceof Integer) {
            long fieldValue = Long.parseLong(String.valueOf(value));
            return FieldValue.of(fieldValue);
        } else if (value instanceof Double) {
            return FieldValue.of((double) value);
        } else if (value instanceof Boolean) {
            return FieldValue.of((boolean) value);
        } else if (value instanceof String) {
            return FieldValue.of((String) value);
        } else {
            return FieldValue.of(value);
        }
    }

}
