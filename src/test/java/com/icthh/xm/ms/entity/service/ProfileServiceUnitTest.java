package com.icthh.xm.ms.entity.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.tenant.spring.config.TenantContextConfiguration;
import com.icthh.xm.ms.entity.EntityApp;
import com.icthh.xm.ms.entity.config.SecurityBeanOverrideConfiguration;
import com.icthh.xm.ms.entity.config.WebMvcConfig;
import com.icthh.xm.ms.entity.config.tenant.WebappTenantOverrideConfiguration;
import com.icthh.xm.ms.entity.domain.Profile;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.repository.ProfileRepository;
import com.icthh.xm.ms.entity.repository.search.ProfileSearchRepository;
import com.icthh.xm.ms.entity.repository.search.XmEntitySearchRepository;
import com.icthh.xm.ms.entity.web.rest.XmEntityResourceIntTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
    EntityApp.class,
    SecurityBeanOverrideConfiguration.class,
    WebappTenantOverrideConfiguration.class,
    TenantContextConfiguration.class
})
public class ProfileServiceUnitTest {

    private static final Long ID = 1L;
    private static final String USER_KEY = "test";

    private ProfileRepository profileRepository;

    private ProfileSearchRepository profileSearchRepository;

    private XmEntitySearchRepository entitySearchRepository;

    private ProfileService service;

    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Autowired
    private XmAuthenticationContextHolder authContextHolder;

    @Before
    public void init() {
        TenantContextUtils.setTenant(tenantContextHolder, "TEST");

        profileRepository = mock(ProfileRepository.class);
        profileSearchRepository = mock(ProfileSearchRepository.class);
        entitySearchRepository = mock(XmEntitySearchRepository.class);
        service = new ProfileService(profileRepository, profileSearchRepository, entitySearchRepository, authContextHolder);
    }

    @After
    @Override
    public void finalize() {
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
    }

    @Test
    public void saveProfile() {
        Profile profile = new Profile();
        profile.setId(ID);
        XmEntity xmEntity = new XmEntity();
        xmEntity.setId(ID);
        profile.setXmentity(xmEntity);
        when(profileRepository.save(profile)).thenReturn(profile);
        when(profileSearchRepository.save(profile)).thenReturn(profile);
        when(entitySearchRepository.save(profile.getXmentity())).thenReturn(profile.getXmentity());
        service.save(profile);

        verify(profileRepository).save(profile);
        verify(profileSearchRepository).save(profile);
        verify(entitySearchRepository).save(profile.getXmentity());
    }

    @Test
    public void getProfile() {
        Profile profile = new Profile();
        profile.setId(1L);
        profile.setUserKey(USER_KEY);
        when(profileRepository.findOneByUserKey(USER_KEY)).thenReturn(profile);
        service.getProfile(USER_KEY);

        verify(profileRepository).findOneByUserKey(USER_KEY);
    }

    @Test
    public void getProfileByEntityId() {
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
}
