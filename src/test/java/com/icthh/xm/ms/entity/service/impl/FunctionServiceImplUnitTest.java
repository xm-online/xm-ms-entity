package com.icthh.xm.ms.entity.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.icthh.xm.commons.service.FunctionTxControl;
import com.icthh.xm.commons.service.impl.FunctionTxControlImpl;
import com.icthh.xm.ms.entity.AbstractJupiterUnitTest;
import com.icthh.xm.ms.entity.config.XmEntityTenantConfigService;
import com.icthh.xm.ms.entity.config.XmEntityTenantConfigService.XmEntityTenantConfig;
import com.icthh.xm.ms.entity.domain.FunctionContext;
import com.icthh.xm.ms.entity.domain.FunctionResultContext;
import com.icthh.xm.ms.entity.domain.ext.IdOrKey;
import com.icthh.xm.ms.entity.domain.spec.FunctionSpec;
import com.icthh.xm.ms.entity.projection.XmEntityStateProjection;
import com.icthh.xm.ms.entity.security.access.XmEntityDynamicPermissionCheckService;
import com.icthh.xm.ms.entity.service.FunctionContextService;
import com.icthh.xm.ms.entity.service.XmEntityFunctionExecutorService;
import com.icthh.xm.ms.entity.service.XmEntityService;
import com.icthh.xm.ms.entity.service.XmEntitySpecService;
import com.icthh.xm.ms.entity.service.json.JsonValidationService;
import com.icthh.xm.ms.entity.service.mapper.FunctionResultMapper;
import com.icthh.xm.ms.entity.service.mapper.FunctionResultMapperImpl;
import org.assertj.core.util.Lists;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.AdditionalAnswers;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.icthh.xm.ms.entity.domain.ext.IdOrKey.SELF;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class FunctionServiceImplUnitTest extends AbstractJupiterUnitTest {

    public static final String UNKNOWN_KEY = "UNKNOWN_KEY";
    public static final String VALIDATION_FUNCTION = "VALIDATION_FUNCTION";

    private XmEntityFunctionServiceFacade functionServiceFacade;

    private XmEntitySpecService xmEntitySpecService;
    private XmEntityService xmEntityService;
    private XmEntityFunctionExecutorService functionExecutorService;
    private FunctionContextService functionContextService;
    private XmEntityDynamicPermissionCheckService dynamicPermissionCheckService;
    private JsonValidationService jsonValidationService;
    private XmEntityTenantConfigService xmEntityTenantConfigService;
    private XmEntityTenantConfig xmEntityTenantConfig;
    private FunctionResultProcessorImpl functionResultProcessor;
    private XmEntityFunctionServiceImpl functionService;
    private FunctionResultMapper functionResultMapper;

    private FunctionTxControl functionTxControl = new FunctionTxControlImpl();

    private String functionName = "F_NAME";

    private IdOrKey key = SELF;

    final String xmEntityTypeKey = "DUMMY";


    @BeforeEach
    public void setUp() {
        xmEntitySpecService = Mockito.mock(XmEntitySpecService.class);
        xmEntityService = Mockito.mock(XmEntityService.class);
        functionExecutorService = Mockito.mock(XmEntityFunctionExecutorService.class);
        functionContextService = Mockito.mock(FunctionContextService.class);
        dynamicPermissionCheckService = Mockito.mock(XmEntityDynamicPermissionCheckService.class);
        xmEntityTenantConfigService = Mockito.mock(XmEntityTenantConfigService.class);
        jsonValidationService = spy(new JsonValidationService(new ObjectMapper()));
        functionResultMapper = spy(new FunctionResultMapperImpl());
        functionResultProcessor = new FunctionResultProcessorImpl(xmEntityService, functionContextService, functionResultMapper);
        functionService = new XmEntityFunctionServiceImpl(dynamicPermissionCheckService, xmEntitySpecService,
            xmEntityTenantConfigService, jsonValidationService);
        functionServiceFacade = new XmEntityFunctionServiceFacade(functionService, functionTxControl, functionExecutorService,
            functionResultProcessor, xmEntityService, xmEntitySpecService);
        xmEntityTenantConfig = new XmEntityTenantConfig();
        when(xmEntityTenantConfigService.getXmEntityTenantConfig()).thenReturn(xmEntityTenantConfig);
    }

    @Test
    public void executeKeyNotNullCheck() {
        NullPointerException exception = assertThrows(NullPointerException.class, () -> {
            functionServiceFacade.execute(null, Maps.newHashMap(), null);
        });
        assertEquals("functionKey can't be null", exception.getMessage());
    }

    @Test
    public void executeSpecNotFoundCheck() {

        when(xmEntitySpecService.findFunction(UNKNOWN_KEY, "POST"))
            .thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            functionServiceFacade.execute(UNKNOWN_KEY, Maps.newHashMap(), null);
        });

        assertEquals("Function not found, function key: " + UNKNOWN_KEY, exception.getMessage());
    }

    @Test
    public void executeWithNoSaveContext() {
        FunctionSpec spec = getFunctionSpec(Boolean.FALSE);

        Map<String, Object> context = Maps.newHashMap();
        context.put("key1", "val1");

        Map<String, Object> data = Maps.newHashMap();
        data.put("KEY1", "VAL1");

        when(xmEntitySpecService.findFunction(functionName, "POST"))
            .thenReturn(Optional.of(spec));

        when(functionExecutorService.execute(functionName, context, "POST"))
            .thenReturn(data);

        when(functionContextService.save(any())).thenReturn(new FunctionContext());

        FunctionContext fc = (FunctionContext) functionServiceFacade.execute(functionName, context, "POST");
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

        when(xmEntitySpecService.findFunction(functionName, "POST"))
            .thenReturn(Optional.of(spec));

        when(functionExecutorService.execute(functionName, context, "POST"))
            .thenReturn(data);

        when(functionContextService.save(any())).thenAnswer((Answer<FunctionContext>) invocation -> {
            Object[] args = invocation.getArguments();
            return (FunctionContext) args[0];
        });

        FunctionContext fc = (FunctionContext) functionServiceFacade.execute(functionName, context, "POST");
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
        NullPointerException exception = assertThrows(NullPointerException.class, () -> {
            functionServiceFacade.execute(null, null, Maps.newHashMap(), null);
        });
        assertEquals("functionKey can't be null", exception.getMessage());
    }

    /**
     * Test in progress
     */
    @Test
    public void executeWithFunctionKeyAndNullId() {
        NullPointerException exception = assertThrows(NullPointerException.class, () -> {
            functionServiceFacade.execute("Any key", null, Maps.newHashMap(), null);
        });
        assertEquals("idOrKey can't be null", exception.getMessage());
    }

    @Test
    public void executeUnknownWithIdOrKey() {
        final String functionKey = UNKNOWN_KEY;

        when(xmEntityService.findStateProjectionById(key)).thenReturn(getProjection(key));

        when(xmEntitySpecService.findEntityFunction(xmEntityTypeKey, functionKey, null))
            .thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            functionServiceFacade.execute(functionKey, key, Maps.newHashMap(), null);
        });
        assertEquals("Function not found for entity type key " + xmEntityTypeKey
            + " and function key: " + functionKey, exception.getMessage());

    }

    @Test
    @Disabled("Ignored until state mapping behaviour will be agreed")
    public void executeFailsIfStatesNotMatched() {
        FunctionSpec spec = getFunctionSpec(Boolean.FALSE);
        spec.setAllowedStateKeys(Lists.newArrayList("SOME-STATE"));

        Map<String, Object> context = Maps.newHashMap();
        context.put("key1", "val1");

        Map<String, Object> data = Maps.newHashMap();
        data.put("KEY1", "VAL1");

        when(xmEntityService.findStateProjectionById(key)).thenReturn(getProjection(key));

        when(xmEntitySpecService.findEntityFunction(xmEntityTypeKey, functionName, "GET"))
            .thenReturn(Optional.of(spec));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            functionServiceFacade.execute(functionName, key, Maps.newHashMap(), "GET");
        });
        assertEquals("Function call forbidden for current state", exception.getMessage());

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

        when(xmEntitySpecService.findEntityFunction(xmEntityTypeKey, functionName, "GET"))
            .thenReturn(Optional.of(spec));

        when(functionExecutorService.execute(functionName, key, xmEntityTypeKey, context))
            .thenReturn(data);

        when(functionContextService.save(any())).thenReturn(new FunctionContext());

        FunctionContext fc = functionServiceFacade.execute(functionName, key, context, "GET");
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

        when(xmEntitySpecService.findEntityFunction(xmEntityTypeKey, functionName, "GET"))
            .thenReturn(Optional.of(spec));

        when(functionExecutorService.execute(functionName, key, xmEntityTypeKey, context))
            .thenReturn(data);

        when(functionContextService.save(any())).thenReturn(new FunctionContext());

        FunctionContext fc = functionServiceFacade.execute(functionName, key, context, "GET");
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

        when(xmEntitySpecService.findEntityFunction(xmEntityTypeKey, functionName, "GET"))
            .thenReturn(Optional.of(spec));

        when(functionExecutorService.execute(functionName, key, xmEntityTypeKey, context))
            .thenReturn(data);

        when(functionContextService.save(any())).then(AdditionalAnswers.returnsFirstArg());

        FunctionContext fc = functionServiceFacade.execute(functionName, key, context, "GET");
        assertThat(fc.getTypeKey()).isEqualTo(functionName);
        assertThat(fc.getKey()).contains(functionName);
        assertThat(fc.getData().keySet()).containsSequence(data.keySet().toArray(new String[0]));

        verify(functionContextService, Mockito.times(1)).save(any());
    }

    @Test
    public void passStateValidationIfNoStateMapping() {
        FunctionSpec spec = new FunctionSpec();
        XmEntityStateProjection p = getProjection(SELF).get();
        functionServiceFacade.assertCallAllowedByState(spec, p);
    }

    @Test
    public void passStateValidationIfNONEStateIsSet() {
        FunctionSpec spec = new FunctionSpec();
        spec.setAllowedStateKeys(Lists.newArrayList(XmEntityFunctionServiceFacade.NONE));
        XmEntityStateProjection p = getProjection(SELF).get();
        functionServiceFacade.assertCallAllowedByState(spec, p);
    }

    @Test
    public void passStateValidationIfStatesMatches() {
        FunctionSpec spec = new FunctionSpec();
        spec.setAllowedStateKeys(Lists.newArrayList("STATE"));
        XmEntityStateProjection p = getProjection(SELF).get();
        functionServiceFacade.assertCallAllowedByState(spec, p);
    }

    @Test
    public void failStateValidationIfStatesNotMatches() {
        FunctionSpec spec = new FunctionSpec();
        spec.setAllowedStateKeys(Lists.newArrayList("XX-STATE-XX"));
        XmEntityStateProjection p = getProjection(SELF).get();
        functionServiceFacade.assertCallAllowedByState(spec, p);
    }

    @Test
    public void functionWithBinaryDataResult() {
        //GIVEN
        Map<String, Object> context = Maps.newHashMap();
        when(xmEntityService.findStateProjectionById(key)).thenReturn(getProjection(key));

        FunctionSpec spec = getFunctionSpec(Boolean.FALSE);
        spec.setKey("FUNCTION_WITH_BINARY_RESULT");
        spec.setBinaryDataField("contentBytes");
        spec.setBinaryDataType("application/pdf");

        when(xmEntitySpecService.findEntityFunction(xmEntityTypeKey, "FUNCTION_WITH_BINARY_RESULT", "GET"))
            .thenReturn(Optional.of(spec));

        Map<String, Object> functionResult = new HashMap<>();
        functionResult.put("contentBytes", "kind_of_pdf_content".getBytes());

        when(functionExecutorService.execute("FUNCTION_WITH_BINARY_RESULT", key, xmEntityTypeKey, context))
            .thenReturn(functionResult);

        //WHEN
        FunctionContext result = functionServiceFacade.execute("FUNCTION_WITH_BINARY_RESULT", key, context, "GET");

        //THEN
        assertTrue(result.isBinaryData());
        assertArrayEquals("kind_of_pdf_content".getBytes(), (byte[]) result.functionResult());
    }

    @Test
    public void executeWithPathDefinedInFunctionSpec() {
        FunctionSpec spec = new FunctionSpec();
        spec.setKey("FUNCTION_WITH_PATH");
        spec.setPath("my/api-path/with/{param1}/and/{param2}");

        //GIVEN
        when(xmEntitySpecService.findFunction("my/api-path/with/101/and/value2", "POST"))
                .thenReturn(Optional.of(spec));

        when(functionExecutorService.execute(eq("FUNCTION_WITH_PATH"), any(), eq("POST")))
                .thenAnswer(invocation -> new HashMap<>(invocation.getArgument(1)));

        //WHEN input null
        FunctionResultContext result = (FunctionResultContext) functionServiceFacade.execute("my/api-path/with/101/and/value2", null, "POST");

        //THEN
        assertEquals(2, result.getData().size());
        assertEquals("101", result.getData().get("param1"));
        assertEquals("value2", result.getData().get("param2"));

        //WHEN input not null
        HashMap<String, Object> input = new HashMap<>();
        input.put("key", "value");
        result = (FunctionResultContext) functionServiceFacade.execute("my/api-path/with/101/and/value2", input, "POST");

        //THEN
        assertEquals(3, result.getData().size());
        assertEquals("value", result.getData().get("key"));
        assertEquals("101", result.getData().get("param1"));
        assertEquals("value2", result.getData().get("param2"));
    }

    @Test
    public void executeAnonymousWithPathDefinedInFunctionSpec() {
        FunctionSpec spec = new FunctionSpec();
        spec.setKey("FUNCTION_WITH_PATH");
        spec.setPath("my/api-path/with/{param1}/and/{param2}");
        spec.setAnonymous(true);

        //GIVEN
        when(xmEntitySpecService.findFunction("my/api-path/with/101/and/value2", "POST"))
                .thenReturn(Optional.of(spec));

        when(functionExecutorService.executeAnonymousFunction(eq("FUNCTION_WITH_PATH"), any(), eq("POST")))
                .thenAnswer(invocation -> new HashMap<>(invocation.getArgument(1)));

        //WHEN
        FunctionContext result = (FunctionContext) functionServiceFacade.executeAnonymous("my/api-path/with/101/and/value2", new HashMap<>(), "POST");

        //THEN
        assertEquals(2, result.getData().size());
        assertEquals("101", result.getData().get("param1"));
        assertEquals("value2", result.getData().get("param2"));
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

    @Test
    public void noValidationOnInvalidFunctionInputWhenValidationDisabled() {
        noValidation(false);
    }

    @Test
    public void validationWhenGloballyEnabled() {
        xmEntityTenantConfig.getEntityFunctions().setValidateFunctionInput(true);
        validatedSuccess(null);
    }

    @Test
    public void excludeValidationWhenGloballyEnabled() {
        xmEntityTenantConfig.getEntityFunctions().setValidateFunctionInput(true);
        noValidation(false);
    }

    @Test
    public void validationSuccessOnValidFunctionInputWhenValidationEnabled() {
        validatedSuccess(true);
    }

    public void noValidation(Boolean validateFunctionInput) {
        FunctionSpec spec = generateFunctionSpec(validateFunctionInput);
        when(xmEntitySpecService.findFunction(VALIDATION_FUNCTION, "POST")).thenReturn(Optional.of(spec));
        functionServiceFacade.execute(VALIDATION_FUNCTION, Map.of("numberArgument", "stringValue"), "POST");
        verifyNoMoreInteractions(jsonValidationService);

        spec.setWithEntityId(true);
        when(xmEntitySpecService.findEntityFunction(xmEntityTypeKey, VALIDATION_FUNCTION, "GET")).thenReturn(Optional.of(spec));
        when(xmEntityService.findStateProjectionById(SELF)).thenReturn(getProjection(SELF));
        functionServiceFacade.execute(VALIDATION_FUNCTION, SELF, Map.of("numberArgument", "stringValue"), "GET");
        verifyNoMoreInteractions(jsonValidationService);
    }

    private void validatedSuccess(Boolean validateFunctionInput) {
        FunctionSpec spec = generateFunctionSpec(validateFunctionInput);
        when(xmEntitySpecService.findFunction(VALIDATION_FUNCTION, "POST")).thenReturn(Optional.of(spec));
        Map<String, Object> functionInput = Map.of("numberArgument", 2);
        functionServiceFacade.execute(VALIDATION_FUNCTION, functionInput, "POST");

        spec.setWithEntityId(true);
        when(xmEntitySpecService.findEntityFunction(xmEntityTypeKey, VALIDATION_FUNCTION, "GET")).thenReturn(Optional.of(spec));
        when(xmEntityService.findStateProjectionById(SELF)).thenReturn(getProjection(SELF));
        functionServiceFacade.execute(VALIDATION_FUNCTION, SELF, functionInput, "GET");

        verify(jsonValidationService, times(2)).assertJson(eq(functionInput), eq(spec.getInputSpec()));
    }

    @Test
    public void validationFailOnInvalidFunctionInputWhenValidationEnabled() {
        String exceptionMessage = "$.numberArgument: string found, number expected";

        FunctionSpec spec = generateFunctionSpec(true);
        when(xmEntitySpecService.findFunction(VALIDATION_FUNCTION, "POST")).thenReturn(Optional.of(spec));
        Map<String, Object> functionInput = Map.of("numberArgument", "stringValue");

        JsonValidationService.InvalidJsonException exception = assertThrows(JsonValidationService.InvalidJsonException.class, () -> {
            functionServiceFacade.execute(VALIDATION_FUNCTION, functionInput, "POST");
        });
        assertEquals(exceptionMessage, exception.getMessage());
        verify(jsonValidationService).assertJson(eq(functionInput), eq(spec.getInputSpec()));

    }

    @Test
    public void validationFailOnInvalidFunctionWithEntityIdInputWhenValidationEnabled() {
        String exceptionMessage = "$.numberArgument: string found, number expected";

        FunctionSpec spec = generateFunctionSpec(true);
        Map<String, Object> functionInput = Map.of("numberArgument", "stringValue");
        spec.setWithEntityId(true);
        when(xmEntitySpecService.findEntityFunction(xmEntityTypeKey, VALIDATION_FUNCTION, "GET")).thenReturn(Optional.of(spec));
        when(xmEntityService.findStateProjectionById(SELF)).thenReturn(getProjection(SELF));

        JsonValidationService.InvalidJsonException exception = assertThrows(JsonValidationService.InvalidJsonException.class, () -> {
            functionServiceFacade.execute(VALIDATION_FUNCTION, SELF, functionInput, "GET");
        });
        assertEquals(exceptionMessage, exception.getMessage());
        verify(jsonValidationService).assertJson(eq(functionInput), eq(spec.getInputSpec()));
    }

    @NotNull
    private FunctionSpec generateFunctionSpec(Boolean validateFunctionInput) {
        FunctionSpec spec = new FunctionSpec();
        spec.setKey(VALIDATION_FUNCTION);
        spec.setValidateFunctionInput(validateFunctionInput);
        // language=JSON
        String inputSpec = "{\n" +
                "              \"type\": \"object\",\n" +
                "              \"properties\": {\n" +
                "                  \"numberArgument\": {\"type\": \"number\" }\n" +
                "              }\n" +
                "            }";
        spec.setInputSpec(inputSpec);
        return spec;
    }
}
