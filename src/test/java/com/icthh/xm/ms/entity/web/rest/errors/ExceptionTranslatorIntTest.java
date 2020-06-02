package com.icthh.xm.ms.entity.web.rest.errors;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.icthh.xm.commons.exceptions.ErrorConstants;
import com.icthh.xm.commons.i18n.error.web.ExceptionTranslator;
import com.icthh.xm.ms.entity.AbstractSpringBootTest;
import com.icthh.xm.ms.entity.web.rest.error.EntityExceptionTranslator;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

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

    @Autowired
    private EntityExceptionTranslator entityExceptionTranslator;

    private MockMvc mockMvc;

    @Before
    public void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(exceptionTranslator, entityExceptionTranslator)
            .build();
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
    public void testUniqueConstrainError() throws Exception {
        mockMvc.perform(get("/test/unique-constrain-error"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("error.unique.constrain"))
            .andExpect(jsonPath("$.error_description").value("DataIntegrityViolationException"));
    }
}
