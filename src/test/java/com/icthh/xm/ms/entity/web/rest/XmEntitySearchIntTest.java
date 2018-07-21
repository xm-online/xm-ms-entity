package com.icthh.xm.ms.entity.web.rest;

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_AUTH_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import static com.icthh.xm.commons.tenant.TenantContextUtils.setTenant;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.icthh.xm.commons.exceptions.spring.web.ExceptionTranslator;
import com.icthh.xm.commons.lep.XmLepScriptConfigServerResourceLoader;
import com.icthh.xm.commons.security.XmAuthenticationContext;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.ms.entity.EntityApp;
import com.icthh.xm.ms.entity.config.LepConfiguration;
import com.icthh.xm.ms.entity.config.SecurityBeanOverrideConfiguration;
import com.icthh.xm.ms.entity.config.tenant.WebappTenantOverrideConfiguration;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.ext.IdOrKey;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
import com.icthh.xm.ms.entity.service.XmEntityGeneratorService;
import com.icthh.xm.ms.entity.service.XmEntitySpecService;
import com.icthh.xm.ms.entity.service.impl.XmEntityServiceImpl;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.Validator;

import java.io.InputStream;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Test class for the XmEntitySpecResource REST controller.
 *
 * @see XmEntitySpecResource
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
    EntityApp.class,
    SecurityBeanOverrideConfiguration.class,
    WebappTenantOverrideConfiguration.class,
    LepConfiguration.class
})
public class XmEntitySearchIntTest {

    private static final String KEY1 = "ACCOUNT";

    private static final String KEY2 = "ACCOUNT.ADMIN";

    private static final String KEY3 = "ACCOUNT.USER";

    private static final String KEY4 = "PRODUCT-OFFERING";

    @Autowired
    private XmEntityServiceImpl xmEntityService;

    @Autowired
    private XmEntitySpecService xmEntitySpecService;

    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Mock
    private XmAuthenticationContextHolder authContextHolder;

    @Mock
    private XmAuthenticationContext context;

    @Autowired
    private LepManager lepManager;

    @Autowired
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Autowired
    private Validator validator;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    private XmEntityGeneratorService xmEntityGeneratorService;

    private MockMvc restXmEntityMockMvc;

    @Autowired
    private XmLepScriptConfigServerResourceLoader leps;

    @Before
    @SneakyThrows
    public void setup() {
        MockitoAnnotations.initMocks(this);
        when(authContextHolder.getContext()).thenReturn(context);
        when(context.getRequiredUserKey()).thenReturn("userKey");

        setTenant(tenantContextHolder, "DEMO");
        lepManager.beginThreadContext(ctx -> {
            ctx.setValue(THREAD_CONTEXT_KEY_TENANT_CONTEXT, tenantContextHolder.getContext());
            ctx.setValue(THREAD_CONTEXT_KEY_AUTH_CONTEXT, authContextHolder.getContext());
        });


        this.restXmEntityMockMvc = MockMvcBuilders.standaloneSetup(new XmEntityResource(xmEntityService, null, null, null, null, null))
            .setValidator(validator).setControllerAdvice(exceptionTranslator).setCustomArgumentResolvers(pageableArgumentResolver).build();

    }

    void initLeps() {
        leps.onRefresh("/config/tenants/DEMO/entity/lep/service/entity/Save$$ACCOUNT$$around.groovy", loadFile("config/testlep/Save$$ACCOUNT$$around.groovy"));
    }

    @SneakyThrows
    public static String loadFile(String path) {
        return IOUtils.toString(new ClassPathResource(path).getInputStream(), UTF_8);
    }

    public static XmEntity createEntity(String typeKey) {
        val data = new HashMap<String, Object>();
        val entity = new XmEntity()
            .key(UUID.randomUUID())
            .typeKey(typeKey)
            .startDate(Instant.now())
            .name("DEFAULT_NAME")
            .description("DEFAULT_DESCRIPTION");
        entity.setData(data);
        return entity;
    }

    @After
    @Override
    public void finalize() {
        lepManager.endThreadContext();
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
    }

    @Test
    @SneakyThrows
    @WithMockUser(authorities = "SUPER-ADMIN")
    public void getAllXmEntityTypeSpecs() throws Exception {
        initLeps();

        XmEntity account = createEntity("ACCOUNT");

        restXmEntityMockMvc.perform(post("/api/xm-entities")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(account)))
            .andDo(print())
            .andExpect(status().isBadRequest())
        ;

        String contentAsString = restXmEntityMockMvc.perform(get("/api/_search/xm-entities?query=DEFAULT_NAME&page=0&size=10&sort=name,asc")
            .contentType(TestUtil.APPLICATION_JSON_UTF8))
            .andDo(print())
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        List<XmEntity> readValue = objectMapper.readValue(contentAsString,  new TypeReference<List<XmEntity>>() { });
        log.info("{}", xmEntityService.findOne(IdOrKey.of(String.valueOf(readValue.get(0).getId()))));
    }

}
