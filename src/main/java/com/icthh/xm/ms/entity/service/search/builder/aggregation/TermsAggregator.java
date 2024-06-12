package com.icthh.xm.ms.entity.service.search.builder.aggregation;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.common.util.Comparators;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.ToXContentFragment;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.search.DocValueFormat;
import org.elasticsearch.search.aggregations.Aggregator;
import org.elasticsearch.search.aggregations.AggregatorFactories;
import org.elasticsearch.search.aggregations.BucketOrder;
import org.elasticsearch.search.aggregations.InternalOrder;
import org.elasticsearch.search.aggregations.bucket.BucketsAggregator;
import org.elasticsearch.search.aggregations.bucket.DeferableBucketAggregator;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.elasticsearch.search.aggregations.bucket.SingleBucketAggregator;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregator;
import org.elasticsearch.search.aggregations.bucket.terms.InternalTerms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.NumericMetricsAggregator;
import org.elasticsearch.search.aggregations.pipeline.PipelineAggregator;
import org.elasticsearch.search.aggregations.support.AggregationPath;
import org.elasticsearch.search.internal.SearchContext;

import java.io.IOException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public abstract class TermsAggregator extends DeferableBucketAggregator {
    protected final DocValueFormat format;
    protected final TermsAggregator.BucketCountThresholds bucketCountThresholds;
    protected final BucketOrder order;
    protected final Set<Aggregator> aggsUsedForSorting = new HashSet();
    protected final Aggregator.SubAggCollectionMode collectMode;

    public TermsAggregator(String name, AggregatorFactories factories, SearchContext context, Aggregator parent, TermsAggregator.BucketCountThresholds bucketCountThresholds, BucketOrder order, DocValueFormat format, Aggregator.SubAggCollectionMode collectMode, List<PipelineAggregator> pipelineAggregators, Map<String, Object> metaData) throws IOException {
        super(name, factories, context, parent, pipelineAggregators, metaData);
        this.bucketCountThresholds = bucketCountThresholds;
        this.order = InternalOrder.validate(order, this);
        this.format = format;
        if (this.subAggsNeedScore() && descendsFromNestedAggregator(parent)) {
            this.collectMode = SubAggCollectionMode.DEPTH_FIRST;
        } else {
            this.collectMode = collectMode;
        }

        if (order instanceof InternalOrder.Aggregation) {
            AggregationPath path = ((InternalOrder.Aggregation)order).path();
            this.aggsUsedForSorting.add(path.resolveTopmostAggregator(this));
        } else if (order instanceof InternalOrder.CompoundOrder) {
            InternalOrder.CompoundOrder compoundOrder = (InternalOrder.CompoundOrder)order;
            Iterator var12 = compoundOrder.orderElements().iterator();

            while(var12.hasNext()) {
                BucketOrder orderElement = (BucketOrder)var12.next();
                if (orderElement instanceof InternalOrder.Aggregation) {
                    AggregationPath path = ((InternalOrder.Aggregation)orderElement).path();
                    this.aggsUsedForSorting.add(path.resolveTopmostAggregator(this));
                }
            }
        }

    }

    static boolean descendsFromNestedAggregator(Aggregator parent) {
        while(parent != null) {
            if (parent.getClass() == NestedAggregator.class) {
                return true;
            }

            parent = parent.parent();
        }

        return false;
    }

    private boolean subAggsNeedScore() {
        Aggregator[] var1 = this.subAggregators;
        int var2 = var1.length;

        for(int var3 = 0; var3 < var2; ++var3) {
            Aggregator subAgg = var1[var3];
            if (subAgg.needsScores()) {
                return true;
            }
        }

        return false;
    }

    public Comparator<MultiBucketsAggregation.Bucket> bucketComparator(AggregationPath path, boolean asc) {
        Aggregator aggregator = path.resolveAggregator(this);
        String key = path.lastPathElement().key;
        if (aggregator instanceof SingleBucketAggregator) {
            assert key == null : "this should be picked up before the aggregation is executed - on validate";

            return (b1, b2) -> {
                int mul = asc ? 1 : -1;
                int v1 = ((SingleBucketAggregator)aggregator).bucketDocCount(((InternalTerms.Bucket)b1).bucketOrd);
                int v2 = ((SingleBucketAggregator)aggregator).bucketDocCount(((InternalTerms.Bucket)b2).bucketOrd);
                return mul * (v1 - v2);
            };
        } else {
            assert !(aggregator instanceof BucketsAggregator) : "this should be picked up before the aggregation is executed - on validate";

            if (aggregator instanceof NumericMetricsAggregator.MultiValue) {
                assert key != null : "this should be picked up before the aggregation is executed - on validate";

                return (b1, b2) -> {
                    double v1 = ((NumericMetricsAggregator.MultiValue)aggregator).metric(key, ((InternalTerms.Bucket)b1).bucketOrd);
                    double v2 = ((NumericMetricsAggregator.MultiValue)aggregator).metric(key, ((InternalTerms.Bucket)b2).bucketOrd);
                    return Comparators.compareDiscardNaN(v1, v2, asc);
                };
            } else {
                return (b1, b2) -> {
                    double v1 = ((NumericMetricsAggregator.SingleValue)aggregator).metric(((InternalTerms.Bucket)b1).bucketOrd);
                    double v2 = ((NumericMetricsAggregator.SingleValue)aggregator).metric(((InternalTerms.Bucket)b2).bucketOrd);
                    return Comparators.compareDiscardNaN(v1, v2, asc);
                };
            }
        }
    }

    protected boolean shouldDefer(Aggregator aggregator) {
        return this.collectMode == SubAggCollectionMode.BREADTH_FIRST && !this.aggsUsedForSorting.contains(aggregator);
    }

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

        public BucketCountThresholds(StreamInput in) throws IOException {
            this.requiredSize = in.readInt();
            this.shardSize = in.readInt();
            this.minDocCount = in.readLong();
            this.shardMinDocCount = in.readLong();
        }

        public void writeTo(StreamOutput out) throws IOException {
            out.writeInt(this.requiredSize);
            out.writeInt(this.shardSize);
            out.writeLong(this.minDocCount);
            out.writeLong(this.shardMinDocCount);
        }

        public BucketCountThresholds(TermsAggregator.BucketCountThresholds bucketCountThresholds) {
            this(bucketCountThresholds.minDocCount, bucketCountThresholds.shardMinDocCount, bucketCountThresholds.requiredSize, bucketCountThresholds.shardSize);
        }

        public void ensureValidity() {
            if (this.shardSize < this.requiredSize) {
                this.setShardSize(this.requiredSize);
            }

            if (this.shardMinDocCount > this.minDocCount) {
                this.setShardMinDocCount(this.minDocCount);
            }

            if (this.requiredSize > 0 && this.shardSize > 0) {
                if (this.minDocCount < 0L || this.shardMinDocCount < 0L) {
                    throw new ElasticsearchException("parameter [min_doc_count] and [shardMinDocCount] must be >=0 in terms aggregation.", new Object[0]);
                }
            } else {
                throw new ElasticsearchException("parameters [required_size] and [shard_size] must be >0 in terms aggregation.", new Object[0]);
            }
        }

        public long getShardMinDocCount() {
            return this.shardMinDocCount;
        }

        public void setShardMinDocCount(long shardMinDocCount) {
            this.shardMinDocCount = shardMinDocCount;
        }

        public long getMinDocCount() {
            return this.minDocCount;
        }

        public void setMinDocCount(long minDocCount) {
            this.minDocCount = minDocCount;
        }

        public int getRequiredSize() {
            return this.requiredSize;
        }

        public void setRequiredSize(int requiredSize) {
            this.requiredSize = requiredSize;
        }

        public int getShardSize() {
            return this.shardSize;
        }

        public void setShardSize(int shardSize) {
            this.shardSize = shardSize;
        }

        public XContentBuilder toXContent(XContentBuilder builder, ToXContent.Params params) throws IOException {
            builder.field(org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder.REQUIRED_SIZE_FIELD_NAME.getPreferredName(), this.requiredSize);
            if (this.shardSize != -1) {
                builder.field(org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder.SHARD_SIZE_FIELD_NAME.getPreferredName(), this.shardSize);
            }

            builder.field(org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder.MIN_DOC_COUNT_FIELD_NAME.getPreferredName(), this.minDocCount);
            builder.field(TermsAggregationBuilder.SHARD_MIN_DOC_COUNT_FIELD_NAME.getPreferredName(), this.shardMinDocCount);
            return builder;
        }

        public int hashCode() {
            return Objects.hash(new Object[]{this.requiredSize, this.shardSize, this.minDocCount, this.shardMinDocCount});
        }

        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            } else if (this.getClass() != obj.getClass()) {
                return false;
            } else {
                org.elasticsearch.search.aggregations.bucket.terms.TermsAggregator.BucketCountThresholds other = (org.elasticsearch.search.aggregations.bucket.terms.TermsAggregator.BucketCountThresholds)obj;
                return Objects.equals(this.requiredSize, other.requiredSize) && Objects.equals(this.shardSize, other.shardSize) && Objects.equals(this.minDocCount, other.minDocCount) && Objects.equals(this.shardMinDocCount, other.shardMinDocCount);
            }
        }
    }
}

