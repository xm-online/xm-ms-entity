package com.icthh.xm.ms.entity.service.search.builder;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchAllQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.NestedQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.PrefixQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.SimpleQueryStringQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQueryField;
import co.elastic.clients.elasticsearch.core.search.InnerHits;
import com.icthh.xm.ms.entity.service.search.mapper.QueryTypeBuilderMapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SearchRequestQueryBuilder {

    private final QueryTypeBuilderMapper queryTypeBuilderMapper;

    private final Map<Class<?>, Function<Object, FieldValue>> typeConverter = initTypeConverter();

    private final Map<Class<?>, Function<QueryBuilder, Query>> queryConverter = initQueryConverter();

    private Map<Class<?>, Function<Object, FieldValue>> initTypeConverter() {
        Map<Class<?>, Function<Object, FieldValue>> typeConverter = new HashMap<>();

        typeConverter.put(Long.class, value -> FieldValue.of(Long.parseLong(String.valueOf(value))));
        typeConverter.put(Integer.class, value -> FieldValue.of(Long.parseLong(String.valueOf(value))));
        typeConverter.put(Double.class, value -> FieldValue.of((double) value));
        typeConverter.put(Boolean.class, value -> FieldValue.of((boolean) value));
        typeConverter.put(String.class, value -> FieldValue.of((String) value));

        return typeConverter;
    }

    private Map<Class<?>, Function<QueryBuilder, Query>> initQueryConverter() {
        Map<Class<?>, Function<QueryBuilder, Query>> queryConverter = new HashMap<>();

        queryConverter.put(QueryStringQueryBuilder.class, this::buildQueryStringQuery);
        queryConverter.put(CommonTermsQueryBuilder.class, this::buildCommonTermsQuery);
        queryConverter.put(NestedQueryBuilder.class, this::buildNestedQuery);
        queryConverter.put(MatchQueryBuilder.class, this::buildMatchQuery);
        queryConverter.put(TermQueryBuilder.class, this::buildTermQuery);
        queryConverter.put(BoolQueryBuilder.class, this::buildBoolQuery);
        queryConverter.put(SimpleQueryStringBuilder.class, this::buildSimpleQueryStringQuery);
        queryConverter.put(MatchAllQueryBuilder.class, this::buildMatchAllQuery);
        queryConverter.put(PrefixQueryBuilder.class, this::buildPrefixQuery);
        queryConverter.put(TermsQueryBuilder.class, this::buildTermsQuery);

        return queryConverter;
    }

    public Query buildQuery(QueryBuilder queryBuilder) {
        Function<QueryBuilder, Query> function = queryConverter.get(queryBuilder.getClass());
        if (function != null) {
            return function.apply(queryBuilder);
        } else {
            return null;
        }
    }

    private Query buildQueryStringQuery(QueryBuilder queryBuilder) {
        QueryStringQueryBuilder queryStringQueryBuilder = (QueryStringQueryBuilder) queryBuilder;
        return new Query(queryTypeBuilderMapper.toQueryStringQueryBuilder(queryStringQueryBuilder).build());
    }

    private Query buildCommonTermsQuery(QueryBuilder queryBuilder) {
        CommonTermsQueryBuilder commonTermsQueryBuilder = (CommonTermsQueryBuilder) queryBuilder;
        return new Query(queryTypeBuilderMapper.toCommonTermsQueryBuilder(commonTermsQueryBuilder).build());
    }

    private Query buildNestedQuery(QueryBuilder queryBuilder) {
        NestedQueryBuilder nestedQueryBuilder = (NestedQueryBuilder) queryBuilder;
        return toNestedQuery(nestedQueryBuilder);
    }

    private Query buildMatchQuery(QueryBuilder queryBuilder) {
        MatchQueryBuilder matchQueryBuilder = (MatchQueryBuilder) queryBuilder;
        FieldValue fieldValue = toFieldValue(matchQueryBuilder.getValue());
        MatchQuery matchQuery = queryTypeBuilderMapper.toMatchQueryBuilder(matchQueryBuilder).query(fieldValue).build();
        return new Query(matchQuery);
    }

    private Query buildTermQuery(QueryBuilder queryBuilder) {
        TermQueryBuilder termQueryBuilder = (TermQueryBuilder) queryBuilder;
        FieldValue fieldValue = toFieldValue(termQueryBuilder.getValue());
        TermQuery termQuery = queryTypeBuilderMapper.toTermQueryBuilder(termQueryBuilder).value(fieldValue).build();
        return new Query(termQuery);
    }

    private Query buildBoolQuery(QueryBuilder queryBuilder) {
        BoolQueryBuilder boolQueryBuilder = (BoolQueryBuilder) queryBuilder;
        return toBoolQuery(boolQueryBuilder);
    }

    private Query buildSimpleQueryStringQuery(QueryBuilder queryBuilder) {
        SimpleQueryStringBuilder simpleQueryStringBuilder = (SimpleQueryStringBuilder) queryBuilder;
        List<String> fields = new ArrayList<>(simpleQueryStringBuilder.fields().keySet());

        SimpleQueryStringQuery.Builder simpleQueryStringQueryBuilder = queryTypeBuilderMapper.toSimpleQueryStringQueryBuilder(simpleQueryStringBuilder);

        if (CollectionUtils.isNotEmpty(fields)) {
            simpleQueryStringQueryBuilder.fields(fields);
        }

        return new Query(simpleQueryStringQueryBuilder.build());
    }

    private Query buildMatchAllQuery(QueryBuilder queryBuilder) {
        return new Query(new MatchAllQuery.Builder().build());
    }

    private Query buildPrefixQuery(QueryBuilder queryBuilder) {
        PrefixQueryBuilder prefixQueryBuilder = (PrefixQueryBuilder) queryBuilder;
        PrefixQuery prefixQuery = queryTypeBuilderMapper.toPrefixQueryBuilder(prefixQueryBuilder).build();
        return new Query(prefixQuery);
    }

    private Query buildTermsQuery(QueryBuilder queryBuilder) {
        TermsQueryBuilder termsQueryBuilder = (TermsQueryBuilder) queryBuilder;
        List<FieldValue> fieldValueList = termsQueryBuilder.values()
            .stream()
            .map(this::toFieldValue)
            .collect(Collectors.toList());

        TermsQuery.Builder termsQuery = queryTypeBuilderMapper.toTermsQueryBuilder(termsQueryBuilder);
        TermsQueryField termsQueryField = new TermsQueryField.Builder().value(fieldValueList).build();
        termsQuery.terms(termsQueryField);
        return new Query(termsQuery.build());
    }

    private Query toNestedQuery(NestedQueryBuilder nestedQueryBuilder) {
        NestedQuery.Builder nestedQuery = queryTypeBuilderMapper.toNestedQueryBuilder(nestedQueryBuilder);
        nestedQuery.query(buildQuery(nestedQueryBuilder.getQuery()));
        nestedQuery.innerHits(toInnerHits(nestedQueryBuilder.innerHit()));

        return Query.of(query -> query.nested(nestedQuery.build()));
    }

    private Query toBoolQuery(BoolQueryBuilder boolQueryBuilder) {
        BoolQuery.Builder boolQuery = new BoolQuery.Builder();

        boolQueryBuilder.must().forEach(queryBuilder -> boolQuery.must(buildQuery(queryBuilder)));
        boolQueryBuilder.should().forEach(queryBuilder -> boolQuery.should(buildQuery(queryBuilder)));
        boolQueryBuilder.mustNot().forEach(queryBuilder -> boolQuery.mustNot(buildQuery(queryBuilder)));
        boolQueryBuilder.filter().forEach(queryBuilder -> boolQuery.filter(buildQuery(queryBuilder)));
        boolQuery.minimumShouldMatch(boolQueryBuilder.minimumShouldMatch());

        return Query.of(query -> query.bool(boolQuery.build()));
    }

    private FieldValue toFieldValue(Object value) {
        return typeConverter.getOrDefault(value.getClass(), FieldValue::of).apply(value);
    }

    private InnerHits toInnerHits(InnerHitBuilder innerHitBuilder) {
        if (innerHitBuilder == null) {
            return null;
        }
        return queryTypeBuilderMapper.toInnerHitBuilder(innerHitBuilder).build();
    }

}
