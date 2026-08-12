package com.icthh.xm.ms.entity.lep;

import com.icthh.xm.commons.lep.XmLepScriptConfigServerResourceLoader;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.ms.entity.AbstractJupiterSpringBootTest;
import com.icthh.xm.ms.entity.service.XmEntityFunctionExecutorService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_AUTH_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Pins the groovy property binding of {@code com.icthh.xm.ms.entity.domain.ext.IdOrKey}, which declares both
 * {@code getId()}/{@code isId()} and {@code getKey()}/{@code isKey()}: without
 * {@code groovy.runtime.metaclass...IdOrKeyMetaClass} groovy 5 binds {@code idOrKey.id} and
 * {@code idOrKey.key} to the boolean getters, and every tenant lep reading them gets {@code true}/
 * {@code false} instead of the value. All the expectations below are the groovy 2 behaviour.
 */
@Slf4j
@Transactional
public class IdOrKeyPropertyIntTest extends AbstractJupiterSpringBootTest {

    private static final String FUNCTION_KEY = "LEP-IDORKEY-TEST";
    private static final String LEP_KEY =
        "/config/tenants/RESINTTEST/entity/lep/function/Function$$LEP_IDORKEY_TEST$$tenant.groovy";
    private static final String ID_OR_KEY = "com.icthh.xm.ms.entity.domain.ext.IdOrKey";

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

    @BeforeEach
    public void setup() {
        lepManager.beginThreadContext(ctx -> {
            ctx.setValue(THREAD_CONTEXT_KEY_TENANT_CONTEXT, tenantContextHolder.getContext());
            ctx.setValue(THREAD_CONTEXT_KEY_AUTH_CONTEXT, authContextHolder.getContext());
        });
    }

    @AfterEach
    public void tearDown() {
        leps.onRefresh(LEP_KEY, null);
        lepManager.endThreadContext();
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
    }

    @Test
    @Transactional
    @SneakyThrows
    public void testIdProperty() {
        Map<String, Object> result = executeLep(
            "def idOrKey = " + ID_OR_KEY + ".of(123L)\n"
                + "return ['id':idOrKey.id, 'getId':idOrKey.getId(), 'isId':idOrKey.isId(), 'key':idOrKey.key]");

        assertEquals(123L, result.get("id"));
        assertEquals(123L, result.get("getId"));
        assertEquals(true, result.get("isId"));
        assertNull(result.get("key"));
    }

    @Test
    @Transactional
    @SneakyThrows
    public void testKeyProperty() {
        Map<String, Object> result = executeLep(
            "def idOrKey = " + ID_OR_KEY + ".ofKey('KEY')\n"
                + "return ['key':idOrKey.key, 'getKey':idOrKey.getKey(), 'isKey':idOrKey.isKey(), 'id':idOrKey.id]");

        assertEquals("KEY", result.get("key"));
        assertEquals("KEY", result.get("getKey"));
        assertEquals(true, result.get("isKey"));
        assertNull(result.get("id"));
    }

    /**
     * Property write is resolved by the setter and was never broken - {@link com.icthh.xm.ms.entity.domain.ext.IdOrKey}
     * has none, so groovy writes the field directly. The check is here for the read back after the write.
     */
    @Test
    @Transactional
    @SneakyThrows
    public void testPropertyAssignment() {
        Map<String, Object> result = executeLep(
            "def idOrKey = " + ID_OR_KEY + ".of(123L)\n"
                + "idOrKey.id = 456L\n"
                + "return ['id':idOrKey.id]");

        assertEquals(456L, result.get("id"));
    }

    /**
     * The reflective property access ({@code properties}, {@code hasProperty}) must agree with {@code idOrKey.id}.
     */
    @Test
    @Transactional
    @SneakyThrows
    public void testPropertyIntrospection() {
        Map<String, Object> result = executeLep(
            "def idOrKey = " + ID_OR_KEY + ".of(123L)\n"
                + "return ['properties':idOrKey.properties.id, 'hasProperty':idOrKey.hasProperty('id').getProperty(idOrKey)]");

        assertEquals(123L, result.get("properties"));
        assertEquals(123L, result.get("hasProperty"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> executeLep(String script) {
        leps.onRefresh(LEP_KEY, script);
        Map<String, Object> result = (Map<String, Object>) functionExecutorServiceImpl
            .execute(FUNCTION_KEY, Map.of(), null);
        log.info("result: {}", result);
        return result;
    }
}
