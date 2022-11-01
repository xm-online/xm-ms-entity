package com.icthh.xm.ms.entity.web.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.commons.i18n.error.web.ExceptionTranslator;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.ms.entity.AbstractSpringBootTest;
import com.icthh.xm.ms.entity.service.XmEntityGeneratorService;
import com.icthh.xm.ms.entity.service.XmEntitySpecService;
import com.icthh.xm.ms.entity.service.impl.XmEntityServiceImpl;
import com.icthh.xm.ms.entity.service.spec.JsonSchemaGenerationService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_AUTH_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import static com.icthh.xm.commons.tenant.TenantContextUtils.setTenant;
import static com.icthh.xm.ms.entity.config.TenantConfigMockConfiguration.getXmEntitySpec;
import static com.icthh.xm.ms.entity.util.IsCollectionNotContaining.hasNotItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test class for the XmEntitySpecResource REST controller.
 *
 * @see XmEntitySpecResource
 */
@Slf4j
public class XmEntitySpecResourceIntTest extends AbstractSpringBootTest {

    private static final String KEY1 = "ACCOUNT";

    private static final String KEY2 = "ACCOUNT.ADMIN";

    private static final String KEY3 = "ACCOUNT.USER";

    private static final String KEY4 = "PRODUCT-OFFERING";

    private static final String KEY5 = "ACCOUNT.CUSTOMER";

    private static final String XM_ENTITY_SPEC = "urn:jsonschema:com:icthh:xm:ms:entity:domain:spec:XmEntitySpec";

    @Autowired
    private XmEntityServiceImpl xmEntityService;

    @Autowired
    private XmEntitySpecService xmEntitySpecService;

    @Autowired
    private JsonSchemaGenerationService jsonSchemaGenerationService;

    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Autowired
    private XmAuthenticationContextHolder authContextHolder;

/*
    @Mock
    private XmAuthenticationContext context;
*/

    @Autowired
    private LepManager lepManager;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    private XmEntityGeneratorService xmEntityGeneratorService;

    private MockMvc restXmEntitySpecMockMvc;

    @Before
    @SneakyThrows
    public void setup() {
        MockitoAnnotations.initMocks(this);


        //when(context.getRequiredUserKey()).thenReturn("userKey");
        //when(authContextHolder.getContext()).thenReturn(context);

        setTenant(tenantContextHolder, "DEMO");

        lepManager.beginThreadContext(ctx -> {
            ctx.setValue(THREAD_CONTEXT_KEY_TENANT_CONTEXT, tenantContextHolder.getContext());
            ctx.setValue(THREAD_CONTEXT_KEY_AUTH_CONTEXT, authContextHolder.getContext());
        });

        xmEntityGeneratorService = new XmEntityGeneratorService(xmEntityService,
            xmEntitySpecService, authContextHolder, objectMapper);

        XmEntitySpecResource xmEntitySpecResource = new XmEntitySpecResource(xmEntitySpecService,
            xmEntityGeneratorService, jsonSchemaGenerationService);

        this.restXmEntitySpecMockMvc = MockMvcBuilders.standaloneSetup(xmEntitySpecResource)
            .setControllerAdvice(exceptionTranslator).build();
    }

    @After
    public void tearDown() {
        lepManager.endThreadContext();
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
    }

    @Test
    @WithUserDetails()
    public void getAllXmEntityTypeSpecs() throws Exception {
        restXmEntitySpecMockMvc.perform(get("/api/xm-entity-specs"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].key").value(hasItem(KEY1)))
            .andExpect(jsonPath("$.[*].key").value(hasItem(KEY2)))
            .andExpect(jsonPath("$.[*].key").value(hasItem(KEY3)));
    }

    @Test
    @WithUserDetails()
    public void getAllXmEntityTypeSpecsWithAdditionFile() throws Exception {
        String configPath = "/config/tenants/DEMO/entity/xmentityspec/additional.yml";
        assertTrue(xmEntitySpecService.isListeningConfiguration(configPath));
        xmEntitySpecService.onRefresh(configPath, getXmEntitySpec("additional"));
        restXmEntitySpecMockMvc.perform(get("/api/xm-entity-specs"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].key").value(hasItem(KEY1)))
            .andExpect(jsonPath("$.[*].key").value(hasItem(KEY2)))
            .andExpect(jsonPath("$.[*].key").value(hasItem(KEY3)))
            .andExpect(jsonPath("$.[*].key").value(hasItem(KEY5)));
    }

    @Test
    @WithUserDetails()
    public void testRemoveAdditionalSpec() throws Exception {
        String configPath = "/config/tenants/DEMO/entity/xmentityspec/additional.yml";
        assertTrue(xmEntitySpecService.isListeningConfiguration(configPath));
        xmEntitySpecService.onRefresh(configPath, getXmEntitySpec("additional"));
        restXmEntitySpecMockMvc.perform(get("/api/xm-entity-specs"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].key").value(hasItem(KEY1)))
            .andExpect(jsonPath("$.[*].key").value(hasItem(KEY5)))
            .andExpect(jsonPath("$.[*].key").value(hasSize(13)));
        xmEntitySpecService.onRefresh(configPath, null);
        restXmEntitySpecMockMvc.perform(get("/api/xm-entity-specs"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].key").value(hasItem(KEY1)))
            .andExpect(jsonPath("$.[*].key").value(hasNotItem(KEY5)))
            .andExpect(jsonPath("$.[*].key").value(hasSize(12)));
    }

    @Test
    @WithUserDetails()
    public void getAllXmEntityTypeSpecsByFilter() throws Exception {
        restXmEntitySpecMockMvc.perform(get("/api/xm-entity-specs?filter=" + XmEntitySpecResource.Filter.ALL))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].key").value(hasItem(KEY1)))
            .andExpect(jsonPath("$.[*].key").value(hasItem(KEY2)))
            .andExpect(jsonPath("$.[*].key").value(hasItem(KEY3)));
    }

    @Test
    @WithUserDetails
    public void getAllAppXmEntityTypeSpecs() throws Exception {
        String configPath = "/config/tenants/DEMO/entity/xmentityspec/demo.yml";
        assertTrue(xmEntitySpecService.isListeningConfiguration(configPath));
        xmEntitySpecService.onRefresh(configPath, getXmEntitySpec("additional"));
        restXmEntitySpecMockMvc.perform(get("/api/xm-entity-specs?filter=" + XmEntitySpecResource.Filter.APP))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andDo((result) -> {
                log.info("{}", result.getResponse().getContentAsString());
            })
            .andExpect(jsonPath("$.[*].key").value(hasItem(KEY1)))
            .andExpect(jsonPath("$.[*].key").value(hasItem(KEY4)));
    }

    @Test
    @WithUserDetails()
    public void getAllNonAbstractXmEntityTypeSpecs() throws Exception {
        restXmEntitySpecMockMvc.perform(get("/api/xm-entity-specs?filter=" + XmEntitySpecResource.Filter.NON_ABSTRACT))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].key").value(hasItem(KEY2)))
            .andExpect(jsonPath("$.[*].key").value(hasItem(KEY3)));
    }

    @Test
    @WithUserDetails()
    public void getXmEntityTypeSpec() throws Exception {
        restXmEntitySpecMockMvc.perform(get("/api/xm-entity-specs/" + KEY1))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.key").value(KEY1));
    }

    @Test
    @WithUserDetails()
    public void getXmEntityTypeSpecNotFound() throws Exception {
        restXmEntitySpecMockMvc.perform(get("/api/xm-entity-specs/UNDEFINED_KEY"))
            .andExpect(status().isNotFound())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE));
    }

    @Test
    @WithUserDetails()
    public void getGenerateXmEntity() throws Exception {
        restXmEntitySpecMockMvc.perform(post("/api/xm-entity-specs/generate-xm-entity"))
            .andExpect(status().isCreated())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
            .andExpect(jsonPath("$.id").isNotEmpty());
    }

    @Test
    @WithUserDetails()
    public void getXmEntityTypeSpecSchema() throws Exception {
        restXmEntitySpecMockMvc.perform(get("/api/xm-entity-specs/schema"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.id").value(XM_ENTITY_SPEC));
    }

}
