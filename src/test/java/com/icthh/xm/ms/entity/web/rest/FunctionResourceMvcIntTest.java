package com.icthh.xm.ms.entity.web.rest;

import static com.google.common.collect.ImmutableMap.of;
import static org.junit.Assert.assertArrayEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@Slf4j
@WebMvcTest(controllers = FunctionResource.class)
@ContextConfiguration(classes = {FunctionResource.class, ExceptionTranslator.class})
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
    public void testCallGetFunction() {
        when(functionService.execute("SOME-FUNCTION_KEY.TROLOLO", of("var1", "val1", "var2", "val2"), "GET"))
            .thenReturn(new FunctionContext().data(of("test", "result")));
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
            .thenReturn(new FunctionContext().data(of("test", "result")));
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
    public void testCallGetAnonymousFunction() {
        when(functionService.executeAnonymous("SOME-ANONYMOUS-FUNCTION_KEY.TROLOLO", of("var1", "val1", "var2", "val2"), "GET"))
                .thenReturn(new FunctionContext().data(of("test", "result")));
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
            .thenReturn(new FunctionContext().data(of("test", "result")));
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
            .thenReturn(new FunctionContext().data(of("test", "result")));
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
                .thenReturn(new FunctionContext().data(of("test", "result")));
        mockMvc.perform(delete("/api/functions/SOME-FUNCTION_KEY.TROLOLO")
                                .contentType(TestUtil.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.data.test").value("result"))
                .andExpect(status().isOk());
        verify(functionService).execute(eq("SOME-FUNCTION_KEY.TROLOLO"), isNull(), eq("DELETE"));
    }

    @Test
    @SneakyThrows
    public void testCallStreamFunction() {
        when(functionService.execute(eq("SOME-FUNCTION_KEY.TROLOLO"), any(), anyString()))
            .then((method) -> {
                FunctionContext data = new FunctionContext().data(of("data", new byte[]{101, 102, 103, 104, 42}));
                data.setOnlyData(true);
                return data;
            });
        byte[] response = mockMvc.perform(get("/api/functions/SOME-FUNCTION_KEY.TROLOLO?var1=val1&var2=val2"))
            .andDo(print())
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();

        assertArrayEquals(new byte[]{101, 102, 103, 104, 42}, response);
    }

}
