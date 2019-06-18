package com.icthh.xm.ms.entity.web.rest;

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_AUTH_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.icthh.xm.commons.i18n.error.web.ExceptionTranslator;
import com.icthh.xm.commons.lep.XmLepScriptConfigServerResourceLoader;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.ms.entity.AbstractSpringBootTest;
import java.io.InputStream;
import java.util.List;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;


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

        this.mockMvc = MockMvcBuilders.standaloneSetup(functionResource)
                                                  .setCustomArgumentResolvers(pageableArgumentResolver)
                                                  .setControllerAdvice(exceptionTranslator)
                                                  .setMessageConverters(jacksonMessageConverter).build();

        initLeps(true);
    }

    @After
    public void destroy(){
        initLeps(false);
    }

    void initLeps(boolean loadData) {
        String body = "return [result: lepContext.inArgs.functionInput.files[0].getInputStream().text]";
        leps.onRefresh("/config/tenants/RESINTTEST/entity/lep/function/Function$$UPLOAD$$tenant.groovy", loadData ? body : null);
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
               .andDo(print()
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

}
