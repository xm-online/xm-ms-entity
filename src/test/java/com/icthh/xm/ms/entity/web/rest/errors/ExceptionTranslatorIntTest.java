package com.icthh.xm.ms.entity.web.rest.errors;

import com.icthh.xm.commons.exceptions.ErrorConstants;
import com.icthh.xm.commons.i18n.error.web.ExceptionTranslator;
import com.icthh.xm.commons.lep.XmLepScriptConfigServerResourceLoader;
import com.icthh.xm.commons.security.XmAuthenticationContext;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.ms.entity.AbstractSpringBootTest;
import com.icthh.xm.ms.entity.web.rest.error.EntityExceptionTranslator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static com.icthh.xm.commons.i18n.I18nConstants.LANGUAGE;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_AUTH_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test class for the ExceptionTranslator controller advice.
 *
 * @see ExceptionTranslator
 */
public class ExceptionTranslatorIntTest extends AbstractSpringBootTest {

    @Autowired
    private ExceptionTranslatorTestController controller;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @Mock
    private XmAuthenticationContext context;

    @Autowired
    private EntityExceptionTranslator entityExceptionTranslator;

    @Autowired
    private XmLepScriptConfigServerResourceLoader lepLoader;

    @Autowired
    private TenantContextHolder tenantContextHolder;

    private MockMvc mockMvc;

    @Autowired
    private LepManager lepManager;

    @Mock
    private XmAuthenticationContextHolder authContextHolder;

    @BeforeTransaction
    public void beforeTransaction() {
        TenantContextUtils.setTenant(tenantContextHolder, "RESINTTEST");
    }


    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(exceptionTranslator, entityExceptionTranslator)
            .build();

        when(context.hasAuthentication()).thenReturn(true);
        when(context.getLogin()).thenReturn(Optional.of("testLogin"));
        when(context.getUserKey()).thenReturn(Optional.of("AAAAAAA"));
        when(context.getDetailsValue(LANGUAGE)).thenReturn(Optional.of("en"));

        when(authContextHolder.getContext()).thenReturn(context);

        lepManager.beginThreadContext(ctx -> {
            ctx.setValue(THREAD_CONTEXT_KEY_TENANT_CONTEXT, tenantContextHolder.getContext());
            ctx.setValue(THREAD_CONTEXT_KEY_AUTH_CONTEXT, authContextHolder.getContext());
        });
    }

    @After
    public void destroy() {
        lepManager.endThreadContext();
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
    }

    @Test
    public void testConcurrencyFailure() throws Exception {
        mockMvc.perform(get("/test/concurrency-failure"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value(ErrorConstants.ERR_CONCURRENCY_FAILURE));
    }

    @Test
    public void testMethodArgumentNotValid() throws Exception {
        mockMvc.perform(post("/test/method-argument").content("{}").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value(ErrorConstants.ERR_VALIDATION))
            .andExpect(jsonPath("$.error_description").value("Input parameters error"))
            .andExpect(jsonPath("$.fieldErrors.[0].objectName").value("testDTO"))
            .andExpect(jsonPath("$.fieldErrors.[0].field").value("test"))
            .andExpect(jsonPath("$.fieldErrors.[0].message").value("NotNull"));
    }

    @Test
    public void testParameterizedError() throws Exception {
        mockMvc.perform(get("/test/parameterized-error"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("error.business"))
            .andExpect(jsonPath("$.error_description").value("test parameterized error"))
            .andExpect(jsonPath("$.params.param0").value("param0_value"))
            .andExpect(jsonPath("$.params.param1").value("param1_value"));
    }

    @Test
    public void testParameterizedError2() throws Exception {
        mockMvc.perform(get("/test/parameterized-error2"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("error.business"))
            .andExpect(jsonPath("$.error_description").value("test parameterized error"))
            .andExpect(jsonPath("$.params.foo").value("foo_value"))
            .andExpect(jsonPath("$.params.bar").value("bar_value"));
    }

    @Test
    public void testAccessDenied() throws Exception {
        mockMvc.perform(get("/test/access-denied"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value(ErrorConstants.ERR_ACCESS_DENIED))
            .andExpect(jsonPath("$.error_description").value("Access denied"));
    }

    @Test
    public void testMethodNotSupported() throws Exception {
        mockMvc.perform(post("/test/access-denied"))
            .andExpect(status().isMethodNotAllowed())
            .andExpect(jsonPath("$.error").value(ErrorConstants.ERR_METHOD_NOT_SUPPORTED))
            .andExpect(jsonPath("$.error_description").value("Method not supported"));
    }

    @Test
    public void testExceptionWithResponseStatus() throws Exception {
        mockMvc.perform(get("/test/response-status"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("error.400"))
            .andExpect(jsonPath("$.error_description").value("Invalid request"));
    }

    @Test
    public void testInternalServerError() throws Exception {
        mockMvc.perform(get("/test/internal-server-error"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.error").value(ErrorConstants.ERR_INTERNAL_SERVER_ERROR))
            .andExpect(jsonPath("$.error_description").value("Internal server error, please try later"));
    }

    @Test
    @Transactional
    public void testDataViolationError() throws Exception {
        lepLoader.onRefresh("/config/tenants/RESINTTEST/entity/lep/service/translator/ExtractParameters$$around.groovy",
            "return ['field_json_path':'$.email', 'field_value':'sdfasdf@gmail.com', 'entity_type_key':'PARTY.INDIVIDUAL']");

        mockMvc.perform(get("/test/integrity-constraint-violation-error"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("23005"))
            .andExpect(jsonPath("$.error_description").value("error.db.23005"))
            .andExpect(jsonPath("$.params.field_json_path").value("$.email"))
            .andExpect(jsonPath("$.params.field_value").value("sdfasdf@gmail.com"))
            .andExpect(jsonPath("$.params.entity_type_key").value("PARTY.INDIVIDUAL"));

        lepLoader.onRefresh("/config/tenants/RESINTTEST/entity/lep/service/translator/ExtractParameters$$around.groovy",
            "lepContext.lep.proceed(lepContext.lep.getMethodArgValues())");
    }
}
