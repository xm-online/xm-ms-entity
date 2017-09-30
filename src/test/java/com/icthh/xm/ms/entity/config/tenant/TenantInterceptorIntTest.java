package com.icthh.xm.ms.entity.config.tenant;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.google.common.collect.ImmutableMap;
import com.icthh.xm.commons.errors.ExceptionTranslator;
import com.icthh.xm.ms.entity.EntityApp;
import com.icthh.xm.ms.entity.config.SecurityBeanOverrideConfiguration;
import com.icthh.xm.ms.entity.web.rest.XmEntityResource;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Test class for the XmEntityResource REST controller.
 *
 * @see XmEntityResource
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {EntityApp.class, SecurityBeanOverrideConfiguration.class, WebappTenantOverrideConfiguration.class})
public class TenantInterceptorIntTest {

    @Autowired
    private XmEntityResource xmEntityResource;

    @Autowired
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Autowired
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @Autowired
    private TenantInterceptor tenantInterceptor;

    @Mock
    private OAuth2Authentication auth;

    @Mock
    private OAuth2AuthenticationDetails details;

    private MockMvc restXmEntityMockMvc;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        SecurityContextHolder.getContext().setAuthentication(auth);
        this.restXmEntityMockMvc = MockMvcBuilders.standaloneSetup(xmEntityResource)
            .addInterceptors(tenantInterceptor)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setMessageConverters(jacksonMessageConverter).build();
    }

    @Test
    public void testNoAuth() throws Exception {
        // Create the XmEntity
        restXmEntityMockMvc.perform(get("/api/xm-entities"))
            .andExpect(status().is4xxClientError());
    }

    @Test
    public void testEmptyAuth() throws Exception {
        when(auth.getDetails()).thenReturn(details);
        // Create the XmEntity
        restXmEntityMockMvc.perform(get("/api/xm-entities"))
            .andExpect(status().is4xxClientError());
    }

    @Test
    public void testEmptyDetails() throws Exception {
        when(auth.getDetails()).thenReturn(details);
        when(details.getDecodedDetails()).thenReturn(Collections.emptyMap());
        // Create the XmEntity
        restXmEntityMockMvc.perform(get("/api/xm-entities"))
            .andExpect(status().is4xxClientError());
    }

    @Test
    public void testSuccess() throws Exception {
        when(auth.getDetails()).thenReturn(details);
        when(details.getDecodedDetails()).thenReturn(ImmutableMap.builder().put("tenant", "XM").build());
        // Create the XmEntity
        restXmEntityMockMvc.perform(get("/api/xm-entities"))
            .andExpect(status().isOk());
    }

}
