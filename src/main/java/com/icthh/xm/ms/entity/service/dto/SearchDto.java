package com.icthh.xm.ms.entity.service.dto;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;

@Builder
@Getter
public class SearchDto {

    private String query;
    private Pageable pageable;
    private String privilegeKey;
    private FetchSourceFilter fetchSourceFilter;
    private Class entityClass;
}
