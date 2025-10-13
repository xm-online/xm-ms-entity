package com.icthh.xm.ms.entity.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.icthh.xm.commons.domain.enums.FunctionTxTypes;
import com.icthh.xm.commons.service.FunctionTxControl;
import com.icthh.xm.commons.service.impl.FunctionTxControlImpl;
import com.icthh.xm.ms.entity.AbstractJupiterUnitTest;
import com.icthh.xm.ms.entity.config.XmEntityTenantConfigService;
import com.icthh.xm.ms.entity.config.XmEntityTenantConfigService.XmEntityTenantConfig;
import com.icthh.xm.ms.entity.domain.FunctionContext;
import com.icthh.xm.ms.entity.domain.FunctionResultContext;
import com.icthh.xm.ms.entity.domain.ext.IdOrKey;
import com.icthh.xm.ms.entity.domain.spec.FunctionSpec;
import com.icthh.xm.ms.entity.security.access.XmEntityDynamicPermissionCheckService;
import com.icthh.xm.ms.entity.service.FunctionContextService;
import com.icthh.xm.ms.entity.service.XmEntityFunctionExecutorService;
import com.icthh.xm.ms.entity.service.XmEntityService;
import com.icthh.xm.ms.entity.service.XmEntitySpecService;
import com.icthh.xm.ms.entity.service.json.JsonValidationService;
import com.icthh.xm.ms.entity.service.mapper.FunctionResultMapper;
import com.icthh.xm.ms.entity.service.mapper.FunctionResultMapperImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static com.icthh.xm.ms.entity.domain.ext.IdOrKey.SELF;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FunctionServiceImplTxControlUnitTest extends AbstractJupiterUnitTest {

    private XmEntityFunctionServiceFacade functionServiceFacade;

    private XmEntitySpecService xmEntitySpecService;
    private XmEntityService xmEntityService;
    private XmEntityFunctionExecutorService functionExecutorServiceImpl;
    private FunctionContextService functionContextService;
    private XmEntityDynamicPermissionCheckService dynamicPermissionCheckService;
    private JsonValidationService jsonValidationService;
    private XmEntityTenantConfigService xmEntityTenantConfigService;
    private XmEntityTenantConfig xmEntityTenantConfig;
    private FunctionResultProcessorImpl functionResultProcessor;
    private XmEntityFunctionServiceImpl functionService;
    private FunctionResultMapper functionResultMapper;

    private FunctionTxControl functionTxControl;

    private String functionName = "F_NAME";

    private IdOrKey key = SELF;

    @BeforeEach
    public void setUp() {
        xmEntitySpecService = Mockito.mock(XmEntitySpecService.class);
        xmEntityService = Mockito.mock(XmEntityService.class);
        functionExecutorServiceImpl = Mockito.mock(XmEntityFunctionExecutorService.class);
        functionContextService = Mockito.mock(FunctionContextService.class);
        dynamicPermissionCheckService = Mockito.mock(XmEntityDynamicPermissionCheckService.class);
        xmEntityTenantConfigService = Mockito.mock(XmEntityTenantConfigService.class);
        functionTxControl = spy(new FunctionTxControlImpl());
        jsonValidationService = spy(new JsonValidationService(new ObjectMapper()));
        functionResultMapper = spy(new FunctionResultMapperImpl());

        functionResultProcessor = new FunctionResultProcessorImpl(xmEntityService, functionContextService, functionResultMapper);
        functionService = new XmEntityFunctionServiceImpl(dynamicPermissionCheckService, xmEntitySpecService,
            xmEntityTenantConfigService, jsonValidationService);
        functionServiceFacade = new XmEntityFunctionServiceFacade(functionService, functionTxControl, functionExecutorServiceImpl,
            functionResultProcessor, xmEntityService, xmEntitySpecService);
        xmEntityTenantConfig = new XmEntityTenantConfig();
        when(xmEntityTenantConfigService.getXmEntityTenantConfig()).thenReturn(xmEntityTenantConfig);
    }

    @Test
    public void executeWithNoTxMode() {

        FunctionSpec spec = getFunctionSpec(s -> {
            s.setSaveFunctionContext(Boolean.FALSE);
            s.setTxType(FunctionTxTypes.NO_TX);
            return s;
        });

        Map<String, Object> context = Maps.newHashMap();
        context.put("key1", "val1");

        Map<String, Object> data = Maps.newHashMap();
        data.put("KEY1", "VAL1");

        when(xmEntitySpecService.findFunction(functionName, "POST"))
            .thenReturn(Optional.of(spec));

        when(functionExecutorServiceImpl.execute(functionName, context, "POST"))
            .thenReturn(data);

        when(functionContextService.save(any())).thenReturn(new FunctionContext());

        verify(functionTxControl, never()).executeWithNoTx(any());
        verify(functionTxControl, never()).executeInTransactionWithRoMode(any());
        verify(functionTxControl, never()).executeInTransaction(any());

        FunctionResultContext fc = (FunctionResultContext) functionServiceFacade.execute(functionName, context, "POST");
        assertThat(fc.getTypeKey()).isEqualTo(functionName);
        assertThat(fc.getKey()).contains(functionName);
        assertThat(fc.getData().keySet()).containsSequence(data.keySet().toArray(new String[0]));

        verify(functionContextService, never()).save(any());
        verify(functionTxControl, times(1)).executeWithNoTx(any());
        verify(functionTxControl, never()).executeInTransactionWithRoMode(any());
        verify(functionTxControl, never()).executeInTransaction(any());
    }

    @Test
    public void executeWitReadOnlyTxModeAndSaveCtxFalse() {

        FunctionSpec spec = getFunctionSpec(s -> {
            s.setSaveFunctionContext(Boolean.FALSE);
            s.setTxType(FunctionTxTypes.READ_ONLY);
            return s;
        });

        Map<String, Object> context = Maps.newHashMap();
        context.put("key1", "val1");

        Map<String, Object> data = Maps.newHashMap();
        data.put("KEY1", "VAL1");

        when(xmEntitySpecService.findFunction(functionName, "POST"))
            .thenReturn(Optional.of(spec));

        when(functionExecutorServiceImpl.execute(functionName, context, "POST"))
            .thenReturn(data);

        when(functionContextService.save(any())).thenReturn(new FunctionContext());

        verify(functionTxControl, never()).executeWithNoTx(any());
        verify(functionTxControl, never()).executeInTransactionWithRoMode(any());
        verify(functionTxControl, never()).executeInTransaction(any());

        FunctionResultContext fc = (FunctionResultContext) functionServiceFacade.execute(functionName, context, "POST");
        assertThat(fc.getTypeKey()).isEqualTo(functionName);
        assertThat(fc.getKey()).contains(functionName);
        assertThat(fc.getData().keySet()).containsSequence(data.keySet().toArray(new String[0]));

        verify(functionContextService, never()).save(any());
        verify(functionTxControl, never()).executeWithNoTx(any());
        verify(functionTxControl, times(1)).executeInTransactionWithRoMode(any());
        verify(functionTxControl, never()).executeInTransaction(any());
    }

    @Test
    public void executeWitRWTxModeAndSaveCtxFalse() {

        FunctionSpec spec = getFunctionSpec(s -> {
            s.setSaveFunctionContext(Boolean.FALSE);
            s.setTxType(FunctionTxTypes.TX);
            return s;
        });

        Map<String, Object> context = Maps.newHashMap();
        context.put("key1", "val1");

        Map<String, Object> data = Maps.newHashMap();
        data.put("KEY1", "VAL1");

        when(xmEntitySpecService.findFunction(functionName, "POST"))
            .thenReturn(Optional.of(spec));

        when(functionExecutorServiceImpl.execute(functionName, context, "POST"))
            .thenReturn(data);

        when(functionContextService.save(any())).thenReturn(new FunctionContext());

        verify(functionTxControl, never()).executeWithNoTx(any());
        verify(functionTxControl, never()).executeInTransactionWithRoMode(any());
        verify(functionTxControl, never()).executeInTransaction(any());

        FunctionContext fc = (FunctionContext) functionServiceFacade.execute(functionName, context, "POST");
        assertThat(fc.getTypeKey()).isEqualTo(functionName);
        assertThat(fc.getKey()).contains(functionName);
        assertThat(fc.getData().keySet()).containsSequence(data.keySet().toArray(new String[0]));

        verify(functionContextService, never()).save(any());
        verify(functionTxControl, never()).executeWithNoTx(any());
        verify(functionTxControl, never()).executeInTransactionWithRoMode(any());
        verify(functionTxControl, times(1)).executeInTransaction(any());
    }

    private FunctionSpec getFunctionSpec(Function<FunctionSpec, FunctionSpec> functionSpec) {
        FunctionSpec spec = new FunctionSpec();
        spec.setKey(functionName);
        return functionSpec.apply(spec);
    }

}
