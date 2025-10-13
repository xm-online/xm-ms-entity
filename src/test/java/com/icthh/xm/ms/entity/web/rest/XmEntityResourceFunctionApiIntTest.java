package com.icthh.xm.ms.entity.web.rest;

import static com.google.common.collect.ImmutableMap.of;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.icthh.xm.commons.i18n.error.web.ExceptionTranslator;
import com.icthh.xm.commons.i18n.spring.service.LocalizationMessageService;
import com.icthh.xm.ms.entity.AbstractJupiterWebMvcTest;
import com.icthh.xm.ms.entity.domain.FunctionResultContext;
import com.icthh.xm.ms.entity.domain.ext.IdOrKey;
import com.icthh.xm.ms.entity.repository.kafka.ProfileEventProducer;
import com.icthh.xm.ms.entity.service.ProfileService;
import com.icthh.xm.ms.entity.service.TenantService;
import com.icthh.xm.ms.entity.service.XmEntityService;
import com.icthh.xm.ms.entity.service.impl.XmEntityFunctionServiceFacade;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@Slf4j
@WebMvcTest(controllers = XmEntityResource.class)
@ContextConfiguration(classes = {XmEntityResource.class, ExceptionTranslator.class})
public class XmEntityResourceFunctionApiIntTest extends AbstractJupiterWebMvcTest {
    @Autowired
    private WebApplicationContext wac;
    private MockMvc mockMvc;
    @MockBean
    private XmEntityFunctionServiceFacade functionService;
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

    private String functionName = "SOME-FUNCTION_KEY.TROLOLO";

    @BeforeEach
    public void setup() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    }

    @Test
    public void executeFunction() throws Exception {
        IdOrKey id = IdOrKey.of("1");
        when(functionService.execute(functionName,
            id, of("var1", "val1", "var2", "val2")))
            .thenReturn((FunctionResultContext) new FunctionResultContext().data(of("test", "result")));
        mockMvc.perform(post("/api/xm-entities/1/functions/" + functionName)
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content("{\"var1\":\"val1\", \"var2\": \"val2\"}"))
            .andDo(print())
            .andExpect(jsonPath("$.data.test").value("result"))
            .andExpect(status().isOk());
    }

    @Test
    public void executeGetFunction() throws Exception {
        IdOrKey id = IdOrKey.of("1");
        when(functionService.execute(functionName,
            id, of("var1", "val1", "var2", "val2")))
            .thenReturn((FunctionResultContext) new FunctionResultContext().data(of("test", "result")));
        mockMvc.perform(get("/api/xm-entities/1/functions/"+functionName+"?var1=val1&var2=val2"))
            .andDo(print())
            .andExpect(jsonPath("$.data.test").value("result"))
            .andExpect(status().isOk());
    }

}
