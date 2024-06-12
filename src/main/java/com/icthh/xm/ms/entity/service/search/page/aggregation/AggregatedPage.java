package com.icthh.xm.ms.entity.service.search.page.aggregation;

import com.icthh.xm.ms.entity.service.search.aggregation.Aggregation;
import com.icthh.xm.ms.entity.service.search.aggregation.Aggregations;
import com.icthh.xm.ms.entity.service.search.page.ScoredPage;
import com.icthh.xm.ms.entity.service.search.page.ScrolledPage;

public interface AggregatedPage<T> extends ScrolledPage<T>, ScoredPage<T> {

    boolean hasAggregations();

    Aggregations getAggregations();

    Aggregation getAggregation(String name);
}
