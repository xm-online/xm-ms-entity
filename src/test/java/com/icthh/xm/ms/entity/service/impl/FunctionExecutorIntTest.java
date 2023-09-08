package com.icthh.xm.ms.entity.service.impl;

import com.icthh.xm.commons.lep.XmLepScriptConfigServerResourceLoader;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.ms.entity.AbstractSpringBootTest;
import com.icthh.xm.ms.entity.lep.LepContext;
import com.icthh.xm.ms.entity.service.FunctionExecutorService;
import lombok.SneakyThrows;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Map;

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_AUTH_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Transactional
public class FunctionExecutorIntTest extends AbstractSpringBootTest {

    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Autowired
    private XmAuthenticationContextHolder authContextHolder;

    @Autowired
    private LepManager lepManager;

    @Autowired
    private XmLepScriptConfigServerResourceLoader leps;

    @Autowired
    private FunctionExecutorService functionExecutorService;

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

    public static volatile long startTime;
    public static volatile long endTime;

    @Test
    @Transactional
    @SneakyThrows
    public void testLepContextCast() {
        String functionPrefix = "/config/tenants/RESINTTEST/entity/lep/function/";
        String functionKey = "TEST_RUN_PERFORMANCE";
        String funcKey = functionPrefix + "Function$$TEST_RUN_PERFORMANCE$$tenant.groovy";
        String function = "return ['result':'OK']\n";
        leps.onRefresh(funcKey, function);
        Map<String, Object> result = functionExecutorService.execute(functionKey, Map.of(), null);
        assertEquals("OK", result.get("result"));

        startTime = System.nanoTime();
        for(int i = 0; i < 50_000; i++) {
            functionExecutorService.execute(functionKey, Map.of(), null);
        }
        endTime = System.nanoTime();
        System.out.println(Duration.ofNanos(endTime - startTime).toMillis());

        leps.onRefresh(funcKey, null);
    }
}
