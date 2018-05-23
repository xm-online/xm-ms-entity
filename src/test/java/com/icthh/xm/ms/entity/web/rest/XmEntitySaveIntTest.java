package com.icthh.xm.ms.entity.web.rest;

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_AUTH_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepScriptConfigServerResourceLoader.XM_MS_CONFIG_URL_PREFIX;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import com.icthh.xm.commons.lep.RouterResourceLoader;
import com.icthh.xm.commons.lep.XmLepResourceService;
import com.icthh.xm.commons.lep.XmLepScriptConfigServerResourceLoader;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.ms.entity.EntityApp;
import com.icthh.xm.ms.entity.config.LepConfiguration;
import com.icthh.xm.ms.entity.config.SecurityBeanOverrideConfiguration;
import com.icthh.xm.ms.entity.config.tenant.WebappTenantOverrideConfiguration;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.service.impl.XmEntityServiceImpl;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.Instant;
import java.util.HashMap;
import java.util.UUID;

@Slf4j
@RunWith(SpringRunner.class)
@WithMockUser(authorities = {"SUPER-ADMIN"})
@SpringBootTest(classes = {
    EntityApp.class,
    SecurityBeanOverrideConfiguration.class,
    WebappTenantOverrideConfiguration.class,
    LepConfiguration.class
})
public class XmEntitySaveIntTest {

    @Autowired
    private XmEntityServiceImpl xmEntityServiceImpl;
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
    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        lepManager.beginThreadContext(ctx -> {
            ctx.setValue(THREAD_CONTEXT_KEY_TENANT_CONTEXT, tenantContextHolder.getContext());
            ctx.setValue(THREAD_CONTEXT_KEY_AUTH_CONTEXT, authContextHolder.getContext());
        });
    }

    void initLeps() {
        String pattern = "/config/tenants/RESINTTEST/entity/lep/service/entity/";

        val testLeps = new String[]{
            "Save$$around.groovy",
            "Save$$TEST_SAVE_AND_PROCEED$$around.groovy",
            "Save$$TEST_SAVE_RUN$$around.groovy",
            "Delete$$around.groovy",
            "Delete$$TEST_DELETE_AND_PROCEED$$around.groovy",
            "Delete$$TEST_DELETE_RUN$$around.groovy"
        };

        for (val lep: testLeps) {
            leps.onRefresh(pattern + lep, loadFile("config/testlep/" + lep));
        }
    }

    @SneakyThrows
    public static String loadFile(String path) {
        InputStream cfgInputStream = new ClassPathResource(path).getInputStream();
        return IOUtils.toString(cfgInputStream, UTF_8);
    }

    @After
    @Override
    public void finalize() {
        lepManager.endThreadContext();
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
    }

    public static XmEntity createEntity(String typeKey) {
        val data = new HashMap<String, Object>();
        data.put("runGeneralScript", 0);
        data.put("runGeneralScriptDELETE", 0);
        data.put("runSaveProceed", 0);
        data.put("runSaveRun", 0);
        data.put("runDeleteProceed", 0);
        data.put("runDeleteRun", 0);
        val entity = new XmEntity()
            .key(UUID.randomUUID())
            .typeKey(typeKey)
            .startDate(Instant.now())
            .stateKey("STATE1")
            .name("DEFAULT_NAME")
            .description("DEFAULT_DESCRIPTION");
        entity.setData(data);
        return entity;
    }

    @Test
    @Transactional
    public void testLep() throws Exception {

        initLeps();

        XmEntity runSaveProceed = createEntity("TEST_SAVE_AND_PROCEED");
        XmEntity runSave = createEntity("TEST_SAVE_RUN");
        XmEntity runGeneralScript = createEntity("TEST_NOT_RUN_SAVE");

        xmEntityServiceImpl.save(runSaveProceed);
        xmEntityServiceImpl.save(runSave);
        xmEntityServiceImpl.save(runGeneralScript);

        assertThat(runSaveProceed.getData().get("runGeneralScript")).isEqualTo(1);
        assertThat(runSaveProceed.getData().get("runSaveProceed")).isEqualTo(1);
        assertThat(runSaveProceed.getData().get("runSaveRun")).isEqualTo(0);

        assertThat(runSave.getData().get("runGeneralScript")).isEqualTo(0);
        assertThat(runSave.getData().get("runSaveProceed")).isEqualTo(0);
        assertThat(runSave.getData().get("runSaveRun")).isEqualTo(1);

        assertThat(runGeneralScript.getData().get("runGeneralScript")).isEqualTo(1);
        assertThat(runGeneralScript.getData().get("runSaveProceed")).isEqualTo(0);
        assertThat(runGeneralScript.getData().get("runSaveRun")).isEqualTo(0);
    }

    @Test
    @Transactional
    public void testDeleteLep() throws Exception {

        initLeps();


        XmEntity runSaveProceed = createEntity("TEST_DELETE_AND_PROCEED");
        XmEntity runSave = createEntity("TEST_DELETE_RUN");
        XmEntity runGeneralScript = createEntity("TEST_NOT_RUN_DELETE");

        runSaveProceed = xmEntityServiceImpl.save(runSaveProceed);
        runSave = xmEntityServiceImpl.save(runSave);
        runGeneralScript = xmEntityServiceImpl.save(runGeneralScript);


        xmEntityServiceImpl.delete(runSaveProceed.getId());
        xmEntityServiceImpl.delete(runSave.getId());
        xmEntityServiceImpl.delete(runGeneralScript.getId());


        assertThat(runSaveProceed.getData().get("runGeneralScriptDELETE")).isEqualTo(1);
        assertThat(runSaveProceed.getData().get("runDeleteProceed")).isEqualTo(1);
        assertThat(runSaveProceed.getData().get("runDeleteRun")).isEqualTo(0);

        assertThat(runSave.getData().get("runGeneralScriptDELETE")).isEqualTo(0);
        assertThat(runSave.getData().get("runDeleteProceed")).isEqualTo(0);
        assertThat(runSave.getData().get("runDeleteRun")).isEqualTo(1);

        assertThat(runGeneralScript.getData().get("runGeneralScriptDELETE")).isEqualTo(1);
        assertThat(runGeneralScript.getData().get("runDeleteProceed")).isEqualTo(0);
        assertThat(runGeneralScript.getData().get("runDeleteRun")).isEqualTo(0);
    }

}
