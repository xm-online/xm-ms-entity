package com.icthh.xm.ms.entity.service.search.mapper;

import co.elastic.clients.elasticsearch._types.query_dsl.CommonTermsQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.NestedQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.SimpleQueryStringQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import com.icthh.xm.ms.entity.service.search.builder.CommonTermsQueryBuilder;
import com.icthh.xm.ms.entity.service.search.builder.MatchQueryBuilder;
import com.icthh.xm.ms.entity.service.search.builder.NestedQueryBuilder;
import com.icthh.xm.ms.entity.service.search.builder.QueryStringQueryBuilder;
import com.icthh.xm.ms.entity.service.search.builder.TermQueryBuilder;
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
    @Mapping(target = "query", ignore = true)
    NestedQuery.Builder toNestedQueryBuilder(NestedQueryBuilder nestedQueryBuilder);

    @Named("convertObjectToString")
    default String convertObjectToString(Object obj) {
        return String.valueOf(obj);
    }

}
