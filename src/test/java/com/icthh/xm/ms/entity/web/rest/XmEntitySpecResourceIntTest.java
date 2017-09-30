package com.icthh.xm.ms.entity.web.rest;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.icthh.lep.api.LepManager;
import com.icthh.xm.ms.entity.EntityApp;
import com.icthh.xm.ms.entity.config.SecurityBeanOverrideConfiguration;
import com.icthh.xm.ms.entity.config.tenant.TenantContext;
import com.icthh.xm.ms.entity.config.tenant.WebappTenantOverrideConfiguration;
import com.icthh.xm.ms.entity.lep.XmLepConstants;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
import com.icthh.xm.ms.entity.service.XmEntityGeneratorService;
import com.icthh.xm.ms.entity.service.XmEntityServiceImpl;
import com.icthh.xm.ms.entity.service.XmEntitySpecService;
import lombok.SneakyThrows;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Test class for the XmEntitySpecResource REST controller.
 *
 * @see XmEntitySpecResource
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {EntityApp.class, SecurityBeanOverrideConfiguration.class,
    WebappTenantOverrideConfiguration.class})
public class XmEntitySpecResourceIntTest {

    private static final String KEY1 = "ACCOUNT";

    private static final String KEY2 = "ACCOUNT.ADMIN";

    private static final String KEY3 = "ACCOUNT.USER";

    private static final String KEY4 = "PRODUCT-OFFERING";

    @Autowired
    private XmEntityRepository xmEntityRepository;

    @Autowired
    private XmEntityServiceImpl xmEntityService;

    @Autowired
    private XmEntitySpecService xmEntitySpecService;

    @Autowired
    private LepManager lepManager;

    private XmEntityGeneratorService xmEntityGeneratorService;

    private MockMvc restXmEntitySpecMockMvc;

    @Before
    @SneakyThrows
    public void setup() {
        TenantContext.setCurrent("DEMO");
        lepManager.beginThreadContext(ctx -> {
            ctx.setValue(XmLepConstants.CONTEXT_KEY_TENANT, TenantContext.getCurrent());
        });

        xmEntityGeneratorService = new XmEntityGeneratorService(xmEntityService, xmEntitySpecService);

        MockitoAnnotations.initMocks(this);
        XmEntitySpecResource xmEntitySpecResource = new XmEntitySpecResource(xmEntitySpecService,
            xmEntityGeneratorService);
        this.restXmEntitySpecMockMvc = MockMvcBuilders.standaloneSetup(xmEntitySpecResource).build();
    }

    @After
    public void finalize() {
        lepManager.endThreadContext();
        TenantContext.setCurrent("XM");
    }

    @Test
    public void getAllXmEntityTypeSpecs() throws Exception {
        restXmEntitySpecMockMvc.perform(get("/api/xm-entity-specs"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].key").value(hasItem(KEY1)))
            .andExpect(jsonPath("$.[*].key").value(hasItem(KEY2)))
            .andExpect(jsonPath("$.[*].key").value(hasItem(KEY3)));
    }

    @Test
    public void getAllAppXmEntityTypeSpecs() throws Exception {
        restXmEntitySpecMockMvc.perform(get("/api/xm-entity-specs?filter=APP"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].key").value(hasItem(KEY1)))
            .andExpect(jsonPath("$.[*].key").value(hasItem(KEY4)));
    }

    @Test
    public void getAllNonAbstractXmEntityTypeSpecs() throws Exception {
        restXmEntitySpecMockMvc.perform(get("/api/xm-entity-specs?filter=NON_ABSTRACT"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].key").value(hasItem(KEY2)))
            .andExpect(jsonPath("$.[*].key").value(hasItem(KEY3)));
    }

    @Test
    public void getXmEntityTypeSpec() throws Exception {
        restXmEntitySpecMockMvc.perform(get("/api/xm-entity-specs/" + KEY1))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.key").value(KEY1));
    }

    @Test
    public void getGenerateXmEntity() throws Exception {
        restXmEntitySpecMockMvc.perform(post("/api/xm-entity-specs/generate-xm-entity"))
            .andExpect(status().isCreated())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
            .andExpect(jsonPath("$.id").isNotEmpty());
    }

}
