package com.icthh.xm.ms.entity.config.tenant;

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_AUTH_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.google.common.collect.ImmutableMap;
import com.icthh.xm.commons.i18n.error.web.ExceptionTranslator;
import com.icthh.xm.commons.security.XmAuthenticationContext;
import com.icthh.xm.commons.tenant.TenantContext;
import com.icthh.xm.commons.tenant.TenantKey;
import com.icthh.xm.commons.web.spring.TenantInterceptor;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.ms.entity.AbstractSpringBootTest;
import com.icthh.xm.ms.entity.web.rest.XmEntityResource;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.Optional;

/**
 * Test class for the XmEntityResource REST controller.
 *
 * @see XmEntityResource
 */
public class TenantInterceptorIntTest extends AbstractSpringBootTest {

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

    @Autowired
    private LepManager lepManager;

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
    @Ignore //TODO fix when security test will be implemented
    public void testSuccess() throws Exception {
        when(auth.getDetails()).thenReturn(details);
        when(details.getDecodedDetails()).thenReturn(ImmutableMap.builder().put("tenant", "xm").build());

        lepManager.beginThreadContext(threadContext -> {
            TenantContext tenantContext = mock(TenantContext.class);
            when(tenantContext.getTenantKey()).thenReturn(Optional.of(TenantKey.valueOf("xm")));
            threadContext.setValue(THREAD_CONTEXT_KEY_TENANT_CONTEXT, tenantContext);
            threadContext.setValue(THREAD_CONTEXT_KEY_AUTH_CONTEXT, mock(XmAuthenticationContext.class));
        });
        try {
            // Create the XmEntity
            restXmEntityMockMvc.perform(get("/api/xm-entities")).andExpect(status().isOk());
        } finally {
            lepManager.endThreadContext();
        }
    }

}
