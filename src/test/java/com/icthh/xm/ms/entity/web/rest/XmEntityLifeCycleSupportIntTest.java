package com.icthh.xm.ms.entity.web.rest;

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_AUTH_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.icthh.xm.commons.i18n.error.web.ExceptionTranslator;
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
import com.icthh.xm.ms.entity.repository.kafka.ProfileEventProducer;
import com.icthh.xm.ms.entity.service.*;
import com.icthh.xm.ms.entity.service.impl.XmEntityServiceImpl;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.mutable.MutableLong;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Validator;

import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;

@Slf4j
@RunWith(SpringRunner.class)
@WithMockUser(authorities = {"SUPER-ADMIN"})
@SpringBootTest(classes = {
    EntityApp.class,
    SecurityBeanOverrideConfiguration.class,
    WebappTenantOverrideConfiguration.class,
    LepConfiguration.class
})
public class XmEntityLifeCycleSupportIntTest {

    @Autowired
    private XmEntityServiceImpl xmEntityServiceImpl;

    private FunctionService functionService;

    @Autowired
    private ProfileService profileService;

    @Autowired
    private ProfileEventProducer profileEventProducer;

    @Autowired
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Autowired
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Autowired
    private XmAuthenticationContextHolder authContextHolder;

    @Autowired
    private LepManager lepManager;

    private MockMvc restXmEntityMockMvc;

    @Autowired
    private Validator validator;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private XmLepScriptConfigServerResourceLoader leps;

    @BeforeTransaction
    public void beforeTransaction() {
        TenantContextUtils.setTenant(tenantContextHolder, "RESINTTEST");
    }

    private int updateState, updateByEntity, updateByTargetState, updateByTransition;

    @SneakyThrows
    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        lepManager.beginThreadContext(ctx -> {
            ctx.setValue(THREAD_CONTEXT_KEY_TENANT_CONTEXT, tenantContextHolder.getContext());
            ctx.setValue(THREAD_CONTEXT_KEY_AUTH_CONTEXT, authContextHolder.getContext());
        });

        XmEntityResource resourceMock = mock(XmEntityResource.class);
        when(resourceMock.createXmEntity(any())).thenReturn(ResponseEntity.created(new URI("")).build());
        XmEntityResource xmEntityResourceMock = new XmEntityResource(xmEntityServiceImpl,
            profileService,
            profileEventProducer,
            functionService,
            tenantService,
            resourceMock
        );
        this.restXmEntityMockMvc = MockMvcBuilders.standaloneSetup(xmEntityResourceMock)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setValidator(validator)
            .setMessageConverters(jacksonMessageConverter).build();
    }

    void initLeps() {
        String pattern = "/config/tenants/RESINTTEST/entity/lep/lifecycle/";

        val testLeps = new String[]{
            "ChangeState$$TEST_LIFECYCLE$$around.groovy",
            "ChangeState$$TEST_LIFECYCLE$$STATE4$$around.groovy",
            "ChangeState$$TEST_LIFECYCLE$$STATE5$$STATE6$$around.groovy",
            "ChangeState$$TEST_LIFECYCLE$$STATE6$$STATE7$$around.groovy",
            "ChangeState$$TEST_LIFECYCLE$$STATE7$$around.groovy"
        };

        leps.onRefresh(pattern + "ChangeState$$around.groovy", loadFile("config/testlep/ChangeState$$around.groovy"));

        for (val lep: testLeps) {
            leps.onRefresh(pattern + "chained/" + lep, loadFile("config/testlep/" + lep));
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

    public static XmEntity createEntity(String key) {
        val data = new HashMap<String, Object>();
        data.put("updateState", 0);
        data.put("updateByEntity", 0);
        data.put("updateByTargetState", 0);
        data.put("updateByTransition", 0);
        val entity = new XmEntity()
            .key(key)
            .typeKey("TEST_LIFECYCLE")
            .stateKey("STATE1")
            .name("DEFAULT_NAME")
            .description("DEFAULT_DESCRIPTION");
        entity.setData(data);
        return entity;
    }

    @Test
    @Transactional
    public void testNotLep() throws Exception {

        MutableLong id = new MutableLong();

        // Create the XmEntity
        restXmEntityMockMvc.perform(post("/api/xm-entities")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(createEntity("KEY1"))))
            .andDo(r -> {
                log.info(r.getResponse().getContentAsString());
                final ObjectNode node = new ObjectMapper().readValue(r.getResponse().getContentAsString(), ObjectNode.class);
                id.setValue(node.get("id").longValue());
            })
            .andExpect(status().isCreated());

        changeState(id, "STATE2", updateStateNotCalled(), updateByEntityNotCalled(),updateByTargetStateNotCalled(),updateByTransitionNotCalled());
        initLeps();
        changeState(id, "STATE3", updateStateCalled(), updateByEntityCalled(),updateByTargetStateNotCalled(),updateByTransitionNotCalled());
        changeState(id, "STATE4", updateStateCalled(), updateByEntityCalled(),updateByTargetStateCalled(),updateByTransitionNotCalled());
        changeState(id, "STATE5", updateStateCalled(), updateByEntityCalled(),updateByTargetStateNotCalled(),updateByTransitionNotCalled());
        changeState(id, "STATE6", updateStateCalled(), updateByEntityCalled(),updateByTargetStateNotCalled(),updateByTransitionCalled());
        changeState(id, "STATE7", updateStateCalled(), updateByEntityCalled(),updateByTargetStateCalled(),updateByTransitionCalled());
    }

    @Test
    @Transactional
    public void setCreateEntityWithoutState() throws Exception {

        // Create the XmEntity
        restXmEntityMockMvc.perform(post("/api/xm-entities")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(createEntity("KEY1").stateKey(null))))
            .andExpect(status().isBadRequest());
    }


    @Test
    @Transactional
    public void setUpdateEntityWithoutState() throws Exception {

        MutableLong id = new MutableLong();

        // Create the XmEntity
        restXmEntityMockMvc.perform(post("/api/xm-entities")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(createEntity("KEY1"))))
            .andDo(r -> {
                log.info(r.getResponse().getContentAsString());
                final ObjectNode node = new ObjectMapper().readValue(r.getResponse().getContentAsString(), ObjectNode.class);
                id.setValue(node.get("id").longValue());
            })
            .andExpect(status().isCreated());

        // Create the XmEntity
        XmEntity entity = createEntity("KEY1");
        entity.setId(id.toLong());
        restXmEntityMockMvc.perform(put("/api/xm-entities")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(entity.stateKey("STATE2"))))
            .andExpect(status().isOk());

        restXmEntityMockMvc.perform(put("/api/xm-entities")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(entity.stateKey(null))))
            .andExpect(jsonPath("$.stateKey").value("STATE2"))
            .andExpect(status().isOk());
    }

    private void changeState(MutableLong id, String state, Integer updateState, Integer updateByEntity, Integer updateByTargetState, Integer updateByTransition) throws Exception {
        restXmEntityMockMvc.perform(put("/api/xm-entities/{id}/states/{state}", id.getValue(), state)
            .contentType(TestUtil.APPLICATION_JSON_UTF8))
            .andDo(r -> {
                String json = r.getResponse().getContentAsString();
                log.info(json);
                final ObjectNode node = new ObjectMapper().readValue(r.getResponse().getContentAsString(), ObjectNode.class);
                assertThat(node.get("data").get("updateState").intValue()).isEqualTo(updateState);
                assertThat(node.get("data").get("updateByEntity").intValue()).isEqualTo(updateByEntity);
                assertThat(node.get("data").get("updateByTargetState").intValue()).isEqualTo(updateByTargetState);
                assertThat(node.get("data").get("updateByTransition").intValue()).isEqualTo(updateByTransition);
            })
            .andExpect(status().isOk());
    }

    // --

    private Integer updateStateCalled() {
        return ++updateState;
    }

    private Integer updateStateNotCalled() {
        return updateState;
    }

    private Integer updateByEntityCalled() {
        return ++updateByEntity;
    }

    private Integer updateByEntityNotCalled() {
        return updateByEntity;
    }

    private Integer updateByTargetStateCalled() {
        return ++updateByTargetState;
    }

    private Integer updateByTargetStateNotCalled() {
        return updateByTargetState;
    }

    private Integer updateByTransitionCalled() {
        return ++updateByTransition;
    }

    private Integer updateByTransitionNotCalled() {
        return updateByTransition;
    }

}
