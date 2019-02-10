package com.icthh.xm.ms.entity.service.impl;

import com.icthh.xm.ms.entity.domain.FunctionContext;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.ext.IdOrKey;
import com.icthh.xm.ms.entity.domain.spec.FunctionSpec;
import com.icthh.xm.ms.entity.projection.XmEntityIdKeyTypeKey;
import com.icthh.xm.ms.entity.service.FunctionContextService;
import com.icthh.xm.ms.entity.service.FunctionExecutorService;
import com.icthh.xm.ms.entity.service.FunctionService;
import com.icthh.xm.ms.entity.service.XmEntityService;
import com.icthh.xm.ms.entity.service.XmEntitySpecService;
import com.icthh.xm.ms.entity.util.CustomCollectionUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * The {@link FunctionServiceImpl} class.
 */
@Slf4j
@Transactional
@Service("functionService")
public class FunctionServiceImpl implements FunctionService {

    private final XmEntitySpecService xmEntitySpecService;
    private final XmEntityService xmEntityService;
    private final FunctionExecutorService functionExecutorService;
    private final FunctionContextService functionContextService;

    public FunctionServiceImpl(XmEntitySpecService xmEntitySpecService,
                               XmEntityService xmEntityService,
                               FunctionExecutorService functionExecutorService,
                               FunctionContextService functionContextService) {
        this.xmEntitySpecService = xmEntitySpecService;
        this.xmEntityService = xmEntityService;
        this.functionExecutorService = functionExecutorService;
        this.functionContextService = functionContextService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FunctionContext execute(final String functionKey, final Map<String, Object> functionInput) {

        Objects.requireNonNull(functionKey, "functionKey can't be null");
        Map<String, Object> vInput = CustomCollectionUtils.emptyIfNull(functionInput);

        FunctionSpec functionSpec = null; /*xmEntitySpecService.findFunction(functionKey).orElseThrow(
            () -> new IllegalArgumentException("Function not found, function key: " + functionKey));*/

        // execute function
        Map<String, Object> data = functionExecutorService.execute(functionKey, vInput);

        // save result in FunctionContext
        if (functionSpec.getSaveFunctionContext()) {
            return saveResult(functionKey, null, data, functionSpec);
        } else {
            return toFunctionContext(functionKey, null, data, functionSpec);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FunctionContext execute(final String functionKey,
                                   final IdOrKey idOrKey,
                                   final Map<String, Object> functionInput) {
        Objects.requireNonNull(functionKey, "functionKey can't be null");
        Objects.requireNonNull(idOrKey, "idOrKey can't be null");

        Map<String, Object> vInput =  CustomCollectionUtils.emptyIfNull(functionInput);

        // get type key
        XmEntityIdKeyTypeKey projection = xmEntityService.getXmEntityIdKeyTypeKey(idOrKey);
        String xmEntityTypeKey = projection.getTypeKey();

        // validate that current XmEntity has function
        FunctionSpec functionSpec = xmEntitySpecService.findFunction(xmEntityTypeKey, functionKey).orElseThrow(
            () -> new IllegalArgumentException("Function not found for entity type key " + xmEntityTypeKey
            + " and function key: " + functionKey)
        );

        // execute function
        Map<String, Object> data = functionExecutorService.execute(functionKey, idOrKey, xmEntityTypeKey, vInput);

        // save result in FunctionContext
        if (functionSpec.getSaveFunctionContext()) {
            return saveResult(functionKey, idOrKey, data, functionSpec);
        } else {
            return toFunctionContext(functionKey, idOrKey, data, functionSpec);
        }
    }

    /**
     * Execute any function.
     *
     * @param functionKey function key
     * @param idOrKey     XmEntity id or key (can be {@code null})
     * @return function execution result context
     */
    private FunctionContext saveResult(String functionKey,
                                       IdOrKey idOrKey,
                                       Map<String, Object> data,
                                       FunctionSpec functionSpec) {
        FunctionContext functionResult = toFunctionContext(functionKey, idOrKey, data, functionSpec);
        return functionContextService.save(functionResult);
    }

    private FunctionContext toFunctionContext(String functionKey, IdOrKey idOrKey,
                                              Map<String, Object> data,
                                              FunctionSpec functionSpec) {

        FunctionContext functionResult = new FunctionContext();
        // TODO review key & typeKey ...
        functionResult.setKey(functionKey + "-" + UUID.randomUUID().toString());
        functionResult.setTypeKey(functionKey);
        functionResult.setData(data);
        functionResult.setStartDate(Instant.now());
        functionResult.setUpdateDate(functionResult.getStartDate());
        if (functionSpec.getSaveFunctionContext()) {
            XmEntity xmEntity = (idOrKey != null) ? xmEntityService.findOne(idOrKey) : null;
            functionResult.setXmEntity(xmEntity);
        }
        functionResult.setOnlyData(functionSpec.getOnlyData());
        return functionResult;
    }

}
