package com.icthh.xm.ms.entity.lep;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.shaded.org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_AUTH_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Transactional
public class LepEntityRepositoryIntTest extends AbstractSpringBootTest  {
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
    public void testFindAllByFields() {
        String functionPrefix = "/config/tenants/RESINTTEST/entity/lep/function/";
        String functionKey = "FIND-ALL-BY-FIELDS";
        String funcKey = functionPrefix + "Function$$FIND_ALL_BY_FIELDS$$tenant.groovy";

        InputStream cfgInputStream = new ClassPathResource("config/testlep/Function$$FIND_All_BY_FIELDS$$around.groovy").getInputStream();
        String function = IOUtils.toString(cfgInputStream, UTF_8);

        leps.onRefresh(funcKey, function);
        Map<String, Object> result = (Map<String, Object>) functionExecutorServiceImpl.execute(functionKey, Map.of(), null);
        List<Map<String, Object>> resultEntities = (List) result.get("results");

        assertEquals(1, resultEntities.size());
        assertEquals(2, resultEntities.get(0).size());
        assertEquals("TEST_EXPORT_2", resultEntities.get(0).get("typeKey"));
        assertTrue(resultEntities.get(0).get("id") instanceof Long);

        leps.onRefresh(funcKey, null);
    }
}
