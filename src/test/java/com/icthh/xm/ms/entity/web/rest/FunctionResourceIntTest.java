package com.icthh.xm.ms.entity.web.rest;

import com.icthh.xm.commons.i18n.error.web.ExceptionTranslator;
import com.icthh.xm.commons.lep.XmLepScriptConfigServerResourceLoader;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.ms.entity.AbstractSpringBootTest;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.service.impl.XmEntityServiceImpl;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.UUID;

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_AUTH_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WithMockUser(authorities = {"SUPER-ADMIN"})
@Transactional
public class FunctionResourceIntTest extends AbstractSpringBootTest {

    private MockMvc mockMvc;

    @Autowired
    private TenantContextHolder tenantContextHolder;
    @Autowired
    private XmAuthenticationContextHolder authContextHolder;

    @Autowired
    private LepManager lepManager;

    @Autowired
    private XmLepScriptConfigServerResourceLoader leps;

    @Autowired
    private FunctionResource functionResource;

    @Autowired
    private XmEntityResource xmEntityResource;

    @Autowired
    private XmEntityServiceImpl xmEntityService;

    @Autowired
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Autowired
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

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

        this.mockMvc = MockMvcBuilders.standaloneSetup(functionResource, xmEntityResource)
                                                  .setCustomArgumentResolvers(pageableArgumentResolver)
                                                  .setControllerAdvice(exceptionTranslator)
                                                  .setMessageConverters(jacksonMessageConverter, new ByteArrayHttpMessageConverter()).build();

        initLeps(true);
    }

    @After
    public void destroy(){
        initLeps(false);
    }

    void initLeps(boolean loadData) {
        String body = "return [result: lepContext.inArgs.functionInput.files[0].getInputStream().text]";
        String functionPrefix = "/config/tenants/RESINTTEST/entity/lep/function/";
        leps.onRefresh(functionPrefix + "Function$$UPLOAD$$tenant.groovy", loadData ? body : null);
        leps.onRefresh(functionPrefix + "some/package/Function$$UPLOAD$$tenant.groovy", loadData ? body : null);
        String packageTestBody = "return lepContext.inArgs";
        leps.onRefresh(functionPrefix + "package/Function$$FUNCTION$PACKAGE_TEST$$tenant.groovy", loadData ? packageTestBody : null);
        leps.onRefresh(functionPrefix + "package/Function$$FUNCTION$PACKAGE_TEST_CTX$$tenant.groovy", loadData ? packageTestBody : null);
        leps.onRefresh(functionPrefix + "package/FunctionWithXmEntity$$FUNCTION_WITH_ENTITY$PACKAGE_TEST$$tenant.groovy", loadData ? packageTestBody : null);

        String functionWithBinaryResult = "return [\"bytes\": \"test\".getBytes()]";
        leps.onRefresh(functionPrefix + "FunctionWithXmEntity$$FUNCTION_WITH_BINARY_RESULT$$tenant.groovy", loadData ? functionWithBinaryResult : null);
        String functionWithNullResult = "return null";
        leps.onRefresh(functionPrefix + "Function$$FUNCTION_WITH_NULL_RESULT$$tenant.groovy", loadData ? functionWithNullResult : null);

        String anonymousFunctionResult = "return [someKey: \"someValue\"]";
        leps.onRefresh(functionPrefix + "AnonymousFunction$$FUNCTION_WITH_ANONYMOUS_NOT_EXPLICITLY_SET$$tenant.groovy", loadData ? anonymousFunctionResult : null);
        leps.onRefresh(functionPrefix + "AnonymousFunction$$FUNCTION_WITH_ANONYMOUS_SET_TO_FALSE$$tenant.groovy", loadData ? anonymousFunctionResult : null);
        leps.onRefresh(functionPrefix + "AnonymousFunction$$FUNCTION_WITH_ANONYMOUS_SET_TO_TRUE$$tenant.groovy", loadData ? anonymousFunctionResult : null);
    }

    @SneakyThrows
    public static String loadFile(String path) {
        InputStream cfgInputStream = new ClassPathResource(path).getInputStream();
        return IOUtils.toString(cfgInputStream, UTF_8);
    }

    @After
    public void tearDown() {
        lepManager.endThreadContext();
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
    }

    @Test
    @SneakyThrows
    public void testFailFunctionWithNoJsonRequest() {
        MockMultipartFile file = new MockMultipartFile("file", "orig", "text/plain", "test no json content" .getBytes(UTF_8));
        mockMvc.perform(multipart("/api/functions/UPLOAD").file(file))
               .andDo(print())
               .andExpect(status().is5xxServerError());
    }

    @Test
    @SneakyThrows
    public void testUploadFunction() {
        MockMultipartFile file = new MockMultipartFile("file", "orig", "text/plain", "test no json content" .getBytes(UTF_8));
        mockMvc.perform(multipart("/api/functions/UPLOAD/upload").file(file))
               .andDo(print())
               .andExpect(jsonPath("$.data.result").value("test no json content"))
               .andExpect(status().isOk());
    }

    @Test
    @SneakyThrows
    public void testUploadInPackageFunction() {
        MockMultipartFile file = new MockMultipartFile("file", "orig", "text/plain", "test no json content" .getBytes(UTF_8));
        mockMvc.perform(multipart("/api/functions/some/package/UPLOAD/upload").file(file))
                .andDo(print())
                .andExpect(jsonPath("$.data.result").value("test no json content"))
                .andExpect(status().isOk());
    }
    @Test
    @Transactional
    @SneakyThrows
    public void textFuncWithCtx() {
        String functionApi = "/api/functions/";
        String functionKey = "package/FUNCTION.PACKAGE-TEST-CTX";
        mockMvc.perform(post(functionApi + functionKey))
            .andDo(print())
            .andExpect(jsonPath("$.data.functionKey").value(functionKey))
            .andExpect(header().exists("location"))
            .andExpect(status().is2xxSuccessful());
    }

    @Test
    @Transactional
    @SneakyThrows
    public void testFunctionWithPackage() {
        String functionApi = "/api/functions/";
        String functionKey = "package/FUNCTION.PACKAGE-TEST";
        mockMvc.perform(post(functionApi + functionKey))
            .andDo(print())
            .andExpect(jsonPath("$.data.functionKey").value(functionKey))
            .andExpect(header().doesNotExist("location"))
            .andExpect(status().is2xxSuccessful());
        mockMvc.perform(get(functionApi + functionKey))
            .andDo(print())
            .andExpect(jsonPath("$.data.functionKey").value(functionKey))
            .andExpect(status().is2xxSuccessful());
        mockMvc.perform(put(functionApi + functionKey))
            .andDo(print())
            .andExpect(jsonPath("$.data.functionKey").value(functionKey))
            .andExpect(status().is2xxSuccessful());

        Long id = xmEntityService.save(new XmEntity().typeKey("TEST_FUNCTION_WITH_PACKAGE")).getId();

        String functionWithEntityApi = "/api/xm-entities/" + id +"/functions/";
        String functionWithEntityKey = "package/FUNCTION-WITH-ENTITY.PACKAGE-TEST";
        mockMvc.perform(post(functionWithEntityApi + functionWithEntityKey))
            .andDo(print())
            .andExpect(header().doesNotExist("location"))
            .andExpect(jsonPath("$.data.functionKey").value(functionWithEntityKey))
            .andExpect(status().is2xxSuccessful());
        mockMvc.perform(get(functionWithEntityApi + functionWithEntityKey))
            .andDo(print())
            .andExpect(jsonPath("$.data.functionKey").value(functionWithEntityKey))
            .andExpect(status().is2xxSuccessful());
    }

    @Test
    @Transactional
    public void functionWithBinaryDataResult() throws Exception {
        //GIVEN
        Long id = xmEntityService.save(new XmEntity().typeKey("TEST_ENTITY_WITH_BINARY_RESULT_FUNCTION").key(UUID.randomUUID()).name("test")).getId();

        //WHEN
        ResultActions resultActions = mockMvc.perform(
            get("/api/xm-entities/{idOrKey}/functions/{functionName}",
                id, "FUNCTION_WITH_BINARY_RESULT"));

        //THEN
        resultActions
            .andExpect(status().is2xxSuccessful())
            .andExpect(header().string("content-type", "application/pdf"))
            .andExpect(content().bytes("test".getBytes()));
    }

    @Test
    @Transactional
    public void anonymousFunction() throws Exception {
        //GIVEN
        xmEntityService.save(new XmEntity().typeKey("TEST_ENTITY_WITH_ANONYMOUS_FUNCTION").key(UUID.randomUUID()).name("test"));
        String testContent = "{\"testKey\": \"testValue\"}";

        //WHEN
        ResultActions functionWithAnonymousFlagNotExplicitlySetCallResult = mockMvc.perform(
            post("/api/functions/anonymous/{functionName}",
                "FUNCTION_WITH_ANONYMOUS_NOT_EXPLICITLY_SET")
                .content(testContent)
                .contentType(APPLICATION_JSON_VALUE));
        ResultActions functionWithAnonymousFlagSetToFalse = mockMvc.perform(
            post("/api/functions/anonymous/{functionName}",
                "FUNCTION_WITH_ANONYMOUS_SET_TO_FALSE")
                .content(testContent)
                .contentType(APPLICATION_JSON_VALUE));
        ResultActions functionWithAnonymousFlagSetToTrue = mockMvc.perform(
            post("/api/functions/anonymous/{functionName}",
                "FUNCTION_WITH_ANONYMOUS_SET_TO_TRUE")
                .content(testContent)
                .contentType(APPLICATION_JSON_VALUE));

        //THEN
        functionWithAnonymousFlagNotExplicitlySetCallResult
            .andExpect(status().is4xxClientError())
            .andExpect(header().string("content-type", "application/json;charset=UTF-8"));
        functionWithAnonymousFlagSetToFalse
            .andExpect(status().is4xxClientError())
            .andExpect(header().string("content-type", "application/json;charset=UTF-8"));
        functionWithAnonymousFlagSetToTrue
            .andExpect(status().is2xxSuccessful())
            .andExpect(header().string("content-type", "application/json;charset=UTF-8"))
            .andExpect(content().json("{\"data\":{\"someKey\":\"someValue\"}}"));
    }

    @Test
    @Transactional
    public void functionWithNullResult() throws Exception {
        ResultActions resultActions = mockMvc.perform(
                get("/api/functions/{functionName}", "FUNCTION_WITH_NULL_RESULT"));
        resultActions.andExpect(status().is2xxSuccessful());
    }

    @Test
    @Transactional
    @SneakyThrows
    public void testFunctionWithPackageAndInput() {
        String functionApi = "/api/functions/";
        String functionKey = "package/FUNCTION.PACKAGE-TEST";
        String content = "{\"parameter\": \"value\"}";
        mockMvc.perform(post(functionApi + functionKey).content(content).contentType(APPLICATION_JSON_VALUE))
               .andDo(print())
               .andExpect(jsonPath("$.data.functionKey").value(functionKey))
               .andExpect(jsonPath("$.data.functionInput.parameter").value("value"))
               .andExpect(status().is2xxSuccessful());

        mockMvc.perform(put(functionApi + functionKey).content(content).contentType(APPLICATION_JSON_VALUE))
               .andDo(print())
               .andExpect(jsonPath("$.data.functionKey").value(functionKey))
               .andExpect(jsonPath("$.data.functionInput.parameter").value("value"))
               .andExpect(status().is2xxSuccessful());

        mockMvc.perform(get(functionApi + functionKey + "?parameter=value"))
               .andDo(print())
               .andExpect(jsonPath("$.data.functionKey").value(functionKey))
               .andExpect(jsonPath("$.data.functionInput.parameter").value("value"))
               .andExpect(status().is2xxSuccessful());

        Long id = xmEntityService.save(new XmEntity().typeKey("TEST_FUNCTION_WITH_PACKAGE")).getId();

        String functionWithEntityApi = "/api/xm-entities/" + id +"/functions/";
        String functionWithEntityKey = "package/FUNCTION-WITH-ENTITY.PACKAGE-TEST";
        mockMvc.perform(post(functionWithEntityApi + functionWithEntityKey).content(content)
                                                                           .contentType(APPLICATION_JSON_VALUE))
               .andDo(print())
               .andExpect(jsonPath("$.data.functionKey").value(functionWithEntityKey))
               .andExpect(jsonPath("$.data.functionInput.parameter").value("value"))
               .andExpect(status().is2xxSuccessful());
        mockMvc.perform(get(functionWithEntityApi + functionWithEntityKey + "?parameter=value")
                            .content(content).contentType(APPLICATION_JSON_VALUE))
               .andDo(print())
               .andExpect(jsonPath("$.data.functionKey").value(functionWithEntityKey))
               .andExpect(jsonPath("$.data.functionInput.parameter").value("value"))
               .andExpect(status().is2xxSuccessful());
    }

    @Test
    @Transactional
    @SneakyThrows
    public void testGStringSerialization() {
        String functionPrefix = "/config/tenants/RESINTTEST/entity/lep/function/";
        String functionApi = "/api/functions/";
        String functionKey = "package/FUNCTION.PACKAGE-TEST";
        String funcKey = functionPrefix + "package/Function$$FUNCTION$PACKAGE_TEST$$tenant.groovy";
        leps.onRefresh(funcKey, "def i = 1; return [result: \"gstr${i}ing\"]");
        mockMvc.perform(post(functionApi + functionKey).content("{}").contentType(APPLICATION_JSON_VALUE))
               .andDo(print())
               .andExpect(jsonPath("$.data.result").isString())
               .andExpect(status().is2xxSuccessful());
        leps.onRefresh(funcKey, null);
    }

    @Test
    @Transactional
    @SneakyThrows
    public void testFunctionExposedByCustomPath() {
        String functionPrefix = "/config/tenants/RESINTTEST/entity/lep/function/";
        String functionApi = "/api/functions/";
        String functionKey = "package/FUNCTION.PATH-PACKAGE-TEST";
        String functionLepKey = functionPrefix + "package/Function$$FUNCTION$PATH_PACKAGE_TEST$$tenant.groovy";

        leps.onRefresh(functionLepKey, "[input: lepContext.inArgs.functionInput, method: lepContext.inArgs.httpMethod]");

        // can resolve function by path
        mockMvc.perform(post(functionApi + "custom/urlpath/157/param/29/code42")
                                .content("{\"paramInBody\": 27.5}")
                                .contentType(APPLICATION_JSON_VALUE))
                .andDo(print())
                .andExpect(jsonPath("$.data.input.paramInBody").value(27.5))
                .andExpect(jsonPath("$.data.input.id").value("157"))
                .andExpect(jsonPath("$.data.input.param").value("29"))
                .andExpect(jsonPath("$.data.input.another").value("code42"))
                .andExpect(status().is2xxSuccessful());

        // can resolve function by key
        mockMvc.perform(post(functionApi + functionKey)
                                .content("{\"paramInBody\": 27.5}")
                                .contentType(APPLICATION_JSON_VALUE))
                .andDo(print())
                .andExpect(jsonPath("$.data.input.paramInBody").value(27.5))
                .andExpect(jsonPath("$.data.input.id").doesNotExist())
                .andExpect(jsonPath("$.data.input.param").doesNotExist())
                .andExpect(jsonPath("$.data.input.another").doesNotExist())
                .andExpect(status().is2xxSuccessful());

        leps.onRefresh(functionLepKey, null);
    }

    @Test
    @Transactional
    @SneakyThrows
    public void testAnonymousFunctionExposedByCustomPath() {
        String functionPrefix = "/config/tenants/RESINTTEST/entity/lep/function/";
        String functionApi = "/api/functions/anonymous/";
        String functionKey = "package/FUNCTION_WITH_ANONYMOUS_SET_TO_TRUE.PATH-PACKAGE-TEST";
        String functionLepKey = functionPrefix + "package/AnonymousFunction$$FUNCTION_WITH_ANONYMOUS_SET_TO_TRUE$PATH_PACKAGE_TEST$$tenant.groovy";

        leps.onRefresh(functionLepKey, "[input: lepContext.inArgs.functionInput, method: lepContext.inArgs.httpMethod]");

        // can resolve function by path
        mockMvc.perform(post(functionApi + "another/urlpath/220/with/37.4/and/tes27")
                                .content("{\"paramInBody\": 29.77}")
                                .contentType(APPLICATION_JSON_VALUE))
                .andDo(print())
                .andExpect(jsonPath("$.data.input.paramInBody").value(29.77))
                .andExpect(jsonPath("$.data.input.id").value("220"))
                .andExpect(jsonPath("$.data.input.param").value("37.4"))
                .andExpect(jsonPath("$.data.input.another").value("tes27"))
                .andExpect(status().is2xxSuccessful());

        // can resolve function by key
        mockMvc.perform(post(functionApi + functionKey)
                                .content("{\"paramInBody\": 27.5}")
                                .contentType(APPLICATION_JSON_VALUE))
                .andDo(print())
                .andExpect(jsonPath("$.data.input.paramInBody").value(27.5))
                .andExpect(jsonPath("$.data.input.id").doesNotExist())
                .andExpect(jsonPath("$.data.input.param").doesNotExist())
                .andExpect(jsonPath("$.data.input.another").doesNotExist())
                .andExpect(status().is2xxSuccessful());

        leps.onRefresh(functionLepKey, null);
    }

}
