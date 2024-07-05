package com.icthh.xm.ms.entity.service.search.mapper;

import co.elastic.clients.elasticsearch._types.query_dsl.ChildScoreMode;
import co.elastic.clients.elasticsearch._types.query_dsl.CommonTermsQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.NestedQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.PrefixQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryStringQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.SimpleQueryStringQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.ZeroTermsQuery;
import co.elastic.clients.elasticsearch.core.search.InnerHits;
import com.icthh.xm.ms.entity.service.search.builder.CommonTermsQueryBuilder;
import com.icthh.xm.ms.entity.service.search.builder.InnerHitBuilder;
import com.icthh.xm.ms.entity.service.search.builder.MatchQueryBuilder;
import com.icthh.xm.ms.entity.service.search.builder.NestedQueryBuilder;
import com.icthh.xm.ms.entity.service.search.builder.PrefixQueryBuilder;
import com.icthh.xm.ms.entity.service.search.builder.QueryStringQueryBuilder;
import com.icthh.xm.ms.entity.service.search.builder.SimpleQueryStringBuilder;
import com.icthh.xm.ms.entity.service.search.builder.TermQueryBuilder;
import com.icthh.xm.ms.entity.service.search.builder.TermsQueryBuilder;
import com.icthh.xm.ms.entity.service.search.enums.ScoreMode;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface QueryTypeBuilderMapper {

    @Mapping(target = "allowLeadingWildcard", source = "")
    @Mapping(target = "query", source = "queryString")
    @Mapping(target = "boost", expression = "java(queryType.boost())")
    @Mapping(target = "queryName", expression = "java(queryType.getWriteableName())")
    @Mapping(target = "autoGenerateSynonymsPhraseQuery", source = "autoGenerateSynonymsPhraseQuery")
    @Mapping(target = "defaultOperator", source = "defaultOperator", qualifiedByName = "convertOperator")
    @Mapping(target = "analyzer", ignore = true)
    @Mapping(target = "analyzeWildcard", ignore = true)
    @Mapping(target = "fields", ignore = true)
    @Mapping(target = "lenient", ignore = true)
    @Mapping(target = "minimumShouldMatch", ignore = true)
    @Mapping(target = "quoteFieldSuffix", ignore = true)
    @Mapping(target = "type", ignore = true)
    @Mapping(target = "timeZone", ignore = true)
    @Mapping(target = "tieBreaker", ignore = true)
    @Mapping(target = "rewrite", ignore = true)
    @Mapping(target = "quoteAnalyzer", ignore = true)
    @Mapping(target = "phraseSlop", ignore = true)
    @Mapping(target = "maxDeterminizedStates", ignore = true)
    @Mapping(target = "fuzzyRewrite", ignore = true)
    @Mapping(target = "fuzziness", ignore = true)
    @Mapping(target = "escape", ignore = true)
    @Mapping(target = "enablePositionIncrements", ignore = true)
    @Mapping(target = "defaultField", ignore = true)
    QueryStringQuery.Builder toQueryStringQueryBuilder(QueryStringQueryBuilder queryType);

    @Mapping(target = "query", source = "text", qualifiedByName = "convertObjectToString")
    @Mapping(target = "field", source = "fieldName")
    @Mapping(target = "boost", expression = "java(commonTermsQueryBuilder.boost())")
    @Mapping(target = "queryName", expression = "java(commonTermsQueryBuilder.getWriteableName())")
    @Mapping(target = "analyzer", ignore = true)
    @Mapping(target = "cutoffFrequency", source = "cutoffFrequency")
    @Mapping(target = "highFreqOperator", source = "highFreqOperator", qualifiedByName = "convertOperator")
    @Mapping(target = "lowFreqOperator", source = "lowFreqOperator", qualifiedByName = "convertOperator")
    @Mapping(target = "minimumShouldMatch", ignore = true)
    CommonTermsQuery.Builder toCommonTermsQueryBuilder(CommonTermsQueryBuilder commonTermsQueryBuilder);

    @Mapping(target = "field", source = "fieldName")
    @Mapping(target = "query", ignore = true)
    @Mapping(target = "boost", expression = "java(matchQueryBuilder.boost())")
    @Mapping(target = "queryName", expression = "java(matchQueryBuilder.getWriteableName())")
    @Mapping(target = "analyzer", ignore = true)
    @Mapping(target = "autoGenerateSynonymsPhraseQuery", source = "autoGenerateSynonymsPhraseQuery")
    @Mapping(target = "cutoffFrequency", ignore = true)
    @Mapping(target = "fuzziness", ignore = true)
    @Mapping(target = "fuzzyRewrite", ignore = true)
    @Mapping(target = "fuzzyTranspositions", source = "fuzzyTranspositions")
    @Mapping(target = "lenient", source = "lenient")
    @Mapping(target = "maxExpansions", source = "maxExpansions")
    @Mapping(target = "minimumShouldMatch", ignore = true)
    @Mapping(target = "operator", source = "operator", qualifiedByName = "convertOperator")
    @Mapping(target = "prefixLength", source = "prefixLength")
    @Mapping(target = "zeroTermsQuery", source = "zeroTermsQuery", qualifiedByName = "convertZeroTermsQuery")
    MatchQuery.Builder toMatchQueryBuilder(MatchQueryBuilder matchQueryBuilder);

    @Mapping(target = "field", source = "fieldName")
    @Mapping(target = "value", ignore = true)
    @Mapping(target = "boost", expression = "java(termQueryBuilder.boost())")
    @Mapping(target = "queryName", expression = "java(termQueryBuilder.getWriteableName())")
    @Mapping(target = "caseInsensitive", ignore = true)
    TermQuery.Builder toTermQueryBuilder(TermQueryBuilder termQueryBuilder);

    @Mapping(target = "path", source = "path")
    @Mapping(target = "boost", expression = "java(nestedQueryBuilder.boost())")
    @Mapping(target = "queryName", expression = "java(nestedQueryBuilder.getWriteableName())")
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
    @Mapping(target = "fields", ignore = true)
    @Mapping(target = "docvalueFields", ignore = true)
    @Mapping(target = "collapse", ignore = true)
    InnerHits.Builder toInnerHitBuilder(InnerHitBuilder innerHitBuilder);

    @Mapping(target = "quoteFieldSuffix", expression = "java(builder.quoteFieldSuffix())")
    @Mapping(target = "query", expression = "java(builder.value())")
    @Mapping(target = "minimumShouldMatch", expression = "java(builder.minimumShouldMatch())")
    @Mapping(target = "lenient", expression = "java(builder.lenient())")
    @Mapping(target = "fuzzyTranspositions", expression = "java(builder.fuzzyTranspositions())")
    @Mapping(target = "fuzzyPrefixLength", expression = "java(builder.fuzzyPrefixLength())")
    @Mapping(target = "fuzzyMaxExpansions", expression = "java(builder.fuzzyMaxExpansions())")
    @Mapping(target = "defaultOperator", expression = "java(convertOperator(builder.defaultOperator()))")
    @Mapping(target = "autoGenerateSynonymsPhraseQuery", expression = "java(builder.autoGenerateSynonymsPhraseQuery())")
    @Mapping(target = "analyzeWildcard", expression = "java(builder.analyzeWildcard())")
    @Mapping(target = "analyzer", ignore = true)
    @Mapping(target = "flags", ignore = true)
    @Mapping(target = "fields", ignore = true)
    SimpleQueryStringQuery.Builder toSimpleQueryStringQueryBuilder(SimpleQueryStringBuilder builder);

    @Mapping(target = "value", expression = "java(builder.value())")
    @Mapping(target = "rewrite", expression = "java(builder.rewrite())")
    @Mapping(target = "field", expression = "java(builder.fieldName())")
    @Mapping(target = "caseInsensitive", ignore = true)
    PrefixQuery.Builder toPrefixQueryBuilder(PrefixQueryBuilder builder);

    @Mapping(target = "field", expression = "java(builder.fieldName())")
    @Mapping(target = "terms", ignore = true)
    TermsQuery.Builder toTermsQueryBuilder(TermsQueryBuilder builder);

    @Named("convertObjectToString")
    default String convertObjectToString(Object obj) {
        return String.valueOf(obj);
    }

    @Named("convertOperator")
    default Operator convertOperator(com.icthh.xm.ms.entity.service.search.query.Operator operator) {
        switch (operator) {
            case OR: return Operator.Or;
            case AND: return Operator.And;
            default: return null;
        }
    }

    @Named("convertZeroTermsQuery")
    default ZeroTermsQuery convertZeroTermsQuery(
        com.icthh.xm.ms.entity.service.search.query.MatchQuery.ZeroTermsQuery zeroTermsQuery) {
        switch (zeroTermsQuery) {
            case ALL: return ZeroTermsQuery.All;
            case NONE: return ZeroTermsQuery.None;
            default: return null;
        }
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
