package com.icthh.xm.ms.entity.service.impl;

import com.icthh.xm.commons.domain.FunctionResult;
import com.icthh.xm.commons.service.FunctionResultProcessor;
import com.icthh.xm.ms.entity.domain.FunctionContext;
import com.icthh.xm.ms.entity.domain.FunctionResultContext;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.ext.IdOrKey;
import com.icthh.xm.ms.entity.domain.spec.FunctionSpec;
import com.icthh.xm.ms.entity.service.FunctionContextService;
import com.icthh.xm.ms.entity.service.XmEntityService;
import com.icthh.xm.ms.entity.service.mapper.FunctionResultMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static java.util.Collections.emptyList;

@Slf4j
@Service
@RequiredArgsConstructor
public class FunctionResultProcessorImpl implements FunctionResultProcessor<FunctionSpec> {

    private final XmEntityService xmEntityService;
    private final FunctionContextService functionContextService;
    private final FunctionResultMapper mapper;

    @Override
    public FunctionResult wrapFunctionResult(String functionKey, Object executorResult, FunctionSpec functionSpec) {
        return processFunctionResult(functionKey, null, executorResult, functionSpec);
    }

    public FunctionResultContext processFunctionResult(String functionKey,
                                                 IdOrKey idOrKey,
                                                 Object data,
                                                 FunctionSpec functionSpec) {
        FunctionContext functionContext = toFunctionContext(functionKey, idOrKey, data, functionSpec);
        if (functionSpec.getSaveFunctionContext()) {
            functionContext = functionContextService.save(functionContext);
        }
        return mapper.toFunctionResultContext(functionContext);
    }

    private FunctionContext toFunctionContext(String functionKey, IdOrKey idOrKey,
                                              Object data,
                                              FunctionSpec functionSpec) {

        FunctionContext functionResult = new FunctionContext();
        // TODO review key & typeKey ...
        functionResult.setKey(functionKey + "-" + UUID.randomUUID().toString());
        functionResult.setTypeKey(functionKey);
        functionResult.setData((Map<String, Object>) data);
        functionResult.setStartDate(Instant.now());
        functionResult.setUpdateDate(functionResult.getStartDate());
        if (functionSpec.getSaveFunctionContext()) {
            XmEntity xmEntity = (idOrKey != null) ? xmEntityService.findOne(idOrKey, emptyList()) : null;
            functionResult.setXmEntity(xmEntity);
        }
        functionResult.setOnlyData(functionSpec.getOnlyData());
        functionResult.setBinaryDataField(functionSpec.getBinaryDataField());
        functionResult.setBinaryDataType(functionSpec.getBinaryDataType());
        return functionResult;
    }
}
