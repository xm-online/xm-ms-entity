package com.icthh.xm.ms.entity.service.search;

import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.service.search.builder.NativeSearchQueryBuilder;
import com.icthh.xm.ms.entity.service.search.builder.QueryBuilders;
import com.icthh.xm.ms.entity.service.search.filter.FetchSourceFilter;
import com.icthh.xm.ms.entity.service.search.query.SearchQuery;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.data.domain.PageRequest;

public class QueryForListSearchMapperUnitTest {

    @Mock
    private ElasticsearchTemplateWrapper wrapper;

    @Test
    void testQueryForList() {
        String query = "";
        String[] includes = {"id"};
        SearchQuery searchQuery = new NativeSearchQueryBuilder()
            .withQuery(QueryBuilders.queryStringQuery(query))
            .withSourceFilter(new FetchSourceFilter(includes, null))
            .withPageable(new PageRequest(0, 10))
            .build();

        wrapper.queryForList(new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.queryStringQuery(query))
                .withSourceFilter(new FetchSourceFilter(includes, null))
                .withPageable(new PageRequest(0, 10))
                .build(),
            XmEntity.class);
    }

    @Test
    void testCommonTermsQuery() {
        wrapper.queryForPage(
            new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.commonTermsQuery("stateKey", "ACTIVE"))
                .withPageable(new PageRequest(0, 10)).build(),
            XmEntity.class);
    }

    @Test
    void testBoolQuery() {
        wrapper.queryForList(new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.boolQuery()
                    .must(QueryBuilders.termQuery("typeKey", "ACCOUNT"))
                    .must(QueryBuilders.matchQuery("data.templateId", 10)))
                .withPageable(PageRequest.of(0, 10))
                .build(),
            XmEntity.class);
    }
}
