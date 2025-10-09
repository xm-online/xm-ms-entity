package com.icthh.xm.ms.entity.web.rest;

import com.icthh.xm.commons.domain.FunctionResult;
import com.icthh.xm.commons.i18n.error.web.ExceptionTranslator;
import com.icthh.xm.commons.i18n.spring.service.LocalizationMessageService;
import com.icthh.xm.commons.web.rest.FunctionApiDocsResource;
import com.icthh.xm.commons.web.rest.FunctionMvcResource;
import com.icthh.xm.commons.web.rest.FunctionResource;
import com.icthh.xm.commons.web.rest.FunctionUploadResource;
import com.icthh.xm.ms.entity.AbstractJupiterWebMvcTest;
import com.icthh.xm.ms.entity.domain.FunctionResultContext;
import com.icthh.xm.ms.entity.service.impl.XmEntityFunctionServiceFacade;
import com.icthh.xm.ms.entity.service.swagger.XmEntityDynamicSwaggerFunctionGeneratorImpl;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@WebMvcTest(controllers = FunctionResource.class)
@ContextConfiguration(classes = {
    FunctionResource.class,
    FunctionApiDocsResource.class,
    FunctionMvcResource.class,
    FunctionUploadResource.class,
    ExceptionTranslator.class
})
public class FunctionResourceMvcIntTest extends AbstractJupiterWebMvcTest {

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    @MockBean
    private XmEntityFunctionServiceFacade functionService;

    @MockBean
    private LocalizationMessageService localizationMessageService;

    @MockBean
    private XmEntityDynamicSwaggerFunctionGeneratorImpl dynamicSwaggerFunctionGenerator;

    @BeforeEach
    public void setup() {
        // Setup MockMVC to use our Spring Configuration
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    }

    @Test
    @SneakyThrows
    public void testCallGetFunction() {
        when(functionService.execute("SOME-FUNCTION_KEY.TROLOLO", of("var1", "val1", "var2", "val2"), "GET"))
            .thenReturn((FunctionResult) new FunctionResultContext().data(of("test", "result")));
        mockMvc.perform(get("/api/functions/SOME-FUNCTION_KEY.TROLOLO?var1=val1&var2=val2"))
            .andDo(print())
            .andExpect(jsonPath("$.data.test").value("result"))
            .andExpect(status().isOk());
        verify(functionService).execute(eq("SOME-FUNCTION_KEY.TROLOLO"), eq(of("var1", "val1", "var2", "val2")), eq("GET"));
    }

    @Test
    @SneakyThrows
    public void testCallPostAnonymousFunction() {
        when(functionService.executeAnonymous("SOME-ANONYMOUS-FUNCTION_KEY.TROLOLO", of("var1", "val1", "var2", "val2"), "POST"))
            .thenReturn((FunctionResult) new FunctionResultContext().data(of("test", "result")));
        mockMvc.perform(post("/api/functions/anonymous/SOME-ANONYMOUS-FUNCTION_KEY.TROLOLO")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content("{\"var1\":\"val1\", \"var2\": \"val2\"}"))
            .andDo(print())
            .andExpect(jsonPath("$.data.test").value("result"))
            .andExpect(status().isCreated());
        verify(functionService).executeAnonymous(eq("SOME-ANONYMOUS-FUNCTION_KEY.TROLOLO"), eq(of("var1", "val1", "var2", "val2")), eq("POST"));
    }

    @Test
    @SneakyThrows
    public void testCallPostAnonymousFunctionWithUrlEncodedParams() {
        when(functionService.executeAnonymous("SOME-ANONYMOUS-FUNCTION_KEY.TROLOLO", of("var1", "val1", "var2", "val2"), "POST_URLENCODED"))
            .thenReturn((FunctionResult) new FunctionResultContext().data(of("test", "result")));
        mockMvc.perform(post("/api/functions/anonymous/SOME-ANONYMOUS-FUNCTION_KEY.TROLOLO")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("var1", "val1")
                .param("var2", "val2")
            )
            .andDo(print())
            .andExpect(jsonPath("$.data.test").value("result"))
            .andExpect(status().isCreated());
        verify(functionService).executeAnonymous(eq("SOME-ANONYMOUS-FUNCTION_KEY.TROLOLO"), eq(of("var1", "val1", "var2", "val2")), eq("POST_URLENCODED"));
    }

    @Test
    @SneakyThrows
    public void testCallPostAnonymousFunctionWithPathWithUrlEncodedParams() {
        when(functionService.executeAnonymous("SOME-ANONYMOUS-FUNCTION_KEY/TROLOLO", of("var1", "val1", "var2", "val2"), "POST_URLENCODED"))
            .thenReturn((FunctionResult) new FunctionResultContext().data(of("test", "result")));
        mockMvc.perform(post("/api/functions/anonymous/SOME-ANONYMOUS-FUNCTION_KEY/TROLOLO")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("var1", "val1")
                .param("var2", "val2")
            )
            .andDo(print())
            .andExpect(jsonPath("$.data.test").value("result"))
            .andExpect(status().isCreated());
        verify(functionService).executeAnonymous(eq("SOME-ANONYMOUS-FUNCTION_KEY/TROLOLO"), eq(of("var1", "val1", "var2", "val2")), eq("POST_URLENCODED"));
    }

    @Test
    @SneakyThrows
    public void testCallPostFunctionWithUrlEncodedParams() {
        when(functionService.execute("SOME-FUNCTION_KEY.TROLOLO", of("var1", "val1", "var2", "val2"), "POST_URLENCODED"))
            .thenReturn((FunctionResult) new FunctionResultContext().data(of("test", "result")));
        mockMvc.perform(post("/api/functions/SOME-FUNCTION_KEY.TROLOLO")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("var1", "val1")
                .param("var2", "val2")
            )
            .andDo(print())
            .andExpect(jsonPath("$.data.test").value("result"))
            .andExpect(status().isCreated());
        verify(functionService).execute(eq("SOME-FUNCTION_KEY.TROLOLO"), eq(of("var1", "val1", "var2", "val2")), eq("POST_URLENCODED"));
    }

    @Test
    @SneakyThrows
    public void testCallMvcAnonymousPostFunctionWithUrlEncodedParams() {
        when(functionService.executeAnonymous("SOME-ANONYMOUS-FUNCTION_KEY.TROLOLO", of("var1", "val1", "var2", "val2"), "POST_URLENCODED"))
            .thenReturn((FunctionResult) new FunctionResultContext().data( of(MVC_FUNC_RESULT, new ModelAndView("redirect:https://google.com"))));
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
            .thenReturn((FunctionResult) new FunctionResultContext().data( of(MVC_FUNC_RESULT, new ModelAndView("redirect:https://google.com"))));
        mockMvc.perform(get("/api/functions/anonymous/mvc/SOME-ANONYMOUS-FUNCTION_KEY.TROLOLO?var1=val1&var2=val2")
                .contentType(MediaType.APPLICATION_JSON)
            )
            .andDo(print())
            .andExpect(header().string("Location", "https://google.com"))
            .andExpect(status().isFound());
        verify(functionService).executeAnonymous(eq("SOME-ANONYMOUS-FUNCTION_KEY.TROLOLO"), eq(of("var1", "val1", "var2", "val2")), eq("GET"));
    }

    @Test
    @SneakyThrows
    public void testCallGetAnonymousFunction() {
        when(functionService.executeAnonymous("SOME-ANONYMOUS-FUNCTION_KEY.TROLOLO", of("var1", "val1", "var2", "val2"), "GET"))
                .thenReturn((FunctionResult) new FunctionResultContext().data(of("test", "result")));
        mockMvc.perform(get("/api/functions/anonymous/SOME-ANONYMOUS-FUNCTION_KEY.TROLOLO?var1=val1&var2=val2"))
                .andDo(print())
                .andExpect(jsonPath("$.data.test").value("result"))
                .andExpect(status().isOk());
        verify(functionService).executeAnonymous(eq("SOME-ANONYMOUS-FUNCTION_KEY.TROLOLO"), eq(of("var1", "val1", "var2", "val2")), eq("GET"));
    }

        @Test
    @SneakyThrows
    public void testCallPutFunction() {
        when(functionService.execute("SOME-FUNCTION_KEY.TROLOLO", of("var1", "val1", "var2", "val2"), "PUT"))
            .thenReturn((FunctionResult) new FunctionResultContext().data(of("test", "result")));
        mockMvc.perform(put("/api/functions/SOME-FUNCTION_KEY.TROLOLO")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content("{\"var1\":\"val1\", \"var2\": \"val2\"}"))
            .andDo(print())
            .andExpect(jsonPath("$.data.test").value("result"))
            .andExpect(status().isOk());
        verify(functionService).execute(eq("SOME-FUNCTION_KEY.TROLOLO"), eq(of("var1", "val1", "var2", "val2")), eq("PUT"));
    }

    @Test
    @SneakyThrows
    public void testCallPatchFunction() {
        when(functionService.execute("SOME-FUNCTION_KEY.TROLOLO", of("var1", "val1", "var2", "val2"), "PATCH"))
            .thenReturn((FunctionResult) new FunctionResultContext().data(of("test", "result")));
        mockMvc.perform(patch("/api/functions/SOME-FUNCTION_KEY.TROLOLO")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content("{\"var1\":\"val1\", \"var2\": \"val2\"}"))
            .andDo(print())
            .andExpect(jsonPath("$.data.test").value("result"))
            .andExpect(status().isOk());
        verify(functionService).execute(eq("SOME-FUNCTION_KEY.TROLOLO"), eq(of("var1", "val1", "var2", "val2")), eq("PATCH"));
    }

    @Test
    @SneakyThrows
    public void testCallDeleteFunction() {
        when(functionService.execute("SOME-FUNCTION_KEY.TROLOLO", null, "DELETE"))
                .thenReturn((FunctionResult) new FunctionResultContext().data(of("test", "result")));
        mockMvc.perform(delete("/api/functions/SOME-FUNCTION_KEY.TROLOLO")
                                .contentType(TestUtil.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.data.test").value("result"))
                .andExpect(status().isOk());
        verify(functionService).execute(eq("SOME-FUNCTION_KEY.TROLOLO"), isNull(), eq("DELETE"));
    }

    @Test
    @SneakyThrows
    public void testCallStreamFunction() {
        FunctionResultContext result = (FunctionResultContext) new FunctionResultContext().data(of("data", new byte[]{101, 102, 103, 104, 42}));
        result.setOnlyData(true);
        when(functionService.execute(eq("SOME-FUNCTION_KEY.TROLOLO"), any(), anyString())).thenReturn(result);
        byte[] response = mockMvc.perform(get("/api/functions/SOME-FUNCTION_KEY.TROLOLO?var1=val1&var2=val2"))
            .andDo(print())
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();

        assertArrayEquals(new byte[]{101, 102, 103, 104, 42}, response);
    }

}
