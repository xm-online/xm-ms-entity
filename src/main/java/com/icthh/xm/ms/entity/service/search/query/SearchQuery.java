package com.icthh.xm.ms.entity.service.search.query;

import com.icthh.xm.ms.entity.service.search.builder.QueryBuilder;
import com.icthh.xm.ms.entity.service.search.builder.aggregation.AbstractAggregationBuilder;

import java.util.List;

public interface SearchQuery extends Query {
    QueryBuilder getQuery();

    QueryBuilder getFilter();

    List<AbstractAggregationBuilder> getAggregations();
}
