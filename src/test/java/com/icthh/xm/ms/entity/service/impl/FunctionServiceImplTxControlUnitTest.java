package com.icthh.xm.ms.entity.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.icthh.xm.ms.entity.AbstractUnitTest;
import com.icthh.xm.ms.entity.config.XmEntityTenantConfigService;
import com.icthh.xm.ms.entity.config.XmEntityTenantConfigService.XmEntityTenantConfig;
import com.icthh.xm.ms.entity.domain.FunctionContext;
import com.icthh.xm.ms.entity.domain.ext.IdOrKey;
import com.icthh.xm.ms.entity.domain.spec.FunctionSpec;
import com.icthh.xm.ms.entity.security.access.DynamicPermissionCheckService;
import com.icthh.xm.ms.entity.service.FunctionContextService;
import com.icthh.xm.ms.entity.service.FunctionExecutorService;
import com.icthh.xm.ms.entity.service.XmEntityService;
import com.icthh.xm.ms.entity.service.XmEntitySpecService;
import com.icthh.xm.ms.entity.service.json.JsonValidationService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
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

public class FunctionServiceImplTxControlUnitTest extends AbstractUnitTest {

    private FunctionServiceImpl functionService;

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private XmEntitySpecService xmEntitySpecService;
    private XmEntityService xmEntityService;
    private FunctionExecutorService functionExecutorService;
    private FunctionContextService functionContextService;
    private DynamicPermissionCheckService dynamicPermissionCheckService;
    private JsonValidationService jsonValidationService;
    private XmEntityTenantConfigService xmEntityTenantConfigService;
    private XmEntityTenantConfig xmEntityTenantConfig;

    private FunctionTxControl functionTxControl;

    private String functionName = "F_NAME";

    private IdOrKey key = SELF;

    @Before
    public void setUp() {
        xmEntitySpecService = Mockito.mock(XmEntitySpecService.class);
        xmEntityService = Mockito.mock(XmEntityService.class);
        functionExecutorService = Mockito.mock(FunctionExecutorService.class);
        functionContextService = Mockito.mock(FunctionContextService.class);
        dynamicPermissionCheckService = Mockito.mock(DynamicPermissionCheckService.class);
        xmEntityTenantConfigService = Mockito.mock(XmEntityTenantConfigService.class);
        functionTxControl = spy(new FunctionTxControlImpl());
        jsonValidationService = spy(new JsonValidationService(new ObjectMapper()));
        functionService = new FunctionServiceImpl(xmEntitySpecService, xmEntityService,
            functionExecutorService, functionContextService, dynamicPermissionCheckService,
                jsonValidationService, xmEntityTenantConfigService, functionTxControl);
        xmEntityTenantConfig = new XmEntityTenantConfig();
        when(xmEntityTenantConfigService.getXmEntityTenantConfig()).thenReturn(xmEntityTenantConfig);
    }

    @Test
    public void executeWithNoTxMode() {

        FunctionSpec spec = getFunctionSpec(s -> {
            s.setSaveFunctionContext(Boolean.FALSE);
            s.setTxType(FunctionSpec.FunctionTxTypes.NO_TX);
            return s;
        });

        Map<String, Object> context = Maps.newHashMap();
        context.put("key1", "val1");

        Map<String, Object> data = Maps.newHashMap();
        data.put("KEY1", "VAL1");

        when(xmEntitySpecService.findFunction(functionName, "POST"))
            .thenReturn(Optional.of(spec));

        when(functionExecutorService.execute(functionName, context, "POST"))
            .thenReturn(data);

        when(functionContextService.save(any())).thenReturn(new FunctionContext());

        verify(functionTxControl, never()).executeWithNoTx(any());
        verify(functionTxControl, never()).executeInTransactionWithRoMode(any());
        verify(functionTxControl, never()).executeInTransaction(any());

        FunctionContext fc = functionService.execute(functionName, context, "POST");
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
            s.setTxType(FunctionSpec.FunctionTxTypes.READ_ONLY);
            return s;
        });

        Map<String, Object> context = Maps.newHashMap();
        context.put("key1", "val1");

        Map<String, Object> data = Maps.newHashMap();
        data.put("KEY1", "VAL1");

        when(xmEntitySpecService.findFunction(functionName, "POST"))
            .thenReturn(Optional.of(spec));

        when(functionExecutorService.execute(functionName, context, "POST"))
            .thenReturn(data);

        when(functionContextService.save(any())).thenReturn(new FunctionContext());

        verify(functionTxControl, never()).executeWithNoTx(any());
        verify(functionTxControl, never()).executeInTransactionWithRoMode(any());
        verify(functionTxControl, never()).executeInTransaction(any());

        FunctionContext fc = functionService.execute(functionName, context, "POST");
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
            s.setTxType(FunctionSpec.FunctionTxTypes.TX);
            return s;
        });

        Map<String, Object> context = Maps.newHashMap();
        context.put("key1", "val1");

        Map<String, Object> data = Maps.newHashMap();
        data.put("KEY1", "VAL1");

        when(xmEntitySpecService.findFunction(functionName, "POST"))
            .thenReturn(Optional.of(spec));

        when(functionExecutorService.execute(functionName, context, "POST"))
            .thenReturn(data);

        when(functionContextService.save(any())).thenReturn(new FunctionContext());

        verify(functionTxControl, never()).executeWithNoTx(any());
        verify(functionTxControl, never()).executeInTransactionWithRoMode(any());
        verify(functionTxControl, never()).executeInTransaction(any());

        FunctionContext fc = functionService.execute(functionName, context, "POST");
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
