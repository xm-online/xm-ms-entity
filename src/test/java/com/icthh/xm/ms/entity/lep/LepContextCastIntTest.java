package com.icthh.xm.ms.entity.lep;

import com.icthh.xm.commons.lep.XmLepScriptConfigServerResourceLoader;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.ms.entity.AbstractSpringBootTest;
import com.icthh.xm.ms.entity.service.XmEntityFunctionExecutorService;
import lombok.SneakyThrows;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_AUTH_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


@Transactional
public class LepContextCastIntTest extends AbstractSpringBootTest {

    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Autowired
    private XmAuthenticationContextHolder authContextHolder;

    @Autowired
    private LepManager lepManager;

    @Autowired
    private XmLepScriptConfigServerResourceLoader leps;

    @Autowired
    private XmEntityFunctionExecutorService functionExecutorServiceImpl;

    @BeforeTransaction
    public void beforeTransaction() {
        TenantContextUtils.setTenant(tenantContextHolder, "RESINTTEST");
    }

    @SneakyThrows
    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        lepManager.beginThreadContext(ctx -> {
            ctx.setValue(THREAD_CONTEXT_KEY_TENANT_CONTEXT, tenantContextHolder.getContext());
            ctx.setValue(THREAD_CONTEXT_KEY_AUTH_CONTEXT, authContextHolder.getContext());
        });
    }

    @After
    public void tearDown() {
        lepManager.endThreadContext();
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
    }

    @Test
    @Transactional
    @SneakyThrows
    public void testLepContextCast() {
        String functionPrefix = "/config/tenants/RESINTTEST/entity/lep/function/";
        String functionKey = "LEP-CONTEXT-TEST";
        String funcKey = functionPrefix + "Function$$LEP_CONTEXT_TEST$$tenant.groovy";
        String function = "import com.icthh.xm.ms.entity.lep.LepContext;\nLepContext context = lepContext\nreturn ['context':context]";
        leps.onRefresh(funcKey, function);
        Map<String, Object> result = functionExecutorServiceImpl.execute(functionKey, Map.of(), null);
        assertTrue(result.get("context") instanceof LepContext);
        leps.onRefresh(funcKey, null);
    }

    @Test
    @Transactional
    @SneakyThrows
    public void testLepContextCastToMap() {
        String functionPrefix = "/config/tenants/RESINTTEST/entity/lep/function/";
        String functionKey = "LEP-CONTEXT-TEST";
        String funcKey = functionPrefix + "Function$$LEP_CONTEXT_TEST$$tenant.groovy";
        String function = "Map<String, Object> context = lepContext\nreturn ['context':context]";
        leps.onRefresh(funcKey, function);
        Map<String, Object> result = functionExecutorServiceImpl.execute(functionKey, Map.of(), null);
        Object context = result.get("context");
        assertEquals("GroovyMapLepContextWrapper", context.getClass().getSimpleName());
        assertTrue(context instanceof Map);
        leps.onRefresh(funcKey, null);
    }

}
