package com.icthh.xm.ms.entity.service.search.mapper;

import co.elastic.clients.elasticsearch._types.query_dsl.CommonTermsQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.SimpleQueryStringQuery;
import com.icthh.xm.ms.entity.service.search.builder.CommonTermsQueryBuilder;
import com.icthh.xm.ms.entity.service.search.builder.QueryStringQueryBuilder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface QueryTypeBuilderMapper {

    @Mapping(source = "queryString", target = "query")
    SimpleQueryStringQuery.Builder toSimpleQueryStringQueryBuilder(QueryStringQueryBuilder queryType);

//    @Mapping(source = "text", target = "query", qualifiedByName = "convertObjectToString")
    @Mapping(source = "fieldName", target = "field")
    CommonTermsQuery.Builder toCommonTermsQueryBuilder(CommonTermsQueryBuilder commonTermsQueryBuilder);

    @Named("convertObjectToString")
    private String convertObjectToString(Object obj) {
        return String.valueOf(obj);
    }
}
