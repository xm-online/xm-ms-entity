package com.icthh.xm.ms.entity.service.search.builder.aggregation;

import com.icthh.xm.ms.entity.service.search.builder.aggregation.support.ValueType;
import com.icthh.xm.ms.entity.service.search.builder.aggregation.support.ValuesSourceType;


public class TermsAggregationBuilder extends ValuesSourceAggregationBuilder<ValuesSourceType, TermsAggregationBuilder> {

    protected static final TermsAggregator.BucketCountThresholds DEFAULT_BUCKET_COUNT_THRESHOLDS = new TermsAggregator.BucketCountThresholds(1L, 0L, 10, -1);

    private TermsAggregator.BucketCountThresholds bucketCountThresholds;
    private boolean showTermDocCountError;

    public TermsAggregationBuilder(String name, ValueType valueType) {
        super(name, ValuesSourceType.ANY, valueType);
        this.bucketCountThresholds = new TermsAggregator.BucketCountThresholds(DEFAULT_BUCKET_COUNT_THRESHOLDS);
        this.showTermDocCountError = false;
    }

    public TermsAggregationBuilder size(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("[size] must be greater than 0. Found [" + size + "] in [" + this.name + "]");
        } else {
            this.bucketCountThresholds.setRequiredSize(size);
            return this;
        }
    }

    public int size() {
        return this.bucketCountThresholds.getRequiredSize();
    }

}
