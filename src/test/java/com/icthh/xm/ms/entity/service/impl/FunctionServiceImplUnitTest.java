package com.icthh.xm.ms.entity.service.impl;

import com.google.common.collect.Maps;
import com.icthh.xm.ms.entity.AbstractUnitTest;
import com.icthh.xm.ms.entity.domain.FunctionContext;
import com.icthh.xm.ms.entity.domain.ext.IdOrKey;
import com.icthh.xm.ms.entity.domain.spec.FunctionSpec;
import com.icthh.xm.ms.entity.projection.XmEntityStateProjection;
import com.icthh.xm.ms.entity.security.access.DynamicPermissionCheckService;
import com.icthh.xm.ms.entity.service.FunctionContextService;
import com.icthh.xm.ms.entity.service.FunctionExecutorService;
import com.icthh.xm.ms.entity.service.XmEntityService;
import com.icthh.xm.ms.entity.service.XmEntitySpecService;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Ignore;
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

public class FunctionServiceImplUnitTest extends AbstractUnitTest {

    public static final String UNKNOWN_KEY = "UNKNOWN_KEY";

    private FunctionServiceImpl functionService;

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private XmEntitySpecService xmEntitySpecService;
    private XmEntityService xmEntityService;
    private FunctionExecutorService functionExecutorService;
    private FunctionContextService functionContextService;
    private DynamicPermissionCheckService dynamicPermissionCheckService;

    private String functionName = "F_NAME";

    private IdOrKey key = IdOrKey.SELF;

    final String xmEntityTypeKey = "DUMMY";


    @Before
    public void setUp() {
        xmEntitySpecService = Mockito.mock(XmEntitySpecService.class);
        xmEntityService = Mockito.mock(XmEntityService.class);
        functionExecutorService = Mockito.mock(FunctionExecutorService.class);
        functionContextService = Mockito.mock(FunctionContextService.class);
        dynamicPermissionCheckService = Mockito.mock(DynamicPermissionCheckService.class);
        functionService = new FunctionServiceImpl(xmEntitySpecService, xmEntityService,
            functionExecutorService, functionContextService, dynamicPermissionCheckService);
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
        assertThat(fc.getData().keySet()).containsSequence(data.keySet().toArray(new String[0]));

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
        assertThat(fc.getData().keySet()).containsSequence(data.keySet().toArray(new String[0]));

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

        when(xmEntityService.findStateProjectionById(key)).thenReturn(getProjection(key));

        when(xmEntitySpecService.findFunction(xmEntityTypeKey, functionKey))
            .thenReturn(Optional.empty());

        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Function not found for entity type key " + xmEntityTypeKey
            + " and function key: " + functionKey);
        functionService.execute(functionKey, key, Maps.newHashMap());
    }

    @Test
    @Ignore("Ignored until state mapping behaviour will be agreed")
    public void executeFailsIfStatesNotMatched() {
        FunctionSpec spec = getFunctionSpec(Boolean.FALSE);
        spec.setAllowedStateKeys(Lists.newArrayList("SOME-STATE"));

        Map<String, Object> context = Maps.newHashMap();
        context.put("key1", "val1");

        Map<String, Object> data = Maps.newHashMap();
        data.put("KEY1", "VAL1");

        when(xmEntityService.findStateProjectionById(key)).thenReturn(getProjection(key));

        when(xmEntitySpecService.findFunction(xmEntityTypeKey, functionName))
            .thenReturn(Optional.of(spec));

        exception.expect(IllegalStateException.class);
        exception.expectMessage("Function call forbidden for current state");
        functionService.execute(functionName, key, Maps.newHashMap());

    }

    @Test
    public void executeWithAbsurdStateMapping() {
        FunctionSpec spec = getFunctionSpec(Boolean.FALSE);
        spec.setAllowedStateKeys(Lists.newArrayList("SOME-STATE"));

        Map<String, Object> context = Maps.newHashMap();
        context.put("key1", "val1");

        Map<String, Object> data = Maps.newHashMap();
        data.put("KEY1", "VAL1");

        when(xmEntityService.findStateProjectionById(key)).thenReturn(getProjection(key));

        when(xmEntitySpecService.findFunction(xmEntityTypeKey, functionName))
            .thenReturn(Optional.of(spec));

        when(functionExecutorService.execute(functionName, key, xmEntityTypeKey, context))
            .thenReturn(data);

        when(functionContextService.save(any())).thenReturn(new FunctionContext());

        FunctionContext fc = functionService.execute(functionName, key, context);
        assertThat(fc.getTypeKey()).isEqualTo(functionName);
        assertThat(fc.getKey()).contains(functionName);
        assertThat(fc.getData().keySet()).containsSequence(data.keySet().toArray(new String[0]));

        verify(functionContextService, never()).save(any());
    }

    @Test
    public void executeWithNoSaveContextForIdOrKey() {
        FunctionSpec spec = getFunctionSpec(Boolean.FALSE);

        Map<String, Object> context = Maps.newHashMap();
        context.put("key1", "val1");

        Map<String, Object> data = Maps.newHashMap();
        data.put("KEY1", "VAL1");

        when(xmEntityService.findStateProjectionById(key)).thenReturn(getProjection(key));

        when(xmEntitySpecService.findFunction(xmEntityTypeKey, functionName))
            .thenReturn(Optional.of(spec));

        when(functionExecutorService.execute(functionName, key, xmEntityTypeKey, context))
            .thenReturn(data);

        when(functionContextService.save(any())).thenReturn(new FunctionContext());

        FunctionContext fc = functionService.execute(functionName, key, context);
        assertThat(fc.getTypeKey()).isEqualTo(functionName);
        assertThat(fc.getKey()).contains(functionName);
        assertThat(fc.getData().keySet()).containsSequence(data.keySet().toArray(new String[0]));

        verify(functionContextService, never()).save(any());
    }

    @Test
    public void executeWithSaveContextForIdOrKey() {
        FunctionSpec spec = getFunctionSpec(Boolean.TRUE);

        Map<String, Object> context = Maps.newHashMap();
        context.put("key1", "val1");

        Map<String, Object> data = Maps.newHashMap();
        data.put("KEY1", "VAL1");

        when(xmEntityService.findStateProjectionById(key)).thenReturn(getProjection(key));

        when(xmEntitySpecService.findFunction(xmEntityTypeKey, functionName))
            .thenReturn(Optional.of(spec));

        when(functionExecutorService.execute(functionName, key, xmEntityTypeKey, context))
            .thenReturn(data);

        when(functionContextService.save(any())).then(AdditionalAnswers.returnsFirstArg());

        FunctionContext fc = functionService.execute(functionName, key, context);
        assertThat(fc.getTypeKey()).isEqualTo(functionName);
        assertThat(fc.getKey()).contains(functionName);
        assertThat(fc.getData().keySet()).containsSequence(data.keySet().toArray(new String[0]));

        verify(functionContextService, Mockito.times(1)).save(any());
    }

    @Test
    public void passStateValidationIfNoStateMapping() {
        FunctionSpec spec = new FunctionSpec();
        XmEntityStateProjection p = getProjection(IdOrKey.SELF).get();
        functionService.assertCallAllowedByState(spec, p);
    }

    @Test
    public void passStateValidationIfNONEStateIsSet() {
        FunctionSpec spec = new FunctionSpec();
        spec.setAllowedStateKeys(Lists.newArrayList(FunctionServiceImpl.NONE));
        XmEntityStateProjection p = getProjection(IdOrKey.SELF).get();
        functionService.assertCallAllowedByState(spec, p);
    }

    @Test
    public void passStateValidationIfStatesMatches() {
        FunctionSpec spec = new FunctionSpec();
        spec.setAllowedStateKeys(Lists.newArrayList("STATE"));
        XmEntityStateProjection p = getProjection(IdOrKey.SELF).get();
        functionService.assertCallAllowedByState(spec, p);
    }

    @Test
    public void failStateValidationIfStatesNotMatches() {
        FunctionSpec spec = new FunctionSpec();
        spec.setAllowedStateKeys(Lists.newArrayList("XX-STATE-XX"));
        XmEntityStateProjection p = getProjection(IdOrKey.SELF).get();
        functionService.assertCallAllowedByState(spec, p);
    }

    private FunctionSpec getFunctionSpec(Boolean saveContext) {
        FunctionSpec spec = new FunctionSpec();
        spec.setKey(functionName);
        spec.setSaveFunctionContext(saveContext);
        return spec;
    }

    private Optional<XmEntityStateProjection> getProjection(IdOrKey key) {
        return Optional.of(new XmEntityStateProjection() {
            @Override
            public String getStateKey() {
                return "STATE";
            }
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
        });
    }

}
