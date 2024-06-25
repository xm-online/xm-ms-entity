package com.icthh.xm.ms.entity.service.search.aggregation;

import java.util.List;

public interface MultiBucketsAggregation extends Aggregation {

    interface Bucket extends HasAggregations {

        Object getKey();


        String getKeyAsString();


        long getDocCount();


        @Override
        Aggregations getAggregations();

    }


    List<? extends Bucket> getBuckets();
}
