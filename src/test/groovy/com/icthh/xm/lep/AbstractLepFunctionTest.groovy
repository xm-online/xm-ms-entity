package com.icthh.xm.lep


import com.icthh.xm.commons.lep.spring.SpringLepManager
import com.icthh.xm.commons.security.XmAuthenticationContext
import com.icthh.xm.commons.tenant.TenantContext
import com.icthh.xm.commons.tenant.TenantKey
import com.icthh.xm.lep.api.ContextScopes
import com.icthh.xm.lep.api.LepProcessingEvent
import com.icthh.xm.lep.api.LepProcessingListener
import com.icthh.xm.lep.api.ScopedContext
import com.icthh.xm.ms.entity.AbstractSpringBootTest
import com.icthh.xm.ms.entity.EntityApp
import com.icthh.xm.ms.entity.config.SecurityBeanOverrideConfiguration
import com.icthh.xm.ms.entity.config.tenant.WebappTenantOverrideConfiguration
import com.icthh.xm.ms.entity.service.FunctionExecutorService
import org.junit.After
import org.junit.Before
import org.junit.experimental.categories.Category
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_AUTH_CONTEXT
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT
import static com.icthh.xm.ms.entity.lep.LepXmEntityMsConstants.*
import static org.mockito.Mockito.when

/**
 * Base test for lep related unit tests.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = [
        EntityApp.class,
        SecurityBeanOverrideConfiguration.class,
        WebappTenantOverrideConfiguration.class
])
@ActiveProfiles('leptest')
@Category(AbstractSpringBootTest.class)
abstract class AbstractLepFunctionTest {


    @Autowired
    protected SpringLepManager lepManager

    @Mock
    protected TenantContext tenantContext

    @Mock
    protected XmAuthenticationContext authenticationContext

    @Autowired
    protected FunctionExecutorService functionExecutor

    private beforeLepListener = createListener()

    @Before
    final void initInternal() {
        MockitoAnnotations.initMocks(this)

        lepManager.beginThreadContext({
            ctx ->
                ctx.setValue(THREAD_CONTEXT_KEY_TENANT_CONTEXT, tenantContext)
                ctx.setValue(THREAD_CONTEXT_KEY_AUTH_CONTEXT, authenticationContext)
        })

        lepManager.registerProcessingListener(beforeLepListener)
    }


    LepProcessingListener createListener() {
        { LepProcessingEvent e ->

            if (e instanceof LepProcessingEvent.BeforeExecutionEvent) {
                ScopedContext context = lepManager.getContext(ContextScopes.EXECUTION)

                Map<String, Object> services = injectServices()
                if (services) {
                    println "BeforeExecutionEvent: override $BINDING_KEY_SERVICES in lepContext: $services}"
                    context.setValue(BINDING_KEY_SERVICES, services)
                }

                Map<String, Object> repositories = injectRepositories()
                if (repositories) {
                    println "BeforeExecutionEvent: override $BINDING_KEY_REPOSITORIES in lepContext: $repositories}"
                    context.setValue(BINDING_KEY_REPOSITORIES, repositories)
                }

                Map<String, Object> templates = injectTemplates()
                if (templates) {
                    println "BeforeExecutionEvent: override $BINDING_KEY_TEMPLATES in lepContext: $templates}"
                    context.setValue(BINDING_KEY_REPOSITORIES, templates)
                }
            }
        }
    }

    @After
    void destroy() {
        lepManager.unregisterProcessingListener(beforeLepListener)
    }

    /**
     * Sets tenant to tenantContext.
     * @param tenant
     */
    protected void mockTenant(String tenant) {
        when(tenantContext.getTenantKey()).thenReturn(Optional.of(TenantKey.valueOf(tenant)))
    }

    /**
     * Injects map of templates which will be accessible from LEPs as lepContext.templates.<templateName>
     * @return Services map with keys defined in {@link com.icthh.xm.ms.entity.lep.LepXmEntityMsConstants}.
     */
    protected Map<String, Object> injectTemplates() {
        null
    }

    /**
     * Injects map of repositories which will be accessible from LEPs as lepContext.repositories.<repositoryName>
     * @return Services map with keys defined in {@link com.icthh.xm.ms.entity.lep.LepXmEntityMsConstants}.
     */
    protected Map<String, Object> injectRepositories() {
        null
    }

    /**
     * Injects map of services which will be accessible from LEPs as lepContext.services.<serviceName>
     * @return Services map with keys defined in {@link com.icthh.xm.ms.entity.lep.LepXmEntityMsConstants}.
     */
    protected Map<String, Object> injectServices() {
        null
    }

}
