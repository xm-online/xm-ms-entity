package com.icthh.xm.ms.entity.web.rest;

import com.icthh.xm.commons.i18n.error.web.ExceptionTranslator;
import com.icthh.xm.commons.lep.api.LepEngineSession;
import com.icthh.xm.commons.lep.api.LepManagementService;
import com.icthh.xm.commons.security.XmAuthenticationContext;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.ms.entity.AbstractJupiterSpringBootTest;
import com.icthh.xm.ms.entity.config.ApplicationProperties;
import com.icthh.xm.ms.entity.config.Constants;
import com.icthh.xm.ms.entity.domain.Profile;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.ext.IdOrKey;
import com.icthh.xm.ms.entity.service.ProfileService;
import com.icthh.xm.ms.entity.service.StorageService;
import com.icthh.xm.ms.entity.service.XmEntityService;
import com.icthh.xm.ms.entity.service.impl.XmEntityAvatarService;
import com.icthh.xm.ms.entity.service.storage.AvatarStorageService;
import com.icthh.xm.ms.entity.util.EntityUtils;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static com.icthh.xm.commons.tenant.TenantContextUtils.setTenant;
import static com.icthh.xm.ms.entity.config.Constants.DEFAULT_AVATAR_URL;
import static com.icthh.xm.ms.entity.config.Constants.DEFAULT_AVATAR_URL_PREFIX;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test class for the StorageResource REST controller.
 *
 * @see StorageResource
 */
@Slf4j
@WithMockUser(authorities = "SUPER-ADMIN")
public class XmEntityAvatarResourceIntTest extends AbstractJupiterSpringBootTest {

    @Mock
    private XmEntityAvatarService avatarService;

    @Autowired
    private EntityManager em;

    @Autowired
    private ApplicationProperties applicationProperties;

    @Autowired
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Autowired
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Autowired
    private LepManagementService lepManagementService;

    private MockMvc avatarResourceMockMvc;

    @Autowired
    private XmEntityService xmEntityService;
    @Autowired
    private StorageService storageService;
    @Autowired
    private AvatarStorageService avatarStorageService;

    @BeforeTransaction
    public void beforeTransaction() {
        TenantContextUtils.setTenant(tenantContextHolder, "RESINTTEST");
    }

    private AutoCloseable mocks;

    private LepEngineSession session;
    @Mock
    private XmAuthenticationContext context;

    @Mock
    ProfileService profileService;

    @Mock
    Profile profile;

    @BeforeEach
    public void setup() {
        mocks = MockitoAnnotations.openMocks(this);
        session =  lepManagementService.beginThreadContext();

        when(context.getRequiredUserKey()).thenReturn("userKey");
        setTenant(tenantContextHolder, "RESINTTEST");

        lenient().when(profileService.getSelfProfile()).thenReturn(profile);

        avatarService = new XmEntityAvatarService(xmEntityService, profileService, applicationProperties, avatarStorageService);

        XmEntityAvatarResource xmEntityAvatarResource = new XmEntityAvatarResource(avatarService);
        this.avatarResourceMockMvc = MockMvcBuilders.standaloneSetup(xmEntityAvatarResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setMessageConverters(jacksonMessageConverter).build();
    }

    @AfterEach
    public void tearDown() throws Exception {
        session.close();
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
        mocks.close();
    }

    @Test
    @Transactional
    public void shouldReturnDefaultRelativeUrl() throws Exception {

        when(profile.getXmentity()).thenReturn(EntityUtils.newEntity(e -> e.setId(123L)));

        ResultActions result = avatarResourceMockMvc.perform(MockMvcRequestBuilders.get("/api/xm-entities/self/avatar"));

        result.andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl(DEFAULT_AVATAR_URL_PREFIX + DEFAULT_AVATAR_URL));

    }


    @Test
    @Transactional
    public void shouldStoreAndReturnAvatarForSomeone() throws Exception {

        XmEntity entity = xmEntityService.save(EntityUtils.newEntity(e -> {
            e.setTypeKey("ACCOUNT.ADMIN");
            e.setVersion(1);
            e.setKey("K:123");
            e.setName("Name");
            e.setData(Map.of("AAAAAAAAAA", "BBBBBBBBBB"));
        }));

        assertThat(entity.getAvatarUrl()).isNull();

        MockMultipartFile file =
            new MockMultipartFile("file", "avatar.jpg", "image/jpg", "BEBEBE".getBytes());

        ResultActions result = avatarResourceMockMvc.perform(
            MockMvcRequestBuilders
                .multipart("/api/xm-entities/"+ entity.getId() +"/avatar")
                .file(file));

        entity = xmEntityService.findOne(IdOrKey.of(entity.getId()));

        result.andExpect(status().is2xxSuccessful())
            .andExpect(header().string("Location", entity.getAvatarUrl()));


        result = avatarResourceMockMvc.perform(MockMvcRequestBuilders.get("/api/xm-entities/"+ entity.getId() +"/avatar"));

        result.andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.IMAGE_JPEG))
            .andExpect(content().bytes("BEBEBE".getBytes()));

    }


}
