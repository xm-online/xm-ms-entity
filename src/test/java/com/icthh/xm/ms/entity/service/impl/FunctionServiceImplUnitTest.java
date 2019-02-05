package com.icthh.xm.ms.entity.service.impl;

import com.google.common.collect.Maps;
import com.icthh.xm.ms.entity.domain.FunctionContext;
import com.icthh.xm.ms.entity.domain.ext.IdOrKey;
import com.icthh.xm.ms.entity.domain.spec.FunctionSpec;
import com.icthh.xm.ms.entity.projection.XmEntityIdKeyTypeKey;
import com.icthh.xm.ms.entity.service.FunctionContextService;
import com.icthh.xm.ms.entity.service.FunctionExecutorService;
import com.icthh.xm.ms.entity.service.XmEntityService;
import com.icthh.xm.ms.entity.service.XmEntitySpecService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.AdditionalAnswers;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class FunctionServiceImplUnitTest {

    public static final String UNKNOWN_KEY = "UNKNOWN_KEY";

    private FunctionServiceImpl functionService;

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private XmEntitySpecService xmEntitySpecService;
    private XmEntityService xmEntityService;
    private FunctionExecutorService functionExecutorService;
    private FunctionContextService functionContextService;

    private String functionName = "F_NAME";

    private IdOrKey key = IdOrKey.SELF;

    final String xmEntityTypeKey = "DUMMY";

    @Before
    public void setUp() {
        xmEntitySpecService = Mockito.mock(XmEntitySpecService.class);
        xmEntityService = Mockito.mock(XmEntityService.class);
        functionExecutorService = Mockito.mock(FunctionExecutorService.class);
        functionContextService = Mockito.mock(FunctionContextService.class);
        functionService = new FunctionServiceImpl(xmEntitySpecService, xmEntityService,
            functionExecutorService, functionContextService);
    }

    @Test
    public void executeKeyNotNullCheck() {
        exception.expect(NullPointerException.class);
        exception.expectMessage("functionKey can't be null");
        functionService.execute(null, Maps.newHashMap());
    }

    @Test
    public void executeSpecNotFoundCheck() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Function not found, function key: " + UNKNOWN_KEY);
        when(xmEntitySpecService.findFunction(UNKNOWN_KEY))
            .thenReturn(Optional.empty());
        functionService.execute(UNKNOWN_KEY, Maps.newHashMap());
    }

    @Test
    public void executeWithNoSaveContext() {
        FunctionSpec spec = getFunctionSpec(Boolean.FALSE);

        Map<String, Object> context = Maps.newHashMap();
        context.put("key1", "val1");

        Map<String, Object> data = Maps.newHashMap();
        data.put("KEY1", "VAL1");

        when(xmEntitySpecService.findFunction(functionName))
            .thenReturn(Optional.of(spec));

        when(functionExecutorService.execute(functionName, context))
            .thenReturn(data);

        when(functionContextService.save(any())).thenReturn(new FunctionContext());

        FunctionContext fc = functionService.execute(functionName, context);
        assertThat(fc.getTypeKey()).isEqualTo(functionName);
        assertThat(fc.getKey()).contains(functionName);
        assertThat(fc.getData().keySet()).containsSequence(data.keySet().stream().toArray(String[]::new));

        verify(functionContextService, never()).save(any());
    }


    /**
     * Test in progress
     */
    @Test
    public void executeWithSaveContext() {
        FunctionSpec spec = getFunctionSpec(Boolean.TRUE);

        Map<String, Object> context = Maps.newHashMap();
        context.put("key1", "val1");

        Map<String, Object> data = Maps.newHashMap();
        data.put("KEY1", "VAL1");

        when(xmEntitySpecService.findFunction(functionName))
            .thenReturn(Optional.of(spec));

        when(functionExecutorService.execute(functionName, context))
            .thenReturn(data);

        when(functionContextService.save(any())).thenAnswer((Answer<FunctionContext>) invocation -> {
            Object[] args = invocation.getArguments();
            return (FunctionContext) args[0];
        });

        FunctionContext fc = functionService.execute(functionName, context);
        assertThat(fc.getTypeKey()).isEqualTo(functionName);
        assertThat(fc.getKey()).contains(functionName);
        assertThat(fc.getData().keySet()).containsSequence(data.keySet().stream().toArray(String[]::new));

        verify(functionContextService, Mockito.times(1)).save(any());
    }

    /**
     * Test in progress
     */
    @Test
    public void executeWithNullFunctionKeyAndNullId() {
        exception.expect(NullPointerException.class);
        exception.expectMessage("functionKey can't be null");
        functionService.execute(null, null, Maps.newHashMap());
    }

    /**
     * Test in progress
     */
    @Test
    public void executeWithFunctionKeyAndNullId() {
        exception.expect(NullPointerException.class);
        exception.expectMessage("idOrKey can't be null");
        functionService.execute("Any key", null, Maps.newHashMap());
    }

    @Test
    public void executeUnknownWithIdOrKey() {
        final String functionKey = UNKNOWN_KEY;

        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Function not found for entity type key " + xmEntityTypeKey
            + " and function key: " + functionKey);

        when(xmEntityService.getXmEntityIdKeyTypeKey(key)).thenReturn(getProjection(key));

        when(xmEntitySpecService.findFunction(xmEntityTypeKey, functionKey))
            .thenReturn(Optional.empty());

        functionService.execute(functionKey, key, Maps.newHashMap());
    }

    @Test
    public void executeWithNoSaveContextForIdOrKey() {
        FunctionSpec spec = getFunctionSpec(Boolean.FALSE);

        Map<String, Object> context = Maps.newHashMap();
        context.put("key1", "val1");

        Map<String, Object> data = Maps.newHashMap();
        data.put("KEY1", "VAL1");

        when(xmEntityService.getXmEntityIdKeyTypeKey(key)).thenReturn(getProjection(key));

        when(xmEntitySpecService.findFunction(xmEntityTypeKey, functionName))
            .thenReturn(Optional.of(spec));

        when(functionExecutorService.execute(functionName, key, xmEntityTypeKey, context))
            .thenReturn(data);

        when(functionContextService.save(any())).thenReturn(new FunctionContext());

        FunctionContext fc = functionService.execute(functionName, key, context);
        assertThat(fc.getTypeKey()).isEqualTo(functionName);
        assertThat(fc.getKey()).contains(functionName);
        assertThat(fc.getData().keySet()).containsSequence(data.keySet().stream().toArray(String[]::new));

        verify(functionContextService, never()).save(any());
    }

    @Test
    public void executeWithSaveContextForIdOrKey() {
        FunctionSpec spec = getFunctionSpec(Boolean.TRUE);

        Map<String, Object> context = Maps.newHashMap();
        context.put("key1", "val1");

        Map<String, Object> data = Maps.newHashMap();
        data.put("KEY1", "VAL1");

        when(xmEntityService.getXmEntityIdKeyTypeKey(key)).thenReturn(getProjection(key));

        when(xmEntitySpecService.findFunction(xmEntityTypeKey, functionName))
            .thenReturn(Optional.of(spec));

        when(functionExecutorService.execute(functionName, key, xmEntityTypeKey, context))
            .thenReturn(data);

        when(functionContextService.save(any())).then(AdditionalAnswers.returnsFirstArg());

        FunctionContext fc = functionService.execute(functionName, key, context);
        assertThat(fc.getTypeKey()).isEqualTo(functionName);
        assertThat(fc.getKey()).contains(functionName);
        assertThat(fc.getData().keySet()).containsSequence(data.keySet().stream().toArray(String[]::new));

        verify(functionContextService, Mockito.times(1)).save(any());
    }

    private FunctionSpec getFunctionSpec(Boolean saveContext) {
        FunctionSpec spec = new FunctionSpec();
        spec.setKey(functionName);
        spec.setSaveFunctionContext(saveContext);
        return spec;
    }

    private XmEntityIdKeyTypeKey getProjection(IdOrKey key) {
        return new XmEntityIdKeyTypeKey() {
            @Override
            public Long getId() {
                return key.getId();
            }

            @Override
            public String getKey() {
                return key.getKey();
            }

            @Override
            public String getTypeKey() {
                return xmEntityTypeKey;
            }
        };
    }

}
