package com.icthh.xm.ms.entity.service.search.builder.aggregation;

import com.icthh.xm.ms.entity.service.search.aggregation.factory.AggregatorFactories;

import java.util.Map;

public abstract class AggregationBuilder {

    protected final String name;
    protected AggregatorFactories.Builder factoriesBuilder = AggregatorFactories.builder();

    protected AggregationBuilder(String name) {
        if (name == null) {
            throw new IllegalArgumentException("[name] must not be null: [" + name + "]");
        } else {
            this.name = name;
        }
    }

    public String getName() {
        return this.name;
    }

    public abstract AggregationBuilder setMetaData(Map<String, Object> var1);

    public abstract Map<String, Object> getMetaData();

}

