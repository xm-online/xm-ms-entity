package com.icthh.xm.ms.entity.lep;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.eq;

import com.icthh.lep.api.ContextScopes;
import com.icthh.lep.api.ContextsHolder;
import com.icthh.lep.api.LepInvocationCauseException;
import com.icthh.lep.api.LepManagerService;
import com.icthh.lep.api.LepMethod;
import com.icthh.lep.api.MethodSignature;
import com.icthh.lep.api.ScopedContext;
import com.icthh.lep.commons.UrlLepResourceKey;
import com.icthh.lep.groovy.DefaultScriptNameLepResourceKeyMapper;
import com.icthh.lep.groovy.GroovyScriptRunner;
import com.icthh.lep.groovy.LazyGroovyScriptEngineProviderStrategy;
import com.icthh.lep.groovy.ScriptNameLepResourceKeyMapper;
import com.icthh.lep.groovy.StrategyGroovyLepExecutor;
import com.icthh.xm.ms.entity.config.tenant.TenantInfo;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * The {@link XmGroovyExecutionStrategyUnitTest} class.
 */
@RunWith(SpringRunner.class)
public class XmGroovyExecutionStrategyUnitTest {

    @Autowired
    private ResourceLoader resourceLoader;

    private XmGroovyExecutionStrategy execStrategy;

    private Supplier<GroovyScriptRunner> resourceExecutorSupplier;

    @Before
    public void before() {
        this.execStrategy = new XmGroovyExecutionStrategy();

        ScriptNameLepResourceKeyMapper mapper = new DefaultScriptNameLepResourceKeyMapper();
        LazyGroovyScriptEngineProviderStrategy providerStrategy = new LazyGroovyScriptEngineProviderStrategy(
            mapper);
        GroovyScriptRunner scriptRunner = new StrategyGroovyLepExecutor(mapper, providerStrategy,
                                                                        execStrategy);
        resourceExecutorSupplier = () -> scriptRunner;
    }

    private LepManagerService buildLepManagerService(String tenantName) {
        TenantInfo tenantInfo = Mockito.mock(TenantInfo.class);
        Mockito.when(tenantInfo.getTenant()).thenReturn(tenantName);

        // Thread context
        ScopedContext threadContext = Mockito.mock(ScopedContext.class);
        Mockito.when(threadContext.getValue(eq(XmLepConstants.CONTEXT_KEY_TENANT),
                                            eq(TenantInfo.class))).thenReturn(tenantInfo);

        // Execution context
        ScopedContext executionContext = Mockito.mock(ScopedContext.class);
        Map<String, Object> executionContextValues = new HashMap<>();
        executionContextValues.put(XmLepScriptConstants.BINDING_KEY_TENANT, tenantInfo);
        executionContextValues.put(XmLepScriptConstants.BINDING_KEY_TENANT_NAME, tenantInfo.getTenant());
        Mockito.when(executionContext.getValues()).thenReturn(executionContextValues);

        ContextsHolder holder = Mockito.mock(ContextsHolder.class);
        Mockito.when(holder.getContext(eq(ContextScopes.THREAD))).thenReturn(threadContext);
        Mockito.when(holder.getContext(eq(ContextScopes.EXECUTION))).thenReturn(executionContext);

        XmClasspathLepResourceService resourceService = new XmClasspathLepResourceService(holder);
        resourceService.setResourceLoader(resourceLoader);

        LepManagerService managerService = Mockito.mock(LepManagerService.class);
        Mockito.when(managerService.getResourceService()).thenReturn(resourceService);
        Mockito.when(managerService.getContext(eq(ContextScopes.THREAD))).thenReturn(threadContext);
        Mockito.when(managerService.getContext(eq(ContextScopes.EXECUTION))).thenReturn(executionContext);
        return managerService;
    }

    private LepMethod buildEmptyLepMethod() {
        MethodSignature methodSignature = Mockito.mock(MethodSignature.class);
        Mockito.when(methodSignature.getParameterNames()).thenReturn(new String[0]);
        Mockito.when(methodSignature.getParameterTypes()).thenReturn(new Class<?>[0]);

        LepMethod lepMethod = Mockito.mock(LepMethod.class);
        Mockito.when(lepMethod.getMethodArgValues()).thenReturn(new Object[0]);
        Mockito.when(lepMethod.getMethodSignature()).thenReturn(methodSignature);
        return lepMethod;
    }

    @Test
    public void simpleDefaultScriptValidExecution() throws LepInvocationCauseException {
        LepManagerService managerService = buildLepManagerService("super");

        LepMethod lepMethod = buildEmptyLepMethod();

        UrlLepResourceKey compositeKey = UrlLepResourceKey
            .valueOfUrlResourcePath("/general/Script.groovy");

        // execute script
        Object result = execStrategy.executeLepResource(compositeKey, lepMethod, managerService,
                                                        resourceExecutorSupplier);
        assertNotNull(result);
        assertEquals("Script.groovy default", result);
    }

    @Test
    public void simpleTenantScriptValidExecution() throws LepInvocationCauseException {
        LepManagerService managerService = buildLepManagerService("super");

        LepMethod lepMethod = buildEmptyLepMethod();

        UrlLepResourceKey compositeKey = UrlLepResourceKey
            .valueOfUrlResourcePath("/general/ScriptWithTenant.groovy");

        // execute script
        Object result = execStrategy.executeLepResource(compositeKey, lepMethod, managerService,
                                                        resourceExecutorSupplier);
        assertNotNull(result);
        assertEquals("ScriptWithTenant.groovy tenant, tenant: super", result);
    }

    @Test
    public void simpleAroundScriptValidExecution() throws LepInvocationCauseException {
        LepManagerService managerService = buildLepManagerService("super");

        LepMethod lepMethod = buildEmptyLepMethod();

        UrlLepResourceKey compositeKey = UrlLepResourceKey
            .valueOfUrlResourcePath("/general/ScriptWithAround.groovy");

        // execute script
        Object result = execStrategy.executeLepResource(compositeKey, lepMethod, managerService,
                                                        resourceExecutorSupplier);
        assertNotNull(result);
        assertEquals("ScriptWithAround.groovy around, tenant: super", result);
    }

    @Test
    public void simpleBeforeAfterScriptValidExecution() throws LepInvocationCauseException {
        LepManagerService managerService = buildLepManagerService("super");

        LepMethod lepMethod = buildEmptyLepMethod();

        UrlLepResourceKey compositeKey = UrlLepResourceKey
            .valueOfUrlResourcePath("/general/ScriptWithBeforeAfter.groovy");

        // execute script
        Object result = execStrategy.executeLepResource(compositeKey, lepMethod, managerService,
                                                        resourceExecutorSupplier);
        assertNotNull(result);
        assertEquals("ScriptWithBeforeAfter.groovy default", result);
    }

    @Test
    public void checkDefaultScriptBindingParams() throws LepInvocationCauseException {
        LepManagerService managerService = buildLepManagerService("super");

        MethodSignature methodSignature = Mockito.mock(MethodSignature.class);
        Mockito.when(methodSignature.getParameterNames()).thenReturn(new String[] {"name", "age"});

        LepMethod lepMethod = Mockito.mock(LepMethod.class);
        Mockito.when(lepMethod.getMethodArgValues()).thenReturn(new Object[] {"John Doe", 23});
        Mockito.when(lepMethod.getMethodSignature()).thenReturn(methodSignature);

        UrlLepResourceKey compositeKey = UrlLepResourceKey
            .valueOfUrlResourcePath("/general/CheckBindingParams.groovy");

        // execute script
        execStrategy.executeLepResource(compositeKey, lepMethod, managerService,
                                        resourceExecutorSupplier);
    }

}
