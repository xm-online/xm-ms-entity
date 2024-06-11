package com.icthh.xm.ms.entity.service.search;

import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.service.search.builder.NativeSearchQueryBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;

public class QueryForListSearchMapperUnitTest {

    private ElasticsearchTemplateWrapper wrapper; //TODO init

    @Test
    void testQueryForList(){
        wrapper.queryForList(new NativeSearchQueryBuilder()
                .withQuery(queryStringQuery(query))
                .withSourceFilter(new FetchSourceFilter(includes, null))
                .withPageable(new PageRequest(page, size))
                .build(),
            XmEntity.class);
    }
}
