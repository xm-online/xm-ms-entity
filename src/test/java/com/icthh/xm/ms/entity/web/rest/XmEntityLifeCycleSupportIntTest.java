package com.icthh.xm.ms.entity.web.rest;

import static com.google.common.collect.ImmutableMap.of;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_AUTH_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
import com.icthh.xm.ms.entity.AbstractSpringBootTest;
import com.icthh.xm.ms.entity.config.XmEntityTenantConfigService;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.repository.kafka.ProfileEventProducer;
import com.icthh.xm.ms.entity.service.FunctionService;
import com.icthh.xm.ms.entity.service.ProfileService;
import com.icthh.xm.ms.entity.service.TenantService;
import com.icthh.xm.ms.entity.service.impl.XmEntityServiceImpl;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.mutable.MutableLong;
import org.apache.commons.lang.text.StrSubstitutor;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ReflectionUtils;
import org.springframework.validation.Validator;

@Slf4j
@WithMockUser(authorities = {"SUPER-ADMIN"})
public class XmEntityLifeCycleSupportIntTest extends AbstractSpringBootTest {

    private final String PATTERN = "/config/tenants/RESINTTEST/entity/lep/lifecycle/";

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

    @Autowired
    private XmEntityTenantConfigService xmEntityTenantConfigService;

    private List<String> lepsForCleanUp = new ArrayList<>();

    @BeforeTransaction
    public void beforeTransaction() {
        TenantContextUtils.setTenant(tenantContextHolder, "RESINTTEST");
        xmEntityTenantConfigService.getXmEntityTenantConfig().getLep().setEnableInheritanceTypeKey(true);
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

        val testLeps = new String[]{
            "ChangeState$$TEST_LIFECYCLE$$around.groovy",
            "ChangeState$$TEST_LIFECYCLE$$STATE4$$around.groovy",
            "ChangeState$$TEST_LIFECYCLE$$STATE5$$STATE6$$around.groovy",
            "ChangeState$$TEST_LIFECYCLE$$STATE6$$STATE7$$around.groovy",
            "ChangeState$$TEST_LIFECYCLE$$STATE7$$around.groovy"
        };

        leps.onRefresh(PATTERN + "ChangeState$$around.groovy", loadFile("config/testlep/ChangeState$$around.groovy"));

        for (val lep: testLeps) {
            leps.onRefresh(PATTERN + "chained/" + lep, loadFile("config/testlep/" + lep));
        }
    }

    private void addLep(String pattern, String lepName) {
        String lepBody = loadFile("config/testlep/ChangeState$$TEST_LIFECYCLE_TYPE_KEY$$around.groovy");
        lepBody = StrSubstitutor.replace(lepBody, of("lepName", lepName));
        leps.onRefresh(pattern + "chained/ChangeState$$" + lepName + "$$around.groovy", lepBody);
        lepsForCleanUp.add(pattern + "Save$$" + lepName + "$$around.groovy");
    }

    @SneakyThrows
    public static String loadFile(String path) {
        InputStream cfgInputStream = new ClassPathResource(path).getInputStream();
        return IOUtils.toString(cfgInputStream, UTF_8);
    }

    @After
    @Override
    public void finalize() {
        lepsForCleanUp.forEach(it -> leps.onRefresh(it, null));
        xmEntityTenantConfigService.getXmEntityTenantConfig().getLep().setEnableInheritanceTypeKey(false);
        lepManager.endThreadContext();
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
    }

    public static XmEntity createEntity(String key, String typeKey) {
        val data = new HashMap<String, Object>();
        data.put("updateState", 0);
        data.put("updateByEntity", 0);
        data.put("updateByTargetState", 0);
        data.put("updateByTransition", 0);
        val entity = new XmEntity()
            .key(key)
            .typeKey(typeKey)
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
            .content(TestUtil.convertObjectToJsonBytes(createEntity("KEY1", "TEST_LIFECYCLE"))))
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
    @SneakyThrows
    public void testExtendsTypeKey() {

        leps.onRefresh(PATTERN + "ChangeState$$around.groovy", loadFile("config/testlep/ChangeState$$around.groovy"));
        addLep(PATTERN, "TEST_LIFECYCLE_TYPE_KEY");
        addLep(PATTERN, "TEST_LIFECYCLE_TYPE_KEY$SUB");
        addLep(PATTERN, "TEST_LIFECYCLE_TYPE_KEY$SUB$CHILD");
        addLep(PATTERN, "TEST_LIFECYCLE_TYPE_KEY$$STATE2");
        addLep(PATTERN, "TEST_LIFECYCLE_TYPE_KEY$SUB$$STATE2");
        addLep(PATTERN, "TEST_LIFECYCLE_TYPE_KEY$SUB$CHILD$$STATE2");
        addLep(PATTERN, "TEST_LIFECYCLE_TYPE_KEY$$STATE1$$STATE2");
        addLep(PATTERN, "TEST_LIFECYCLE_TYPE_KEY$SUB$$STATE1$$STATE2");
        addLep(PATTERN, "TEST_LIFECYCLE_TYPE_KEY$SUB$CHILD$$STATE1$$STATE2");

        MutableLong id = new MutableLong();
        // Create the XmEntity
        XmEntity entity = createEntity("TEST_KEY", "TEST_LIFECYCLE_TYPE_KEY.SUB.CHILD");
        entity.getData().put("called", "");
        restXmEntityMockMvc.perform(post("/api/xm-entities")
                                        .contentType(TestUtil.APPLICATION_JSON_UTF8)
                                        .content(TestUtil.convertObjectToJsonBytes(entity)))
            .andDo(r -> {
                log.info(r.getResponse().getContentAsString());
                final ObjectNode node = new ObjectMapper().readValue(r.getResponse().getContentAsString(), ObjectNode.class);
                id.setValue(node.get("id").longValue());
            })
            .andExpect(status().isCreated());

        restXmEntityMockMvc.perform(put("/api/xm-entities/{id}/states/{state}", id.toLong(), "STATE2")
                                        .contentType(TestUtil.APPLICATION_JSON_UTF8))
            .andExpect(status().isOk())
            .andDo(r -> {
                String json = r.getResponse().getContentAsString();
                log.info(json);
                final ObjectNode node = new ObjectMapper().readValue(r.getResponse().getContentAsString(), ObjectNode.class);
                String expected = " root TEST_LIFECYCLE_TYPE_KEY TEST_LIFECYCLE_TYPE_KEY$SUB " +
                                  "TEST_LIFECYCLE_TYPE_KEY$SUB$CHILD TEST_LIFECYCLE_TYPE_KEY$$STATE2" +
                                  " TEST_LIFECYCLE_TYPE_KEY$SUB$$STATE2 TEST_LIFECYCLE_TYPE_KEY$SUB$CHILD$$STATE2" +
                                  " TEST_LIFECYCLE_TYPE_KEY$$STATE1$$STATE2 TEST_LIFECYCLE_TYPE_KEY$SUB$$STATE1$$STATE2" +
                                  " TEST_LIFECYCLE_TYPE_KEY$SUB$CHILD$$STATE1$$STATE2";
                assertThat(node.get("data").get("called").textValue()).isEqualTo(expected);
            });

        leps.onRefresh(PATTERN + "ChangeState$$around.groovy", null);
    }

    @Test
    @Transactional
    public void setCreateEntityWithoutState() throws Exception {
        // Create the XmEntity
        restXmEntityMockMvc.perform(post("/api/xm-entities")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(createEntity("KEY1", "TEST_LIFECYCLE").stateKey(null))))
            .andExpect(jsonPath("$.stateKey").value("STATE1"))
            .andExpect(status().isCreated());
    }


    @Test
    @Transactional
    public void setUpdateEntityWithoutState() throws Exception {

        MutableLong id = new MutableLong();

        // Create the XmEntity
        restXmEntityMockMvc.perform(post("/api/xm-entities")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(createEntity("KEY1", "TEST_LIFECYCLE"))))
            .andDo(r -> {
                log.info(r.getResponse().getContentAsString());
                final ObjectNode node = new ObjectMapper().readValue(r.getResponse().getContentAsString(), ObjectNode.class);
                id.setValue(node.get("id").longValue());
            })
            .andExpect(status().isCreated());

        // Create the XmEntity
        XmEntity entity = createEntity("KEY1", "TEST_LIFECYCLE");
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
