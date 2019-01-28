package com.icthh.xm.ms.entity.web.rest;

import com.icthh.xm.commons.i18n.error.web.ExceptionTranslator;
import com.icthh.xm.commons.i18n.spring.service.LocalizationMessageService;
import com.icthh.xm.ms.entity.domain.FunctionContext;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.ext.IdOrKey;
import com.icthh.xm.ms.entity.projection.XmEntityIdKeyTypeKey;
import com.icthh.xm.ms.entity.repository.kafka.ProfileEventProducer;
import com.icthh.xm.ms.entity.service.FunctionService;
import com.icthh.xm.ms.entity.service.ProfileService;
import com.icthh.xm.ms.entity.service.TenantService;
import com.icthh.xm.ms.entity.service.XmEntityService;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.Validator;
import org.springframework.web.context.WebApplicationContext;

import static com.google.common.collect.ImmutableMap.of;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@RunWith(SpringRunner.class)
@WebMvcTest(controllers = XmEntityResource.class)
@ContextConfiguration(classes = {XmEntityResource.class, ExceptionTranslator.class})
public class XmEntityResourceFunctionApiIntTest {

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    @MockBean(name = "functionService")
    private FunctionService functionService;

    @MockBean
    private LocalizationMessageService localizationMessageService;
    @MockBean
    private XmEntityService xmEntityService;
    @MockBean
    private ProfileService profileService;
    @MockBean
    private ProfileEventProducer profileEventProducer;
    @MockBean
    private TenantService tenantService;
    @MockBean
    private XmEntityResource xmEntityResource;

    @Before
    public void setup() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    }

    @Test
    public void executeFunction() throws Exception {
        IdOrKey id = IdOrKey.of("1");
        when(functionService.execute("SOME-FUNCTION_KEY.TROLOLO",
            id, of("var1", "val1", "var2", "val2")))
            .thenReturn(new FunctionContext().data(of("test", "result")));
        when(xmEntityService.getXmEntityIdKeyTypeKey(id)).thenReturn(createXmProjection(id));
        mockMvc.perform(post("/api/xm-entities/1/functions/SOME-FUNCTION_KEY.TROLOLO")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content("{\"var1\":\"val1\", \"var2\": \"val2\"}"))
            .andDo(print())
            .andExpect(jsonPath("$.data.test").value("result"))
            .andExpect(status().isOk());
        verify(functionService).execute(eq("SOME-FUNCTION_KEY.TROLOLO"), eq(id),
            eq(of("var1", "val1", "var2", "val2")));
    }

    @Test
    public void executeGetFunction() throws Exception {
        IdOrKey id = IdOrKey.of("1");
        when(functionService.execute("SOME-FUNCTION_KEY.TROLOLO",
            id, of("var1", "val1", "var2", "val2")))
            .thenReturn(new FunctionContext().data(of("test", "result")));
        mockMvc.perform(get("/api/xm-entities/1/functions/SOME-FUNCTION_KEY.TROLOLO?var1=val1&var2=val2"))
            .andDo(print())
            .andExpect(jsonPath("$.data.test").value("result"))
            .andExpect(status().isOk());
        verify(functionService).execute(eq("SOME-FUNCTION_KEY.TROLOLO"), eq(id), eq(of("var1", "val1", "var2", "val2")));
    }

    private XmEntityIdKeyTypeKey createXmProjection(IdOrKey key) {
        return new XmEntityIdKeyTypeKey() {
            @Override
            public Long getId() {
                return key.getId();
            }

            @Override
            public String getKey() {
                return key.getKey();
            }

            @Override
            public String getTypeKey() {
                return "DUMMY";
            }
        };
    }

}
