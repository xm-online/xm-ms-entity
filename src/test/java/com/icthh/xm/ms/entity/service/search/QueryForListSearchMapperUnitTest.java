package com.icthh.xm.ms.entity.service.search;

import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.NestedQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.search.SourceFilter;
import com.icthh.xm.ms.entity.AbstractUnitTest;
import com.icthh.xm.ms.entity.service.search.builder.NativeSearchQueryBuilder;
import com.icthh.xm.ms.entity.service.search.builder.QueryBuilders;
import com.icthh.xm.ms.entity.service.search.builder.SearchRequestQueryBuilder;
import com.icthh.xm.ms.entity.service.search.builder.aggregation.SearchRequestAggregationBuilder;
import com.icthh.xm.ms.entity.service.search.enums.ScoreMode;
import com.icthh.xm.ms.entity.service.search.filter.FetchSourceFilter;
import com.icthh.xm.ms.entity.service.search.mapper.AggregationTypeBuilderMapper;
import com.icthh.xm.ms.entity.service.search.mapper.QueryTypeBuilderMapper;
import com.icthh.xm.ms.entity.service.search.mapper.SearchRequestBuilder;
import com.icthh.xm.ms.entity.service.search.query.dto.NativeSearchQuery;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mapstruct.factory.Mappers;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class QueryForListSearchMapperUnitTest extends AbstractUnitTest {

    private final SearchRequestQueryBuilder searchRequestQueryBuilder = new SearchRequestQueryBuilder(Mappers.getMapper(QueryTypeBuilderMapper.class));

    private SearchRequestBuilder searchRequestBuilder;

    @Before
    public void setUp() {
        SearchRequestAggregationBuilder searchRequestAggregationBuilder = new SearchRequestAggregationBuilder(Mappers.getMapper(AggregationTypeBuilderMapper.class));
        searchRequestBuilder = new SearchRequestBuilder(searchRequestQueryBuilder, searchRequestAggregationBuilder);
    }

    @Test
    public void testQueryForList() {
        String query = "typeKey:ACCOUNT";
        NativeSearchQuery nativeSearchQuery = new NativeSearchQueryBuilder()
            .withQuery(QueryBuilders.queryStringQuery(query))
            .withPageable(PageRequest.of(0, 10))
            .build();

        String actualQuery = searchRequestQueryBuilder.buildQuery(nativeSearchQuery.getQuery()).simpleQueryString().query();

        assertEquals(query, actualQuery);
    }

    @Test
    public void testCommonTermsQuery() {
        String fieldName = "stateKey";
        String text = "ACTIVE";
        NativeSearchQuery nativeSearchQuery = new NativeSearchQueryBuilder()
            .withQuery(QueryBuilders.commonTermsQuery(fieldName, text))
            .withPageable(PageRequest.of(0, 10))
            .build();

        Query query = searchRequestQueryBuilder.buildQuery(nativeSearchQuery.getQuery());
        String actualField = query.common().field();
        String actualQuery = query.common().query();

        assertEquals(fieldName, actualField);
        assertEquals(text, actualQuery);
    }

    @Test
    public void testBoolQuery() {
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
        long templateIdValue = Long.parseLong(templateIdTermQuery.value()._get().toString());
        String templateIdValueKind = templateIdTermQuery.value()._kind().toString();

        assertEquals("data.templateId", templateIdField);
        assertEquals(10L, templateIdValue);
        assertEquals("Long", templateIdValueKind);
    }

    @Test
    public void testTermQuery() {
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
    public void testMatchQuery() {
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

    @Test
    public void testNestedQuery() {
        String path = "data.contactInfo";
        String queryString = "name:Hecate";
        NativeSearchQuery nativeSearchQuery = new NativeSearchQueryBuilder()
            .withQuery(QueryBuilders.nestedQuery(path, QueryBuilders.queryStringQuery(queryString), ScoreMode.None)).build();

        NestedQuery nestedQuery = searchRequestQueryBuilder.buildQuery(nativeSearchQuery.getQuery()).nested();
        String nestedQueryPath = nestedQuery.path();
        String nestedQueryStringQuery = nestedQuery.query().simpleQueryString().query();

        assertEquals(path, nestedQueryPath);
        assertEquals(queryString, nestedQueryStringQuery);
    }

    @Test
    public void testWithPageable() {
        int pageNumber = 2;
        int pageSize = 15;
        NativeSearchQuery nativeSearchQuery = new NativeSearchQueryBuilder()
            .withQuery(QueryBuilders.matchQuery("typeKey", "ACCOUNT"))
            .withPageable(PageRequest.of(pageNumber, pageSize))
            .build();

        SearchRequest searchRequest = searchRequestBuilder.buildSearchRequest(nativeSearchQuery);
        int actualPageNumber = searchRequest.from() / searchRequest.size();
        int actualPageSize = searchRequest.size();

        assertEquals(pageNumber, actualPageNumber);
        assertEquals(pageSize, actualPageSize);
    }

    @Test
    public void testWithSourceFilter() {
        String[] includes = new String[]{"id", "typeKey", "stateKey"};
        String[] excludes = new String[]{"key"};
        NativeSearchQuery nativeSearchQuery = new NativeSearchQueryBuilder()
            .withQuery(QueryBuilders.matchQuery("typeKey", "ACCOUNT"))
            .withPageable(PageRequest.of(0, 10))
            .withSourceFilter(new FetchSourceFilter(includes, excludes))
            .build();

        SearchRequest searchRequest = searchRequestBuilder.buildSearchRequest(nativeSearchQuery);
        SourceFilter sourceFilter = (SourceFilter) searchRequest.source()._get();
        List<String> actualIncludes = sourceFilter.includes();
        List<String> actualExcludes = sourceFilter.excludes();

        assertEquals(List.of(includes), actualIncludes);
        assertEquals(List.of(excludes), actualExcludes);
    }
}
