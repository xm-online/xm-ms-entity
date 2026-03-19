package com.icthh.xm.ms.entity.web.rest;

import com.icthh.xm.commons.i18n.error.web.ExceptionTranslator;
import com.icthh.xm.commons.lep.api.LepManagementService;
import com.icthh.xm.commons.security.XmAuthenticationConstants;
import com.icthh.xm.commons.security.internal.XmAuthenticationDetails;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.ms.entity.AbstractJupiterSpringBootTest;
import com.icthh.xm.ms.entity.config.ApplicationProperties;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.ext.IdOrKey;
import com.icthh.xm.ms.entity.service.*;
import com.icthh.xm.ms.entity.service.impl.XmEntityAvatarService;
import com.icthh.xm.ms.entity.service.impl.XmeStorageServiceFacadeImpl;
import com.icthh.xm.ms.entity.service.storage.AvatarStorageService;
import com.icthh.xm.ms.entity.util.AuthTokenUtils;
import com.icthh.xm.ms.entity.util.EntityUtils;
import com.icthh.xm.ms.entity.util.ProfileUtils;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

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
public class XmEntityAvatarResourceIntTest extends AbstractJupiterSpringBootTest {

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
    private AvatarStorageService avatarStorageService;

    @Autowired
    private ProfileService profileService;

    @BeforeAll
    public void setup() {
        TenantContextUtils.setTenant(tenantContextHolder, "RESINTTEST");
        lepManagementService.beginThreadContext();

        XmeStorageServiceFacade storage = new XmeStorageServiceFacadeImpl(null, avatarStorageService, null);
        avatarService = new XmEntityAvatarService(xmEntityService, applicationProperties, storage);

        XmEntityAvatarResource xmEntityAvatarResource = new XmEntityAvatarResource(avatarService);
        this.avatarResourceMockMvc = MockMvcBuilders.standaloneSetup(xmEntityAvatarResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setMessageConverters(jacksonMessageConverter).build();
    }

    @AfterAll
    public void tearDown() throws Exception {
        lepManagementService.endThreadContext();
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
    }

    @Test
    @Transactional
    public void shouldReturnDefaultRelativeUrl() throws Exception {
        XmEntity entity = xmEntityService.save(EntityUtils.newEntity(e -> {
            e.setTypeKey("ACCOUNT.ADMIN");
            e.setVersion(1);
            e.setKey("K:123");
            e.setName("Name");
            e.setData(Map.of("AAAAAAAAAA", "BBBBBBBBBB"));
        }));

        profileService.save(ProfileUtils.newProfile(p -> {
            p.setUserKey(entity.getKey());
            p.setXmentity(entity);
        }));

        var auth = mock(XmAuthenticationDetails.class);
        when(auth.getDecodedDetails()).thenReturn(new HashMap<>(Map.of(XmAuthenticationConstants.AUTH_DETAILS_USER_KEY, entity.getKey())));

        SecurityContextHolder.getContext().setAuthentication(AuthTokenUtils.newToken(t -> {
            t.setDetails(auth);
        }));

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
