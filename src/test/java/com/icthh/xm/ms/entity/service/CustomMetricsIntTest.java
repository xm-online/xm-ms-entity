package com.icthh.xm.ms.entity.service;

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_AUTH_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.icthh.xm.commons.lep.XmLepScriptConfigServerResourceLoader;
import com.icthh.xm.commons.security.XmAuthenticationContext;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.ms.entity.AbstractJupiterSpringBootTest;
import com.icthh.xm.ms.entity.service.metrics.CustomMetricsConfiguration;
import com.icthh.xm.ms.entity.web.rest.XmEntityResource;
import java.io.InputStream;
import java.util.Optional;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.transaction.BeforeTransaction;

/**
 * Test class for the XmEntityResource REST controller.
 *
 * @see XmEntityResource
 */
@Slf4j
@WithMockUser(authorities = {"SUPER-ADMIN"})
public class CustomMetricsIntTest extends AbstractJupiterSpringBootTest {

    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Mock
    private XmAuthenticationContextHolder authContextHolder;

    @Mock
    private XmAuthenticationContext context;

    @Autowired
    private LepManager lepManager;

    @Autowired
    private MetricRegistry metricRegistry;

    @Autowired
    private CustomMetricsConfiguration customMetricsConfiguration;

    @Autowired
    private XmLepScriptConfigServerResourceLoader lepResourceLoader;

    @BeforeTransaction
    public void beforeTransaction() {
        TenantContextUtils.setTenant(tenantContextHolder, "RESINTTEST");
    }

    @SneakyThrows
    @BeforeEach
    public void setup() {

        TenantContextUtils.setTenant(tenantContextHolder, "RESINTTEST");

        MockitoAnnotations.initMocks(this);
        when(authContextHolder.getContext()).thenReturn(context);
        when(context.getUserKey()).thenReturn(Optional.of("userKey"));

        lepManager.beginThreadContext(ctx -> {
            ctx.setValue(THREAD_CONTEXT_KEY_TENANT_CONTEXT, tenantContextHolder.getContext());
            ctx.setValue(THREAD_CONTEXT_KEY_AUTH_CONTEXT, authContextHolder.getContext());
        });

    }

    @AfterEach
    public void tearDown() {
        lepManager.endThreadContext();
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
    }

    @SneakyThrows
    public static String loadFile(String path) {
        InputStream cfgInputStream = new ClassPathResource(path).getInputStream();
        return IOUtils.toString(cfgInputStream, UTF_8);
    }

    @Test
    @SneakyThrows
    public void testCustomMetrics() {

        lepResourceLoader.onRefresh("/config/tenants/RESINTTEST/entity/lep/metrics/Metric$$my$every$call$metric$$around.groovy",
                                    loadFile("config/testlep/metrics/Metric$$my$every$call$metric$$around.groovy"));
        lepResourceLoader.onRefresh("/config/tenants/RESINTTEST/entity/lep/metrics/Metric$$my$periodic$metric$$around.groovy",
                                    loadFile("config/testlep/metrics/Metric$$my$periodic$metric$$around.groovy"));

        customMetricsConfiguration.onRefresh("/config/tenants/RESINTTEST/entity/metrics.yml",
                                             loadFile("config/metrics.yml"));

        Gauge everyTimeMetric = metricRegistry.getGauges().get("custom.metrics.resinttest.my.every.call.metric");
        Gauge periodicMetric = metricRegistry.getGauges().get("custom.metrics.resinttest.my.periodic.metric");

        assertNull(periodicMetric.getValue());
        assertNull(periodicMetric.getValue());

        waitValue(periodicMetric, 1);
        assertEquals(1, periodicMetric.getValue());
        assertEquals(1, periodicMetric.getValue());
        assertEquals(1, periodicMetric.getValue());
        waitValue(periodicMetric, 2);
        assertEquals(2, periodicMetric.getValue());
        long time = waitValue(periodicMetric, 3);
        assertEquals(3, periodicMetric.getValue());
        log.info("result time -> {}", time);
        assertTrue(time + " less than 2000", (time/1000_000L) > 1900);
        assertTrue(time + " more than 2100", (time/1000_000L) < 2100);

        assertEquals(1, everyTimeMetric.getValue());
        assertEquals(2, everyTimeMetric.getValue());
        assertEquals(3, everyTimeMetric.getValue());
    }

    private long waitValue(Gauge periodicMetric, Integer value) {
        long startTime = System.nanoTime();
        await().atMost(5, SECONDS).until(() -> value.equals(periodicMetric.getValue()));
        return System.nanoTime() - startTime;
    }

}
