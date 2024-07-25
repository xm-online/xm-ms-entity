package com.icthh.xm.ms.entity.service.search.query.dto;

import com.icthh.xm.ms.entity.service.search.builder.QueryBuilder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class DeleteQuery {
    private QueryBuilder query;
    private String index;
    private String type;
    private Integer pageSize;
    private Long scrollTimeInMillis;
}
