package com.icthh.xm.ms.entity.service.search.builder.aggregation;

public abstract class AggregationBuilders {

    public static TermsAggregationBuilder terms(String name) {
        return new TermsAggregationBuilder(name, null);
    }

    public static StatsAggregationBuilder stats(String name) {
        return new StatsAggregationBuilder(name);
    }

}
