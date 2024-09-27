package com.icthh.xm.ms.entity.web.rest;

import com.icthh.xm.commons.i18n.error.web.ExceptionTranslator;
import com.icthh.xm.commons.lep.XmLepScriptConfigServerResourceLoader;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.ms.entity.AbstractSpringBootTest;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.io.InputStream;

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_AUTH_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WithMockUser(authorities = {"SUPER-ADMIN"})
@Transactional
public class FunctionResourceUploadIntTest extends AbstractSpringBootTest {

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
    private FunctionUploadResource functionUploadResource;

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

    @Autowired
    private WebApplicationContext webApplicationContext;

    @BeforeTransaction
    public void beforeTransaction() {
        TenantContextUtils.setTenant(tenantContextHolder, "RESINTTEST");
    }

    @SneakyThrows
    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        TenantContextUtils.setTenant(tenantContextHolder, "RESINTTEST");
        lepManager.beginThreadContext(ctx -> {
            ctx.setValue(THREAD_CONTEXT_KEY_TENANT_CONTEXT, tenantContextHolder.getContext());
            ctx.setValue(THREAD_CONTEXT_KEY_AUTH_CONTEXT, authContextHolder.getContext());
        });

        this.mockMvc = MockMvcBuilders.standaloneSetup(functionUploadResource, xmEntityResource)
                                                  .setCustomArgumentResolvers(pageableArgumentResolver)
                                                  .setControllerAdvice(exceptionTranslator)
                                                  .setMessageConverters(jacksonMessageConverter, new ByteArrayHttpMessageConverter()).build();

        initLeps(true);
    }

    @After
    public void destroy(){
        initLeps(false);
        lepManager.endThreadContext();
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
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
    public void testFailFunctionWithInvalidUrl() {
        MockMultipartFile file = new MockMultipartFile("file", "orig", "text/plain", "test no json content" .getBytes(UTF_8));
        mockMvc.perform(multipart("/api/functions/some/path").file(file))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$").value("Invalid upload url"));
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

}
