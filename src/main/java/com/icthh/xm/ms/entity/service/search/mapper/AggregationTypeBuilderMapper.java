package com.icthh.xm.ms.entity.service.search.mapper;

import co.elastic.clients.elasticsearch._types.aggregations.MaxAggregation;
import co.elastic.clients.elasticsearch._types.aggregations.StatsAggregation;
import co.elastic.clients.elasticsearch._types.aggregations.TermsAggregation;
import com.icthh.xm.ms.entity.service.search.builder.aggregation.MaxAggregationBuilder;
import com.icthh.xm.ms.entity.service.search.builder.aggregation.StatsAggregationBuilder;
import com.icthh.xm.ms.entity.service.search.builder.aggregation.TermsAggregationBuilder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AggregationTypeBuilderMapper {

    @Mapping(target = "showTermDocCountError", expression = "java(builder.showTermDocCountError())")
    @Mapping(target = "field", expression = "java(builder.field())")
    @Mapping(target = "minDocCount", expression = "java(Long.valueOf(builder.minDocCount()).intValue())")
    @Mapping(target = "size", expression = "java(builder.size())")
    @Mapping(target = "shardSize", ignore = true) // TODO: default is -1
    @Mapping(target = "valueType", ignore = true)
    @Mapping(target = "order", ignore = true)
    @Mapping(target = "format", ignore = true)
    @Mapping(target = "script", ignore = true)
    @Mapping(target = "missing", ignore = true)
    @Mapping(target = "missingOrder", ignore = true)
    @Mapping(target = "missingBucket", ignore = true)
    @Mapping(target = "collectMode", ignore = true)
    @Mapping(target = "executionHint", ignore = true)
    @Mapping(target = "exclude", ignore = true)
    @Mapping(target = "include", ignore = true)
    TermsAggregation.Builder toTermsAggregationBuilder(TermsAggregationBuilder builder);

    @Mapping(target = "field", expression = "java(builder.field())")
    @Mapping(target = "format", ignore = true)
    @Mapping(target = "script", ignore = true)
    @Mapping(target = "missing", ignore = true)
    StatsAggregation.Builder toStatsAggregationBuilder(StatsAggregationBuilder builder);

    @Mapping(target = "field", expression = "java(builder.field())")
    @Mapping(target = "format", ignore = true)
    @Mapping(target = "script", ignore = true)
    @Mapping(target = "missing", ignore = true)
    MaxAggregation.Builder toMaxAggregationBuilder(MaxAggregationBuilder builder);
}
