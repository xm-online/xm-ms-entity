package com.icthh.xm.ms.entity.domain.serializer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.icthh.xm.commons.i18n.error.web.ExceptionTranslator;
import com.icthh.xm.commons.i18n.spring.service.LocalizationMessageService;
import com.icthh.xm.commons.lep.XmLepScriptConfigServerResourceLoader;
import com.icthh.xm.commons.logging.config.LoggingConfigService;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.security.spring.config.XmAuthenticationContextConfiguration;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.tenant.spring.config.TenantContextConfiguration;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.ms.entity.AbstractWebMvcTest;
import com.icthh.xm.ms.entity.config.JacksonConfiguration;
import com.icthh.xm.ms.entity.config.LepConfiguration;
import com.icthh.xm.ms.entity.config.TestLepContextFactory;
import com.icthh.xm.ms.entity.config.TestLepUpdateModeConfiguration;
import com.icthh.xm.ms.entity.config.WebMvcConfiguration;
import com.icthh.xm.ms.entity.domain.Link;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.ext.IdOrKey;
import com.icthh.xm.ms.entity.lep.LepContextFactoryImpl;
import com.icthh.xm.ms.entity.repository.kafka.ProfileEventProducer;
import com.icthh.xm.ms.entity.service.FunctionService;
import com.icthh.xm.ms.entity.service.ProfileService;
import com.icthh.xm.ms.entity.service.TenantService;
import com.icthh.xm.ms.entity.service.XmEntityService;
import com.icthh.xm.ms.entity.web.rest.XmEntityResource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_AUTH_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import static com.icthh.xm.ms.entity.domain.serializer.JsonResponseFilteringUnitTest.addTargetLink;
import static com.icthh.xm.ms.entity.domain.serializer.JsonResponseFilteringUnitTest.assertLinkStructure;
import static com.icthh.xm.ms.entity.domain.serializer.JsonResponseFilteringUnitTest.createMockResultEntity;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@WebMvcTest(controllers = XmEntityResource.class)
@ContextConfiguration(classes = {
    TestLepContextFactory.class,
    TestLepUpdateModeConfiguration.class,
    LepConfiguration.class,
    JacksonConfiguration.class,
    XmEntityResource.class,
    ExceptionTranslator.class,
    TenantContextConfiguration.class,
    XmAuthenticationContextConfiguration.class,
})
@EnableAspectJAutoProxy
@EnableSpringDataWebSupport
public class JsonResponseFilteringLepUnitTest extends AbstractWebMvcTest {

    // XmEntityResource config
    @MockBean
    private XmEntityService xmEntityService;
    @MockBean
    private ProfileService profileService;
    @MockBean
    private ProfileEventProducer profileEventProducer;
    @MockBean
    private TenantService tenantService;
    @MockBean
    private FunctionService functionService;
    @MockBean
    private LocalizationMessageService localizationMessageService;
    @MockBean
    private LoggingConfigService loggingConfigService;

    @Autowired
    private XmEntityResource xmEntityResource;

    @Autowired
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @Autowired
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    private MockMvc mockMvc;

    // Squiggly config
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    XmSquigglyInterceptor xmSquigglyInterceptor;

    @Autowired
    private JacksonConfiguration.HttpMessageConverterCustomizer httpMessageConverterCustomizer;

    // LEP config
    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Autowired
    private XmAuthenticationContextHolder authContextHolder;

    @Autowired
    private LepManager lepManager;

    @Autowired
    private XmLepScriptConfigServerResourceLoader leps;

    @Before
    @SneakyThrows
    public void setup() {

        MockitoAnnotations.initMocks(this);

        TenantContextUtils.setTenant(tenantContextHolder, "RESINTTEST");

        httpMessageConverterCustomizer.customize(Collections.singletonList(jacksonMessageConverter));
        objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        this.mockMvc = MockMvcBuilders.standaloneSetup(xmEntityResource)
                                      .setControllerAdvice(exceptionTranslator)
                                      .setMessageConverters(jacksonMessageConverter)
                                      .setCustomArgumentResolvers(pageableArgumentResolver)
                                      .addMappedInterceptors(WebMvcConfiguration.getJsonFilterAllowedURIs(),
                                                             xmSquigglyInterceptor)
                                      .build();

        lepManager.beginThreadContext(ctx -> {
            ctx.setValue(THREAD_CONTEXT_KEY_TENANT_CONTEXT, tenantContextHolder.getContext());
            ctx.setValue(THREAD_CONTEXT_KEY_AUTH_CONTEXT, authContextHolder.getContext());
        });
    }

    @After
    public void destroy() {
        initLeps(false);
        lepManager.endThreadContext();
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
    }

    void initLeps(boolean loadData) {
        String pattern = "/config/tenants/RESINTTEST/entity/lep/serialize/filter/";

        val testLeps = new String[]{
            "CustomizeFilter$$around.groovy"
        };

        for (val lep : testLeps) {
            leps.onRefresh(pattern + lep, loadData ? loadFile("config/testlep/" + lep) : null);
        }
    }

    @SneakyThrows
    private static String loadFile(String path) {
        InputStream cfgInputStream = new ClassPathResource(path).getInputStream();
        return IOUtils.toString(cfgInputStream, UTF_8);
    }

    @Test
    @SneakyThrows
    public void testLinksFilterByLepApplied() {

        String targetTypeKey = "ACCOUNT.USER";

        XmEntity source = createMockResultEntity("ACCOUNT.ADMIN");

        XmEntity target1 = createMockResultEntity(targetTypeKey);
        XmEntity target2 = createMockResultEntity(targetTypeKey);

        addTargetLink(source, target1);
        addTargetLink(source, target2);

        List<Link> result = new ArrayList<>(source.getTargets());

        Assert.assertNotNull(source.getId());
        Integer srcId = source.getId().intValue();

        when(xmEntityService.getLinkTargets(IdOrKey.of(source.getId()), targetTypeKey)).thenReturn(result);

        ResultActions actions = performGet("/api/xm-entities/{id}/links/targets?typeKey={typeKey}",
                                           srcId, targetTypeKey)
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE));

        assertLinkStructure(actions, 2);

        initLeps(true);

        performGet("/api/xm-entities/{id}/links/targets?typeKey={typeKey}", srcId, targetTypeKey)
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$.[*].id", hasSize(2)))
            .andExpect(jsonPath("$.[*].name", hasSize(2)))
            .andExpect(jsonPath("$.[*].source", hasSize(2)))
            .andExpect(jsonPath("$.[*].typeKey").doesNotExist())
            .andExpect(jsonPath("$.[*].description").doesNotExist())
            .andExpect(jsonPath("$.[*].startDate").doesNotExist())
            .andExpect(jsonPath("$.[*].endDate").doesNotExist())
            .andExpect(jsonPath("$.[*].target").doesNotExist());

    }

    @Test
    @SneakyThrows
    public void testXmEntityFilterByLepDisabled() {

        String targetTypeKey = "ACCOUNT.USER";

        XmEntity entity1 = createMockResultEntity(targetTypeKey);
        XmEntity entity2 = createMockResultEntity(targetTypeKey);

        Page<XmEntity> result = new PageImpl<>(Arrays.asList(entity1, entity2));

        when(xmEntityService.findAll(isA(Pageable.class), any(), any())).thenReturn(result);

        performGet("/api/xm-entities?size=10")
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].key", hasSize(2)))
            .andExpect(jsonPath("$.[*].typeKey", hasSize(2)))
            .andExpect(jsonPath("$.[*].stateKey", hasSize(2)))
            .andExpect(jsonPath("$.[*].name", hasSize(2)))
            .andExpect(jsonPath("$.[*].description", hasSize(2)))
            .andExpect(jsonPath("$.[*].startDate", hasSize(2)))
            .andExpect(jsonPath("$.[*].endDate", hasSize(2)))
            .andExpect(jsonPath("$.[*].data", hasSize(2)))
            .andExpect(jsonPath("$.[*].avatarUrl", hasSize(2)))
            .andExpect(jsonPath("$.[*].attachments.[*].typeKey", hasSize(2)))
            .andExpect(jsonPath("$.[*].attachments.[*].content.value", hasSize(2)))
            .andExpect(jsonPath("$.[*].locations.[*].typeKey", hasSize(2)))
            .andExpect(jsonPath("$.[*].tags.[*].typeKey", hasSize(2)))
        ;

        initLeps(true);

        performGet("/api/xm-entities?size=10")
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$.[*].id", hasSize(2)))
            .andExpect(jsonPath("$.[*].key", hasSize(2)))
            .andExpect(jsonPath("$.[*].typeKey", hasSize(2)))
            .andExpect(jsonPath("$.[*].stateKey", hasSize(2)))
            .andExpect(jsonPath("$.[*].name", hasSize(2)))
            .andExpect(jsonPath("$.[*].description", hasSize(2)))
            .andExpect(jsonPath("$.[*].startDate", hasSize(2)))
            .andExpect(jsonPath("$.[*].endDate", hasSize(2)))
            .andExpect(jsonPath("$.[*].data", hasSize(2)))
            .andExpect(jsonPath("$.[*].attachments", hasSize(2)))
            .andExpect(jsonPath("$.[*].attachments.[*].typeKey", hasSize(2)))
            .andExpect(jsonPath("$.[*].attachments.[*].content.value", hasSize(2)))
            .andExpect(jsonPath("$.[*].locations", hasSize(2)))
            .andExpect(jsonPath("$.[*].tags", hasSize(2)))
            .andExpect(jsonPath("$.[*].avatarUrl", hasSize(2)))
        ;

    }

    @SneakyThrows
    private ResultActions performGet(String url, Object... params) {
        return mockMvc.perform(get(url, params))
                      .andDo(this::printMvcResult);
    }

    private void printMvcResult(MvcResult result) throws UnsupportedEncodingException {
        log.info("MVC result: {}", result.getResponse().getContentAsString());
    }

}
