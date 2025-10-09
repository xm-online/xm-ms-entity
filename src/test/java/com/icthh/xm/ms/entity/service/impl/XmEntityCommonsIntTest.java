package com.icthh.xm.ms.entity.service.impl;

import static com.google.common.collect.ImmutableMap.of;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_AUTH_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.commons.lep.XmLepScriptConfigServerResourceLoader;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.ms.entity.AbstractJupiterSpringBootTest;
import com.icthh.xm.ms.entity.service.XmEntityFunctionExecutorService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.Map;

@Slf4j
public class XmEntityCommonsIntTest extends AbstractJupiterSpringBootTest {

    @Autowired
    private XmEntityFunctionExecutorService functionService;


    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Autowired
    private XmAuthenticationContextHolder authContextHolder;

    @Autowired
    private LepManager lepManager;

    @Autowired
    private XmLepScriptConfigServerResourceLoader leps;

    @BeforeTransaction
    public void beforeTransaction() {
        TenantContextUtils.setTenant(tenantContextHolder, "RESINTTEST");
    }

    @SneakyThrows
    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);

        lepManager.beginThreadContext(ctx -> {
            ctx.setValue(THREAD_CONTEXT_KEY_TENANT_CONTEXT, tenantContextHolder.getContext());
            ctx.setValue(THREAD_CONTEXT_KEY_AUTH_CONTEXT, authContextHolder.getContext());
        });

    }

    void initLeps() {
        leps.onRefresh("/config/tenants/RESINTTEST/entity/lep/function/Function$$NAME$$tenant.groovy", loadFile("config/testlep/Function$$NAME$$tenant.groovy"));
        leps.onRefresh("/config/tenants/RESINTTEST/entity/lep/commons/xm/path/to/commons/Commons$$trololo$$around.groovy", loadFile("config/testlep/Commons$$trololo$$around.groovy"));
    }

    @SneakyThrows
    public static String loadFile(String path) {
        InputStream cfgInputStream = new ClassPathResource(path).getInputStream();
        return IOUtils.toString(cfgInputStream, UTF_8);
    }

    @AfterEach
    public void tearDown() {
        lepManager.endThreadContext();
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
    }


    @Test
    @Transactional
    public void testCommons() {
        initLeps();
        Object result = functionService.execute("NAME", of(), null);
        Map<String, Object> name = new ObjectMapper().convertValue(result, new TypeReference<>() {});
        log.info("RESULT {}", name);
        assertThat(name.get("result")).isEqualTo("RESULT [1, 2, 5] | COMMON_ARGUMENT");
    }



}
