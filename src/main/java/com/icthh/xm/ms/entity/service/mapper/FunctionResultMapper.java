package com.icthh.xm.ms.entity.service.mapper;

import com.icthh.xm.ms.entity.domain.FunctionContext;
import com.icthh.xm.ms.entity.domain.FunctionResultContext;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FunctionResultMapper {

    FunctionResultContext toFunctionResultContext(FunctionContext context);
}
