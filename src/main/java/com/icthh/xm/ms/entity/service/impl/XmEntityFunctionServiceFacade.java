package com.icthh.xm.ms.entity.service.impl;

import com.icthh.xm.commons.exceptions.EntityNotFoundException;
import com.icthh.xm.commons.service.FunctionTxControl;
import com.icthh.xm.commons.service.impl.AbstractFunctionServiceFacade;
import com.icthh.xm.ms.entity.domain.FunctionResultContext;
import com.icthh.xm.ms.entity.domain.ext.IdOrKey;
import com.icthh.xm.ms.entity.domain.spec.FunctionSpec;
import com.icthh.xm.ms.entity.projection.XmEntityIdKeyTypeKey;
import com.icthh.xm.ms.entity.projection.XmEntityStateProjection;
import com.icthh.xm.ms.entity.service.XmEntityFunctionExecutorService;
import com.icthh.xm.ms.entity.service.XmEntityService;
import com.icthh.xm.ms.entity.service.XmEntitySpecService;
import com.icthh.xm.ms.entity.util.CustomCollectionUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.icthh.xm.ms.entity.security.access.FeatureContext.FUNCTION;

@Slf4j
@Service
public class XmEntityFunctionServiceFacade extends AbstractFunctionServiceFacade<FunctionSpec> {

    public static final String POST_URLENCODED = "POST_URLENCODED";
    //Function is not visible, but could be executed
    public static final String NONE = "NONE";
    public static final String XM_ENITITY_FUNCTION_CALL_PRIV = "XMENTITY.FUNCTION.EXECUTE";

    private final XmEntityFunctionServiceImpl functionService;
    private final XmEntityService xmEntityService;
    private final XmEntitySpecService xmEntitySpecService;
    private final XmEntityFunctionExecutorService functionExecutorService;
    private final FunctionResultProcessorImpl functionResultProcessor;

    public XmEntityFunctionServiceFacade(XmEntityFunctionServiceImpl functionService,
                                         FunctionTxControl functionTxControl,
                                         XmEntityFunctionExecutorService functionExecutorService,
                                         FunctionResultProcessorImpl functionResultProcessor,
                                         XmEntityService xmEntityService,
                                         XmEntitySpecService xmEntitySpecService) {
        super(functionService, functionTxControl, functionExecutorService, functionResultProcessor);
        this.functionService = functionService;
        this.xmEntityService = xmEntityService;
        this.functionExecutorService = functionExecutorService;
        this.xmEntitySpecService = xmEntitySpecService;
        this.functionResultProcessor = functionResultProcessor;
    }

    public FunctionResultContext execute(final String functionKey,
                                         final IdOrKey idOrKey,
                                         final Map<String, Object> functionInput,
                                         final String httpMethod) {
        functionService.validateFunctionKey(functionKey);
        Objects.requireNonNull(idOrKey, "idOrKey can't be null");

        // get type key
        XmEntityStateProjection projection = xmEntityService.findStateProjectionById(idOrKey).orElseThrow(
            () -> new EntityNotFoundException("XmEntity with idOrKey [" + idOrKey + "] not found")
        );

        // validate that current XmEntity has function
        FunctionSpec functionSpec = findFunctionSpec(functionKey, projection, httpMethod);

        String functionKeyFromSpec = functionSpec.getKey();
        functionService.checkPermissions(FUNCTION, XM_ENITITY_FUNCTION_CALL_PRIV, functionKeyFromSpec);

        //orElseThrow is replaced by war message
        assertCallAllowedByState(functionSpec, projection);

        Map<String, Object> vInput = functionService.getValidFunctionInput(functionSpec, functionInput);
        functionService.enrichInputFromPathParams(functionKey, vInput, functionSpec);

        // execute function
        return (FunctionResultContext) callLepExecutor(functionSpec.getTxType(), () -> {
            Map<String, Object> data = functionExecutorService.execute(functionKeyFromSpec, idOrKey, projection.getTypeKey(), vInput);
            return functionResultProcessor.processFunctionResult(functionKeyFromSpec, idOrKey, data, functionSpec);
        });
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

    private FunctionSpec findFunctionSpec(String functionKey, XmEntityIdKeyTypeKey projection, String httpMethod) {
        return xmEntitySpecService.findEntityFunction(projection.getTypeKey(), functionKey, httpMethod).orElseThrow(
            () -> new IllegalArgumentException("Function not found for entity type key " + projection.getTypeKey()
                + " and function key: " + functionKey)
        );
    }
}
