package com.icthh.xm.ms.entity.service.impl;

import com.icthh.xm.commons.exceptions.EntityNotFoundException;
import com.icthh.xm.ms.entity.config.XmEntityTenantConfigService;
import com.icthh.xm.ms.entity.domain.FunctionContext;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.ext.IdOrKey;
import com.icthh.xm.ms.entity.domain.spec.FunctionSpec;
import com.icthh.xm.ms.entity.projection.XmEntityIdKeyTypeKey;
import com.icthh.xm.ms.entity.projection.XmEntityStateProjection;
import com.icthh.xm.ms.entity.security.access.DynamicPermissionCheckService;
import com.icthh.xm.ms.entity.service.FunctionContextService;
import com.icthh.xm.ms.entity.service.FunctionExecutorService;
import com.icthh.xm.ms.entity.service.FunctionService;
import com.icthh.xm.ms.entity.service.XmEntityService;
import com.icthh.xm.ms.entity.service.XmEntitySpecService;
import com.icthh.xm.ms.entity.service.json.JsonValidationService;
import com.icthh.xm.ms.entity.util.CustomCollectionUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

import static java.util.Collections.emptyList;

/**
 * The {@link FunctionServiceImpl} class.
 */
@Slf4j
@Service("functionService")
@RequiredArgsConstructor
public class FunctionServiceImpl implements FunctionService {

    //Function is not visible, but could be executed
    public static String NONE = "NONE";

    public static String FUNCTION_CALL_PRIV = "FUNCTION.CALL";
    public static String XM_ENITITY_FUNCTION_CALL_PRIV = "XMENTITY.FUNCTION.EXECUTE";

    private final XmEntitySpecService xmEntitySpecService;
    private final XmEntityService xmEntityService;
    private final FunctionExecutorService functionExecutorService;
    private final FunctionContextService functionContextService;
    private final DynamicPermissionCheckService dynamicPermissionCheckService;
    private final JsonValidationService jsonValidationService;
    private final XmEntityTenantConfigService xmEntityTenantConfigService;
    private final FunctionTxControl functionTxControl;

    private final AntPathMatcher matcher = new AntPathMatcher();

    /**
     * {@inheritDoc}
     */
    @Override
    public FunctionContext execute(final String functionKey, final Map<String, Object> functionInput, String httpMethod) {

        Objects.requireNonNull(functionKey, "functionKey can't be null");
        Map<String, Object> vInput = CustomCollectionUtils.emptyIfNull(functionInput);

        dynamicPermissionCheckService.checkContextPermission(DynamicPermissionCheckService.FeatureContext.FUNCTION,
            FUNCTION_CALL_PRIV, functionKey);

        FunctionSpec functionSpec = findFunctionSpec(functionKey, null);

        validateFunctionInput(functionSpec, functionInput);

        enrichInputFromPathParams(functionKey, vInput, functionSpec);

        // execute function
        return callLepExecutor(functionSpec, () -> {
            Map<String, Object> data = functionExecutorService.execute(functionSpec.getKey(), vInput, httpMethod);
            return processFunctionResult(functionKey, data, functionSpec);
        });

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
            XM_ENITITY_FUNCTION_CALL_PRIV, functionKey);

        // get type key
        XmEntityStateProjection projection = xmEntityService.findStateProjectionById(idOrKey).orElseThrow(
            () -> new EntityNotFoundException("XmEntity with idOrKey [" + idOrKey + "] not found")
        );

        // validate that current XmEntity has function
        FunctionSpec functionSpec = findFunctionSpec(functionKey, projection);

        //orElseThrow is replaced by war message
        assertCallAllowedByState(functionSpec, projection);

        validateFunctionInput(functionSpec, functionInput);

        // execute function
        return callLepExecutor(functionSpec, () -> {
            Map<String, Object> data = functionExecutorService.execute(functionKey, idOrKey, projection.getTypeKey(), vInput);
            return processFunctionResult(functionKey, idOrKey, data, functionSpec);
        });

    }

    @Override
    public FunctionContext executeAnonymous(String functionKey, Map<String, Object> functionInput, String httpMethod) {
        FunctionSpec functionSpec = findFunctionSpec(functionKey, null);

        if (!functionSpec.getAnonymous()) {
            throw new AccessDeniedException("access denied");
        }

        Objects.requireNonNull(functionKey, "functionKey can't be null");
        Map<String, Object> vInput = CustomCollectionUtils.emptyIfNull(functionInput);

        enrichInputFromPathParams(functionKey, vInput, functionSpec);

        return callLepExecutor(functionSpec, () -> {
            Map<String, Object> data = functionExecutorService.executeAnonymousFunction(functionSpec.getKey(), vInput, httpMethod);
            return processFunctionResult(functionKey, data, functionSpec);
        });

    }

    protected FunctionContext callLepExecutor(FunctionSpec functionSpec, Supplier<FunctionContext> logic) {
        switch (functionSpec.getTxType()) {
            case READ_ONLY: return functionTxControl.executeInTransactionWithRoMode(logic);
            case NO_TX: return functionTxControl.executeWithNoTx(logic);
            default: return functionTxControl.executeInTransaction(logic);
        }
    }

    private void validateFunctionInput(FunctionSpec functionSpec, Map<String, Object> functionInput) {
        if (xmEntityTenantConfigService.getXmEntityTenantConfig().getEntityFunctions().getValidateFunctionInput()) {
            // exclude one when enabled for all
            if (!Boolean.FALSE.equals(functionSpec.getValidateFunctionInput())) {
                jsonValidationService.assertJson(functionInput, functionSpec.getInputSpec());
            }
        } else {
            // include one when disabled for all
            if (Boolean.TRUE.equals(functionSpec.getValidateFunctionInput())) {
                jsonValidationService.assertJson(functionInput, functionSpec.getInputSpec());
            }
        }
    }

    /**
     * Validates, if current entity state is one of allowed states
     * @param functionSpec - functionSpec
     * @param projection - entity projection
     */
    protected void assertCallAllowedByState(FunctionSpec functionSpec, XmEntityStateProjection projection) {
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
            XmEntity xmEntity = (idOrKey != null) ? xmEntityService.findOne(idOrKey, emptyList()) : null;
            functionResult.setXmEntity(xmEntity);
        }
        functionResult.setOnlyData(functionSpec.getOnlyData());
        functionResult.setBinaryDataField(functionSpec.getBinaryDataField());
        functionResult.setBinaryDataType(functionSpec.getBinaryDataType());
        return functionResult;
    }

    private void enrichInputFromPathParams(String functionKey, Map<String, Object> functionInput, FunctionSpec functionSpec) {
        if (functionSpec.getPath() != null && matcher.match(functionSpec.getPath(), functionKey)) {
            Map<String, String> pathParams = matcher.extractUriTemplateVariables(functionSpec.getPath(), functionKey);
            functionInput.putAll(pathParams);
        }
    }

}
