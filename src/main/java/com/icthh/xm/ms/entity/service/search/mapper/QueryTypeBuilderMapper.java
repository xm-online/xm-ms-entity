package com.icthh.xm.ms.entity.service.search.mapper;

import co.elastic.clients.elasticsearch._types.query_dsl.ChildScoreMode;
import co.elastic.clients.elasticsearch._types.query_dsl.CommonTermsQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.NestedQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.SimpleQueryStringQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch.core.search.InnerHits;
import com.icthh.xm.ms.entity.service.search.builder.CommonTermsQueryBuilder;
import com.icthh.xm.ms.entity.service.search.builder.InnerHitBuilder;
import com.icthh.xm.ms.entity.service.search.builder.MatchQueryBuilder;
import com.icthh.xm.ms.entity.service.search.builder.NestedQueryBuilder;
import com.icthh.xm.ms.entity.service.search.builder.QueryStringQueryBuilder;
import com.icthh.xm.ms.entity.service.search.builder.TermQueryBuilder;
import com.icthh.xm.ms.entity.service.search.enums.ScoreMode;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface QueryTypeBuilderMapper {

    @Mapping(target = "query", source = "queryString")
    SimpleQueryStringQuery.Builder toSimpleQueryStringQueryBuilder(QueryStringQueryBuilder queryType);

    @Mapping(target = "query", source = "text", qualifiedByName = "convertObjectToString")
    @Mapping(target = "field", source = "fieldName")
    CommonTermsQuery.Builder toCommonTermsQueryBuilder(CommonTermsQueryBuilder commonTermsQueryBuilder);

    @Mapping(target = "field", source = "fieldName")
    @Mapping(target = "query", ignore = true)
    MatchQuery.Builder toMatchQueryBuilder(MatchQueryBuilder matchQueryBuilder);

    @Mapping(target = "field", source = "fieldName")
    @Mapping(target = "value", ignore = true)
    TermQuery.Builder toTermQueryBuilder(TermQueryBuilder termQueryBuilder);

    @Mapping(target = "path", source = "path")
    @Mapping(target = "scoreMode", expression = "java(resolveScoreMode(nestedQueryBuilder.scoreMode()))")
    @Mapping(target = "ignoreUnmapped", expression = "java(nestedQueryBuilder.ignoreUnmapped())")
    @Mapping(target = "query", ignore = true)
    @Mapping(target = "innerHits", ignore = true)
    NestedQuery.Builder toNestedQueryBuilder(NestedQueryBuilder nestedQueryBuilder);

    @Mapping(target = "storedFields", ignore = true)
    @Mapping(target = "source", ignore = true)
    @Mapping(target = "sort", ignore = true)
    @Mapping(target = "seqNoPrimaryTerm", ignore = true)
    @Mapping(target = "scriptFields", ignore = true)
    @Mapping(target = "highlight", ignore = true)
    @Mapping(target = "fields", source = "")
    @Mapping(target = "docvalueFields", ignore = true)
    @Mapping(target = "collapse", ignore = true)
    InnerHits.Builder toInnerHitBuilder(InnerHitBuilder innerHitBuilder);

    @Named("convertObjectToString")
    default String convertObjectToString(Object obj) {
        return String.valueOf(obj);
    }

    @Named("resolveScoreMode")
    default ChildScoreMode resolveScoreMode(ScoreMode scoreMode) {
        switch (scoreMode) {
            case Avg: return ChildScoreMode.Avg;
            case Max: return ChildScoreMode.Max;
            case Min: return ChildScoreMode.Min;
            case None: return ChildScoreMode.None;
            case Total: return ChildScoreMode.Sum;
            default: return null;
        }
    }

}
