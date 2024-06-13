package com.icthh.xm.ms.entity.service.search.builder.aggregation;

public abstract class TermsAggregator {

    public static class BucketCountThresholds {
        private long minDocCount;
        private long shardMinDocCount;
        private int requiredSize;
        private int shardSize;

        public BucketCountThresholds(long minDocCount, long shardMinDocCount, int requiredSize, int shardSize) {
            this.minDocCount = minDocCount;
            this.shardMinDocCount = shardMinDocCount;
            this.requiredSize = requiredSize;
            this.shardSize = shardSize;
        }

        public BucketCountThresholds(TermsAggregator.BucketCountThresholds bucketCountThresholds) {
            this(bucketCountThresholds.minDocCount, bucketCountThresholds.shardMinDocCount, bucketCountThresholds.requiredSize, bucketCountThresholds.shardSize);
        }

        public int getRequiredSize() {
            return this.requiredSize;
        }

        public void setRequiredSize(int requiredSize) {
            this.requiredSize = requiredSize;
        }

    }
}

