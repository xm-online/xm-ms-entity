package com.icthh.xm.ms.entity.web.rest;

import static com.icthh.xm.commons.i18n.I18nConstants.LANGUAGE;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_AUTH_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.icthh.xm.commons.i18n.error.web.ExceptionTranslator;
import com.icthh.xm.commons.security.XmAuthenticationContext;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.ms.entity.AbstractJupiterSpringBootTest;
import com.icthh.xm.ms.entity.domain.Profile;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.repository.ProfileRepository;
import com.icthh.xm.ms.entity.repository.search.XmEntitySearchRepository;
import com.icthh.xm.ms.entity.service.ProfileService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Test class for the ProfileResource REST controller.
 *
 * @see ProfileResource
 */
public class ProfileResourceIntTest extends AbstractJupiterSpringBootTest {

    private static final String DEFAULT_USER_KEY = "AAAAAAAAAA";

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
    private ProfileRepository profileRepository;

    @Autowired
    private XmEntitySearchRepository entitySearchRepository;

    @Mock
    private XmAuthenticationContextHolder authContextHolder;

    @Mock
    private XmAuthenticationContext context;

    private MockMvc restProfileMockMvc;
    private Profile profile;
    private XmEntity entity;

    @BeforeTransaction
    public void beforeTransaction() {
        TenantContextUtils.setTenant(tenantContextHolder, "RESINTTEST");
    }

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);


        when(context.hasAuthentication()).thenReturn(true);
        when(context.isFullyAuthenticated()).thenReturn(true);
        when(context.getUserKey()).thenReturn(Optional.of(DEFAULT_USER_KEY));
        when(context.getRequiredUserKey()).thenReturn(DEFAULT_USER_KEY);
        when(context.getDetailsValue(LANGUAGE)).thenReturn(Optional.of("en"));


        when(authContextHolder.getContext()).thenReturn(context);

        profileService = new ProfileService(profileRepository, entitySearchRepository, authContextHolder);

        final ProfileResource profileResource = new ProfileResource(profileService);
        this.restProfileMockMvc = MockMvcBuilders.standaloneSetup(profileResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setMessageConverters(jacksonMessageConverter).build();

        lepManager.beginThreadContext(ctx -> {
            ctx.setValue(THREAD_CONTEXT_KEY_TENANT_CONTEXT, tenantContextHolder.getContext());
            ctx.setValue(THREAD_CONTEXT_KEY_AUTH_CONTEXT, authContextHolder.getContext());
        });

        entity = XmEntityResourceIntTest.createEntity();
        profile = createEntity(em, entity);
    }

    @AfterEach
    public void tearDown() {
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
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
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
        Assertions.assertNotNull(profileByEntityId);
        Assertions.assertEquals(profile.getId(), profileByEntityId.getId());
        Assertions.assertEquals(profile.getXmentity().getId(), profileByEntityId.getXmentity().getId());
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
