package com.icthh.xm.ms.entity.web.rest;

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_AUTH_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.commons.exceptions.spring.web.ExceptionTranslator;
import com.icthh.xm.commons.security.XmAuthenticationContext;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.ms.entity.EntityApp;
import com.icthh.xm.ms.entity.config.SecurityBeanOverrideConfiguration;
import com.icthh.xm.ms.entity.config.tenant.WebappTenantOverrideConfiguration;
import com.icthh.xm.ms.entity.domain.Profile;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.service.ProfileService;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import javax.persistence.EntityManager;

/**
 * Test class for the ProfileResource REST controller.
 *
 * @see ProfileResource
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
    EntityApp.class,
    SecurityBeanOverrideConfiguration.class,
    WebappTenantOverrideConfiguration.class,
    ProfileResourceIntTest.class
})
@Configuration
public class ProfileResourceIntTest {

    private static final String DEFAULT_USER_KEY = "AAAAAAAAAA";

    @Autowired
    private ProfileService profileService;

    @Autowired
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Autowired
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @Autowired
    private EntityManager em;

    @Autowired
    private LepManager lepManager;

    @Autowired
    private TenantContextHolder tenantContextHolder;
    @Autowired
    private XmAuthenticationContextHolder authContextHolder;

    private MockMvc restProfileMockMvc;
    private Profile profile;
    private XmEntity entity;

    @BeforeTransaction
    public void beforeTransaction() {
        TenantContextUtils.setTenant(tenantContextHolder, "RESINTTEST");
    }

    @Bean
    @Primary
    public XmAuthenticationContextHolder xmAuthenticationContextHolder() {
        XmAuthenticationContext context = mock(XmAuthenticationContext.class);
        when(context.hasAuthentication()).thenReturn(true);
        when(context.isFullyAuthenticated()).thenReturn(true);
        when(context.getUserKey()).thenReturn(Optional.of(DEFAULT_USER_KEY));
        when(context.getRequiredUserKey()).thenReturn(DEFAULT_USER_KEY);


        XmAuthenticationContextHolder holder = mock(XmAuthenticationContextHolder.class);
        when(holder.getContext()).thenReturn(context);

        return holder;
    }

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        final ProfileResource profileResource = new ProfileResource(profileService);
        this.restProfileMockMvc = MockMvcBuilders.standaloneSetup(profileResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setMessageConverters(jacksonMessageConverter).build();

        lepManager.beginThreadContext(ctx -> {
            ctx.setValue(THREAD_CONTEXT_KEY_TENANT_CONTEXT, tenantContextHolder.getContext());
            ctx.setValue(THREAD_CONTEXT_KEY_AUTH_CONTEXT, authContextHolder.getContext());
        });

        entity = XmEntityResourceIntTest.createEntity(em);
        profile = createEntity(em, entity);
    }

    @After
    @Override
    public void finalize() {
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
        lepManager.endThreadContext();
    }

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Profile createEntity(EntityManager em, XmEntity xmEntity) {
        Profile profile = new Profile()
            .userKey(DEFAULT_USER_KEY);
        // Add required entity

        em.persist(xmEntity);
        em.flush();
        profile.setXmentity(xmEntity);
        return profile;
    }

    @Test
    @Transactional
    public void getProfile() throws Exception {
        // Initialize the database
        profileService.save(profile);

        // Get the profile
        restProfileMockMvc.perform(get("/api/profile"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.name").value(entity.getName()));
    }

    @Test
    @Transactional
    public void getProfileNotFound() throws Exception {
        restProfileMockMvc.perform(get("/api/profile"))
            .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void getProfileByXmEntityId() throws Exception {
        profileService.save(profile);

        Profile profileByEntityId = profileService.getByXmEntityId(profile.getXmentity().getId());
        Assert.assertNotNull(profileByEntityId);
        Assert.assertEquals(profile.getId(), profileByEntityId.getId());
        Assert.assertEquals(profile.getXmentity().getId(), profileByEntityId.getXmentity().getId());
    }

    @Test
    @Transactional
    public void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Profile.class);
        Profile profile1 = new Profile();
        profile1.setId(1L);
        Profile profile2 = new Profile();
        profile2.setId(profile1.getId());
        assertThat(profile1).isEqualTo(profile2);
        profile2.setId(2L);
        assertThat(profile1).isNotEqualTo(profile2);
        profile1.setId(null);
        assertThat(profile1).isNotEqualTo(profile2);
    }

}
