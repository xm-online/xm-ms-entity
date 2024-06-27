package com.icthh.xm.ms.entity.service.search;

import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import com.icthh.xm.ms.entity.service.search.builder.NativeSearchQueryBuilder;
import com.icthh.xm.ms.entity.service.search.builder.QueryBuilders;
import com.icthh.xm.ms.entity.service.search.builder.SearchRequestQueryBuilder;
import com.icthh.xm.ms.entity.service.search.mapper.QueryTypeBuilderMapper;
import com.icthh.xm.ms.entity.service.search.query.dto.NativeSearchQuery;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class QueryForListSearchMapperUnitTest {

    private final SearchRequestQueryBuilder searchRequestQueryBuilder = new SearchRequestQueryBuilder(Mappers.getMapper(QueryTypeBuilderMapper.class));

    @Test
    void testQueryForList() {
        String query = "typeKey:ACCOUNT";
        NativeSearchQuery nativeSearchQuery = new NativeSearchQueryBuilder()
            .withQuery(QueryBuilders.queryStringQuery(query))
            .withPageable(new PageRequest(0, 10))
            .build();

        String actualQuery = searchRequestQueryBuilder.buildQuery(nativeSearchQuery.getQuery()).simpleQueryString().query();

        assertEquals(query, actualQuery);
    }

    @Test
    void testCommonTermsQuery() {
        String fieldName = "stateKey";
        String text = "ACTIVE";
        NativeSearchQuery nativeSearchQuery = new NativeSearchQueryBuilder()
            .withQuery(QueryBuilders.commonTermsQuery(fieldName, text))
            .withPageable(new PageRequest(0, 10))
            .build();

        Query query = searchRequestQueryBuilder.buildQuery(nativeSearchQuery.getQuery());
        String actualField = query.common().field();
        String actualQuery = query.common().query();

        assertEquals(fieldName, actualField);
        assertEquals(text, actualQuery);
    }

    @Test
    void testBoolQuery() {
        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.boolQuery()
                    .must(QueryBuilders.termQuery("typeKey", "ACCOUNT"))
                    .must(QueryBuilders.boolQuery()
                        .should(QueryBuilders.matchQuery("data.first", "abc"))
                        .should(QueryBuilders.matchQuery("data.second", "bca")))
                    .must(QueryBuilders.termQuery("data.templateId", 10)))
                .withPageable(PageRequest.of(0, 10))
                .build();

        List<Query> queryList = searchRequestQueryBuilder.buildQuery(searchQuery.getQuery()).bool().must();

        TermQuery typeKeyTermQuery = queryList.get(0).term();
        String typeKeyTermQueryField = typeKeyTermQuery.field();
        String typeKeyTermQueryValue = typeKeyTermQuery.value()._get().toString();
        String typeKeyTermQueryValueKind = typeKeyTermQuery.value()._kind().toString();

        assertEquals("typeKey", typeKeyTermQueryField);
        assertEquals("ACCOUNT", typeKeyTermQueryValue);
        assertEquals("String", typeKeyTermQueryValueKind);

        List<Query> secondSubqueries = queryList.get(1).bool().should();
        String firstSubqueryField = secondSubqueries.get(0).match().field();
        String firstSubqueryValue = secondSubqueries.get(0).match().query()._get().toString();
        String firstSubqueryValueKind = secondSubqueries.get(0).match().query()._kind().toString();
        String secondSubqueryField = secondSubqueries.get(1).match().field();
        String secondSubqueryValue = secondSubqueries.get(1).match().query()._get().toString();
        String secondSubqueryValueKind = secondSubqueries.get(1).match().query()._kind().toString();

        assertEquals("data.first", firstSubqueryField);
        assertEquals("abc", firstSubqueryValue);
        assertEquals("String", firstSubqueryValueKind);
        assertEquals("data.second", secondSubqueryField);
        assertEquals("bca", secondSubqueryValue);
        assertEquals("String", secondSubqueryValueKind);

        TermQuery templateIdTermQuery = queryList.get(2).term();
        String templateIdField = templateIdTermQuery.field();
        long templateIdValue = Long.valueOf(templateIdTermQuery.value()._get().toString());
        String templateIdValueKind = templateIdTermQuery.value()._kind().toString();

        assertEquals("data.templateId", templateIdField);
        assertEquals(10L, templateIdValue);
        assertEquals("Long", templateIdValueKind);
    }

    @Test
    void testTermQuery() {
        NativeSearchQuery nativeSearchQuery = new NativeSearchQueryBuilder()
            .withQuery(QueryBuilders.termQuery("data.size", 1.25)).build();

        TermQuery termQuery = searchRequestQueryBuilder.buildQuery(nativeSearchQuery.getQuery()).term();
        String termQueryField = termQuery.field();
        double termQueryValue = (double) termQuery.value()._get();
        String termQueryValueKind = termQuery.value()._kind().toString();

        assertEquals("data.size", termQueryField);
        assertEquals(1.25D, termQueryValue, 0D);
        assertEquals("Double", termQueryValueKind);
    }

    @Test
    void testMatchQuery() {
        NativeSearchQuery nativeSearchQuery = new NativeSearchQueryBuilder()
            .withQuery(QueryBuilders.matchQuery("name", "Artemis")).build();

        MatchQuery matchQuery = searchRequestQueryBuilder.buildQuery(nativeSearchQuery.getQuery()).match();
        String matchQueryFiled = matchQuery.field();
        String matchQueryValue = matchQuery.query()._get().toString();
        String matchQueryKind = matchQuery.query()._kind().toString();

        assertEquals("name", matchQueryFiled);
        assertEquals("Artemis", matchQueryValue);
        assertEquals("String", matchQueryKind);
    }
}
