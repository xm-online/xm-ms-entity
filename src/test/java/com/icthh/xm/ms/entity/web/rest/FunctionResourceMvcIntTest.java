package com.icthh.xm.ms.entity.web.rest;

import static com.google.common.collect.ImmutableMap.of;
import static org.junit.Assert.assertArrayEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.icthh.xm.commons.i18n.error.web.ExceptionTranslator;
import com.icthh.xm.commons.i18n.spring.service.LocalizationMessageService;
import com.icthh.xm.ms.entity.domain.FunctionContext;
import com.icthh.xm.ms.entity.service.FunctionService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import java.util.Map;

@Slf4j
@RunWith(SpringRunner.class)
@WebMvcTest(controllers = FunctionResource.class)
@ContextConfiguration(classes = {FunctionResource.class, ExceptionTranslator.class})
public class FunctionResourceMvcIntTest {

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
        when(functionService.execute("SOME-FUNCTION_KEY.TROLOLO", of("var1", "val1", "var2", "val2")))
            .thenReturn(new FunctionContext().data(of("test", "result")));
        mockMvc.perform(get("/api/functions/SOME-FUNCTION_KEY.TROLOLO?var1=val1&var2=val2"))
            .andDo(print())
            .andExpect(jsonPath("$.data.test").value("result"))
            .andExpect(status().isOk());
        verify(functionService).execute(eq("SOME-FUNCTION_KEY.TROLOLO"), eq(of("var1", "val1", "var2", "val2")));
    }

    @Test
    @SneakyThrows
    public void testCallPutFunction() {
        when(functionService.execute("SOME-FUNCTION_KEY.TROLOLO", of("var1", "val1", "var2", "val2")))
            .thenReturn(new FunctionContext().data(of("test", "result")));
        mockMvc.perform(put("/api/functions/SOME-FUNCTION_KEY.TROLOLO")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content("{\"var1\":\"val1\", \"var2\": \"val2\"}"))
            .andDo(print())
            .andExpect(jsonPath("$.data.test").value("result"))
            .andExpect(status().isOk());
        verify(functionService).execute(eq("SOME-FUNCTION_KEY.TROLOLO"), eq(of("var1", "val1", "var2", "val2")));
    }

    @Test
    @SneakyThrows
    public void testCallStreamFunction() {
        when(functionService.execute(eq("SOME-FUNCTION_KEY.TROLOLO"), any()))
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
