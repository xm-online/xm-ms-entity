package com.icthh.xm.ms.entity.service.search.builder.aggregation;

import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.search.aggregations.AggregatorFactories;
import org.elasticsearch.search.aggregations.AggregatorFactory;
import org.elasticsearch.search.aggregations.PipelineAggregationBuilder;
import org.elasticsearch.search.internal.SearchContext;

import java.io.IOException;
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
            return this;
        }
    }

    public AB subAggregation(PipelineAggregationBuilder aggregation) {
        if (aggregation == null) {
            throw new IllegalArgumentException("[aggregation] must not be null: [" + this.name + "]");
        } else {
            this.factoriesBuilder.addPipelineAggregator(aggregation);
            return this;
        }
    }

    public AB subAggregations(AggregatorFactories.Builder subFactories) {
        if (subFactories == null) {
            throw new IllegalArgumentException("[subFactories] must not be null: [" + this.name + "]");
        } else {
            this.factoriesBuilder = subFactories;
            return this;
        }
    }

    public AB setMetaData(Map<String, Object> metaData) {
        if (metaData == null) {
            throw new IllegalArgumentException("[metaData] must not be null: [" + this.name + "]");
        } else {
            this.metaData = metaData;
            return this;
        }
    }

    public Map<String, Object> getMetaData() {
        return this.metaData == null ? Collections.emptyMap() : Collections.unmodifiableMap(this.metaData);
    }

    public final String getWriteableName() {
        return this.getType();
    }

    public final AggregatorFactory<?> build(SearchContext context, AggregatorFactory<?> parent) throws IOException {
        AggregatorFactory<?> factory = this.doBuild(context, parent, this.factoriesBuilder);
        return factory;
    }

    protected abstract AggregatorFactory<?> doBuild(SearchContext var1, AggregatorFactory<?> var2, AggregatorFactories.Builder var3) throws IOException;

    public final XContentBuilder toXContent(XContentBuilder builder, ToXContent.Params params) throws IOException {
        builder.startObject(this.name);
        if (this.metaData != null) {
            builder.field("meta", this.metaData);
        }

        builder.field(this.getType());
        this.internalXContent(builder, params);
        if (this.factoriesBuilder != null && this.factoriesBuilder.count() > 0) {
            builder.field("aggregations");
            this.factoriesBuilder.toXContent(builder, params);
        }

        return builder.endObject();
    }

    protected abstract XContentBuilder internalXContent(XContentBuilder var1, ToXContent.Params var2) throws IOException;

    protected abstract int doHashCode();

    protected abstract boolean doEquals(Object var1);
}

