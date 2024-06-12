package com.icthh.xm.ms.entity.service.search.builder.aggregation;

import java.util.Collections;
import java.util.Map;

public abstract class AbstractAggregationBuilder<AB extends AbstractAggregationBuilder<AB>> extends AggregationBuilder {

    protected Map<String, Object> metaData;

    public AbstractAggregationBuilder(String name) {
        super(name);
    }

    public AB subAggregation(AggregationBuilder aggregation) {
        if (aggregation == null) {
            throw new IllegalArgumentException("[aggregation] must not be null: [" + this.name + "]");
        } else {
            this.factoriesBuilder.addAggregator(aggregation);
            return (AB) this;
        }
    }

    public AB setMetaData(Map<String, Object> metaData) {
        if (metaData == null) {
            throw new IllegalArgumentException("[metaData] must not be null: [" + this.name + "]");
        } else {
            this.metaData = metaData;
            return (AB) this;
        }
    }

    public Map<String, Object> getMetaData() {
        return this.metaData == null ? Collections.emptyMap() : Collections.unmodifiableMap(this.metaData);
    }
}

