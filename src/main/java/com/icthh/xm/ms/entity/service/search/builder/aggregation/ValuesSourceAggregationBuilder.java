package com.icthh.xm.ms.entity.service.search.builder.aggregation;

import com.icthh.xm.ms.entity.service.search.builder.aggregation.support.ValueType;
import com.icthh.xm.ms.entity.service.search.builder.aggregation.support.ValuesSource;
import com.icthh.xm.ms.entity.service.search.builder.aggregation.support.ValuesSourceType;

public abstract class ValuesSourceAggregationBuilder<VS extends ValuesSource, AB extends ValuesSourceAggregationBuilder<VS, AB>> extends AbstractAggregationBuilder<AB> {
    private final ValuesSourceType valuesSourceType;
    private final ValueType targetValueType;
    private String field = null;

    protected ValuesSourceAggregationBuilder(String name, ValuesSourceType valuesSourceType, ValueType targetValueType) {
        super(name);
        if (valuesSourceType == null) {
            throw new IllegalArgumentException("[valuesSourceType] must not be null: [" + name + "]");
        } else {
            this.valuesSourceType = valuesSourceType;
            this.targetValueType = targetValueType;
        }
    }

    public AB field(String field) {
        if (field == null) {
            throw new IllegalArgumentException("[field] must not be null: [" + this.name + "]");
        } else {
            this.field = field;
            return (AB) this;
        }
    }

    public String field() {
        return this.field;
    }
}
