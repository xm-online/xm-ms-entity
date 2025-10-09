package com.icthh.xm.ms.entity.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.ms.entity.AbstractJupiterSpringBootTest;
import com.icthh.xm.ms.entity.domain.Profile;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.repository.ProfileRepository;
import com.icthh.xm.ms.entity.repository.search.XmEntitySearchRepository;
import com.icthh.xm.ms.entity.web.rest.XmEntityResourceIntTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class ProfileServiceIntTest extends AbstractJupiterSpringBootTest {

    private static final Long ID = 1L;
    private static final String USER_KEY = "test";

    @Autowired
    private ProfileRepository profileRepository;

    private XmEntitySearchRepository entitySearchRepository;

    private ProfileService service;

    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Autowired
    private XmAuthenticationContextHolder authContextHolder;

    @BeforeEach
    public void init() {
        TenantContextUtils.setTenant(tenantContextHolder, "TEST");

        entitySearchRepository = mock(XmEntitySearchRepository.class);
        service = new ProfileService(profileRepository, entitySearchRepository, authContextHolder);
    }

    @BeforeTransaction
    public void beforeTransaction() {
        TenantContextUtils.setTenant(tenantContextHolder, "TEST");
    }

    @AfterEach
    public void tearDown() {
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
    }

    @Test
    public void saveProfile() {
        profileRepository = mock(ProfileRepository.class);
        service = new ProfileService(profileRepository, entitySearchRepository, authContextHolder);
        Profile profile = new Profile();
        profile.setId(ID);
        XmEntity xmEntity = new XmEntity();
        xmEntity.setId(ID);
        profile.setXmentity(xmEntity);
        when(profileRepository.save(profile)).thenReturn(profile);
        service.save(profile);

        verify(profileRepository).save(profile);
        verifyNoMoreInteractions(entitySearchRepository);
    }

    @Test
    public void getProfile() {
        profileRepository = mock(ProfileRepository.class);
        service = new ProfileService(profileRepository, entitySearchRepository, authContextHolder);
        Profile profile = new Profile();
        profile.setId(1L);
        profile.setUserKey(USER_KEY);
        when(profileRepository.findOneByUserKey(USER_KEY)).thenReturn(profile);
        service.getProfile(USER_KEY);

        verify(profileRepository).findOneByUserKey(USER_KEY);
    }

    @Test
    public void getProfileByEntityId() {
        profileRepository = mock(ProfileRepository.class);
        service = new ProfileService(profileRepository, entitySearchRepository, authContextHolder);
        Profile profile = new Profile();
        profile.setId(1L);
        profile.setUserKey(USER_KEY);
        XmEntity entity = XmEntityResourceIntTest.createEntity();
        entity.setId(2L);
        profile.setXmentity(entity);
        when(profileRepository.findOneByXmentityId(entity.getId())).thenReturn(profile);
        service.getByXmEntityId(entity.getId());

        verify(profileRepository).findOneByXmentityId(entity.getId());
    }

    @Test
    public void deleteProfile() {
        XmEntity entity = XmEntityResourceIntTest.createEntity();
        entity.setTypeKey("TYPE1");
        Profile profile = new Profile();
        profile.setUserKey(USER_KEY);
        profile.setXmentity(entity);

        profile = profileRepository.saveAndFlush(profile);
        int dbSizeBeforeDeleteProfile = profileRepository.findAll().size();

        service.deleteProfile(profile);
        int dbSizeAfterDeleteProfile = profileRepository.findAll().size();

        assertEquals(dbSizeBeforeDeleteProfile, dbSizeAfterDeleteProfile + 1);
    }
}
