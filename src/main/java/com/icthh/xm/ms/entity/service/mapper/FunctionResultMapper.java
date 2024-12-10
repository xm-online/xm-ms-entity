package com.icthh.xm.ms.entity.service.mapper;

import com.icthh.xm.ms.entity.domain.FunctionContext;
import com.icthh.xm.ms.entity.domain.FunctionResultContext;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FunctionResultMapper {

    @Mapping(target = "onlyData", source = "onlyData")
    FunctionResultContext toFunctionResultContext(FunctionContext context);
}
