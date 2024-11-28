package com.icthh.xm.ms.entity.web.rest;

import com.icthh.xm.commons.i18n.error.web.ExceptionTranslator;
import com.icthh.xm.commons.i18n.spring.service.LocalizationMessageService;
import com.icthh.xm.ms.entity.AbstractWebMvcTest;
import com.icthh.xm.ms.entity.domain.FunctionContext;
import com.icthh.xm.ms.entity.service.FunctionService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.ModelAndView;

import static com.google.common.collect.ImmutableMap.of;
import static com.icthh.xm.ms.entity.config.Constants.MVC_FUNC_RESULT;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@WebMvcTest(controllers = FunctionResource.class)
@ContextConfiguration(classes = {FunctionMvcResource.class, ExceptionTranslator.class})
public class FunctionResourceMvcIntTest extends AbstractWebMvcTest {

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    @MockBean(name = "functionService")
    private FunctionService functionService;

    @MockBean
    private LocalizationMessageService localizationMessageService;

    @Before
    public void setup() {
        // Setup MockMVC to use our Spring Configuration
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    }

    @Test
    @SneakyThrows
    public void testCallMvcAnonymousPostFunctionWithUrlEncodedParams() {
        when(functionService.executeAnonymous("SOME-ANONYMOUS-FUNCTION_KEY.TROLOLO", of("var1", "val1", "var2", "val2"), "POST_URLENCODED"))
            .thenReturn(new FunctionContext().data( of(MVC_FUNC_RESULT, new ModelAndView("redirect:https://google.com"))));
        mockMvc.perform(post("/api/functions/anonymous/mvc/SOME-ANONYMOUS-FUNCTION_KEY.TROLOLO")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("var1", "val1")
                .param("var2", "val2")
            )
            .andDo(print())
            .andExpect(header().string("Location", "https://google.com"))
            .andExpect(status().isFound());
        verify(functionService).executeAnonymous(eq("SOME-ANONYMOUS-FUNCTION_KEY.TROLOLO"), eq(of("var1", "val1", "var2", "val2")), eq("POST_URLENCODED"));
    }

    @Test
    @SneakyThrows
    public void testCallMvcAnonymousGetFunctionWithUrlEncodedParams() {
        when(functionService.executeAnonymous("SOME-ANONYMOUS-FUNCTION_KEY.TROLOLO", of("var1", "val1", "var2", "val2"), "GET"))
            .thenReturn(new FunctionContext().data( of(MVC_FUNC_RESULT, new ModelAndView("redirect:https://google.com"))));
        mockMvc.perform(get("/api/functions/anonymous/mvc/SOME-ANONYMOUS-FUNCTION_KEY.TROLOLO?var1=val1&var2=val2")
                .contentType(MediaType.APPLICATION_JSON)
            )
            .andDo(print())
            .andExpect(header().string("Location", "https://google.com"))
            .andExpect(status().isFound());
        verify(functionService).executeAnonymous(eq("SOME-ANONYMOUS-FUNCTION_KEY.TROLOLO"), eq(of("var1", "val1", "var2", "val2")), eq("GET"));
    }

}
