package com.icthh.xm.ms.entity.service.impl;

import static com.icthh.xm.ms.entity.config.Constants.FUNCTION_PREFIX;
import static com.icthh.xm.ms.entity.config.Constants.FUNCTION_WITH_XM_ENTITY_PREFIX;

import com.icthh.xm.commons.exceptions.EntityNotFoundException;
import com.icthh.xm.commons.lep.XmLepScriptConfigServerResourceLoader;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.ms.entity.domain.FunctionContext;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.ext.IdOrKey;
import com.icthh.xm.ms.entity.domain.spec.FunctionSpec;
import com.icthh.xm.ms.entity.projection.XmEntityIdKeyTypeKey;
import com.icthh.xm.ms.entity.projection.XmEntityStateProjection;
import com.icthh.xm.ms.entity.repository.OneTimeFunctionKeyPool;
import com.icthh.xm.ms.entity.repository.OneTimeFunctionKeyPool.OneTimeFunctionKey;
import com.icthh.xm.ms.entity.security.access.DynamicPermissionCheckService;
import com.icthh.xm.ms.entity.service.FunctionContextService;
import com.icthh.xm.ms.entity.service.FunctionExecutorService;
import com.icthh.xm.ms.entity.service.FunctionService;
import com.icthh.xm.ms.entity.service.SeparateTransactionExecutor;
import com.icthh.xm.ms.entity.service.XmEntityService;
import com.icthh.xm.ms.entity.service.XmEntitySpecService;
import com.icthh.xm.ms.entity.util.CustomCollectionUtils;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The {@link FunctionServiceImpl} class.
 */
@Slf4j
@Transactional
@Service("functionService")
@RequiredArgsConstructor
public class FunctionServiceImpl implements FunctionService {

    //Function is not visible, but could be executed
    public static String NONE = "NONE";

    public static String FUNCTION_CALL_PRIV = "FUNCTION.CALL";
    public static String XM_ENITITY_FUNCTION_CALL_PRIV = "XMENTITY.FUNCTION.EXECUTE";
    public static String TENANT_KEY_PLACEHOLDER = "{tenant-key}";
    public static String XM_ENTITY_FUNCTION_PATH = "/config/tenants/{tenant-key}/entity/lep/function/";

    private final XmEntitySpecService xmEntitySpecService;
    private final XmEntityService xmEntityService;
    private final FunctionExecutorService functionExecutorService;
    private final FunctionContextService functionContextService;
    private final DynamicPermissionCheckService dynamicPermissionCheckService;
    private final XmLepScriptConfigServerResourceLoader scriptResourceLoader;
    private final TenantContextHolder tenantContextHolder;
    private final OneTimeFunctionKeyPool oneTimeFunctionIdPool;

    /**
     * {@inheritDoc}
     */
    @Override
    public FunctionContext execute(final String functionKey, final Map<String, Object> functionInput) {

        Objects.requireNonNull(functionKey, "functionKey can't be null");
        Map<String, Object> vInput = CustomCollectionUtils.emptyIfNull(functionInput);

        dynamicPermissionCheckService.checkContextPermission(DynamicPermissionCheckService.FeatureContext.FUNCTION,
            FUNCTION_CALL_PRIV, functionKey);

        FunctionSpec functionSpec = findFunctionSpec(functionKey, null);

        // execute function
        Map<String, Object> data = functionExecutorService.execute(functionKey, vInput);

        return processFunctionResult(functionKey,  data, functionSpec);
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

        Map<String, Object> vInput = CustomCollectionUtils.emptyIfNull(functionInput);

        dynamicPermissionCheckService.checkContextPermission(DynamicPermissionCheckService.FeatureContext.FUNCTION,
            XM_ENITITY_FUNCTION_CALL_PRIV , functionKey);

        // get type key
        XmEntityStateProjection projection = xmEntityService.findStateProjectionById(idOrKey).orElseThrow(
            () -> new EntityNotFoundException("XmEntity with idOrKey [" + idOrKey + "] not found")
        );

        // validate that current XmEntity has function
        FunctionSpec functionSpec = findFunctionSpec(functionKey, projection);

        //orElseThorw is replaced by war message
        assertCallAllowedByState(functionSpec, projection);

        // execute function
        Map<String, Object> data = functionExecutorService.execute(functionKey, idOrKey, projection.getTypeKey(), vInput);

        return processFunctionResult(functionKey, idOrKey, data, functionSpec);

    }

    @Autowired
    private SeparateTransactionExecutor transactionExecutor;
    @Override
    public FunctionContext evaluate(String functionSourceCode, Map<String, Object> functionInput) {
        return oneTimeFunction(functionSourceCode, FUNCTION_PREFIX, (functionKey) ->
            functionExecutorService.execute(functionKey, functionInput));
    }

    @Override
    public FunctionContext evaluate(String functionSourceCode,
                                    IdOrKey idOrKey,
                                    Map<String, Object> functionInput) {
        // get type key
        XmEntityStateProjection projection = xmEntityService.findStateProjectionById(idOrKey).orElseThrow(
            () -> new EntityNotFoundException("XmEntity with idOrKey [" + idOrKey + "] not found"));
        return oneTimeFunction(functionSourceCode, FUNCTION_WITH_XM_ENTITY_PREFIX, (functionKey) ->
            functionExecutorService.execute(functionKey, idOrKey, projection.getTypeKey(), functionInput));
    }

    @SneakyThrows
    private FunctionContext oneTimeFunction(String functionSourceCode, String functionPrefix,
                                            Function<String, Map<String, Object>> function) {
        String tenantKey = tenantContextHolder.getTenantKey();
        try (OneTimeFunctionKey key = oneTimeFunctionIdPool.take()) {
            String functionKey = key.getId();
            String lepPath = XM_ENTITY_FUNCTION_PATH.replace(TENANT_KEY_PLACEHOLDER, tenantKey);
            lepPath = lepPath + functionPrefix + "$$" + functionKey + "$$tenant.groovy";
            try {
                scriptResourceLoader.onRefresh(lepPath, functionSourceCode);
                return processFunctionResult(functionKey, function.apply(functionKey), new FunctionSpec());
            } finally {
                scriptResourceLoader.onRefresh(lepPath, null);
            }
        }
    }

    /**
     * Validates, if current entity state is one of allowed states
     * @param functionSpec - functionSpec
     * @param projection - entity projection
     */
    void assertCallAllowedByState(FunctionSpec functionSpec, XmEntityStateProjection projection) {
        List<String> allowedStates = CustomCollectionUtils.nullSafe(functionSpec.getAllowedStateKeys());
        if (allowedStates.isEmpty()) {
            return;
        }
        //this state is UI hide flag, so no execute validation applied
        if (allowedStates.contains(NONE)) {
            return;
        }

        if (!allowedStates.contains(projection.getStateKey())) {
            // TODO: to decide how to turn on state change checking per tenant
            log.warn("Entity state [{}] not found in specMapping for {}",
                     projection.getStateKey(),
                     functionSpec.getKey());
        }

    }

    private FunctionSpec findFunctionSpec(String functionKey, XmEntityIdKeyTypeKey projection) {
        if (projection == null) {
            return xmEntitySpecService.findFunction(functionKey).orElseThrow(
                () -> new IllegalArgumentException("Function not found, function key: " + functionKey));
        }
        return xmEntitySpecService.findFunction(projection.getTypeKey(), functionKey).orElseThrow(
            () -> new IllegalArgumentException("Function not found for entity type key " + projection.getTypeKey()
                + " and function key: " + functionKey)
        );
    }

    private FunctionContext processFunctionResult(String functionKey,
                                                  Map<String, Object> data,
                                                  FunctionSpec functionSpec) {
        return processFunctionResult(functionKey, null, data, functionSpec);
    }

    private FunctionContext processFunctionResult(String functionKey,
                                  IdOrKey idOrKey,
                                  Map<String, Object> data,
                                  FunctionSpec functionSpec) {
        FunctionContext functionResult = toFunctionContext(functionKey, idOrKey, data, functionSpec);
        if (functionSpec.getSaveFunctionContext()) {
            return functionContextService.save(functionResult);
        }
        return functionResult;
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
