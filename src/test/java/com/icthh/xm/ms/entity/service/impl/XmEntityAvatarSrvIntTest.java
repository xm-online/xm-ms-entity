package com.icthh.xm.ms.entity.service.impl;

import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.ms.entity.AbstractSpringBootTest;
import com.icthh.xm.ms.entity.domain.Profile;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.ext.IdOrKey;
import com.icthh.xm.ms.entity.repository.XmEntityRepositoryInternal;
import com.icthh.xm.ms.entity.service.ProfileService;
import com.icthh.xm.ms.entity.service.StorageService;
import com.icthh.xm.ms.entity.util.EntityUtils;
import com.icthh.xm.ms.entity.util.XmHttpEntityUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@Slf4j
public class XmEntityAvatarSrvIntTest extends AbstractSpringBootTest {

    private static final String FILE_NAME = "test.jpg";

    private XmEntityAvatarService xmEntityAvatarService;

    @Mock
    private ProfileService profileService;

    @Mock
    private StorageService storageService;

    @Mock
    private XmEntityServiceImpl xmEntityService;

    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Autowired
    private XmEntityRepositoryInternal xmEntityRepository;

    private AutoCloseable mockito;

    private Profile self;

    private Long otherId = 0L;

    Locale locale;

    @BeforeTransaction
    public void beforeTransaction() throws IOException {
        TenantContextUtils.setTenant(tenantContextHolder, "RESINTTEST");

        mockito = MockitoAnnotations.openMocks(this);
        xmEntityAvatarService = new XmEntityAvatarService(xmEntityService, storageService, profileService);
    }

    @AfterTransaction
    public void afterTransaction() {
        try {
            mockito.close();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    @Before
    public void before() {

        //Create SELF profile
        XmEntity sourceEntity = xmEntityRepository.save(
            EntityUtils.newEntity(entity -> {
              entity.setKey("KEY-" + 1L);
              entity.setTypeKey("ACCOUNT.USER");
              entity.setStateKey("STATE1");
            }));
        self = new Profile();
        self.setXmentity(sourceEntity);

        when(profileService.getSelfProfile()).thenReturn(self);

        //Create other entity
        XmEntity otherEntity = xmEntityRepository.save(
            EntityUtils.newEntity(entity -> {
                entity.setKey("KEY-" + 2L);
                entity.setTypeKey("ACCOUNT.USER");
                entity.setStateKey("STATE1");
            }));

        otherId = otherEntity.getId();
        when(xmEntityService.findOne(eq(IdOrKey.of(otherId)), eq(List.of()))).thenReturn(otherEntity);

        locale = Locale.getDefault();
        Locale.setDefault(Locale.US);
    }

    @Test
    @Transactional
    @WithMockUser(authorities = "SUPER-ADMIN")
    public void updateSelfAvatarTest() throws Exception {
        when(storageService.store(Mockito.any(HttpEntity.class), Mockito.any())).thenReturn(FILE_NAME);

        MockMultipartFile file =
            new MockMultipartFile("file", FILE_NAME, "image/jpg", "TEST".getBytes());
        HttpEntity<Resource> avatarEntity = XmHttpEntityUtils.buildAvatarHttpEntity(file);

        URI uri = xmEntityAvatarService.updateAvatar(IdOrKey.SELF, avatarEntity);
        assertThat(uri.getPath()).isEqualTo(FILE_NAME);

        XmEntity storedEntity = xmEntityRepository.findOne(self.getXmentity().getId());
        assertThat(storedEntity.getAvatarUrl()).isEqualTo(FILE_NAME);
    }

    @Test
    @Transactional
    @WithMockUser(authorities = "SUPER-ADMIN")
    public void updateEntityAvatarTest() throws Exception {
        when(storageService.store(Mockito.any(HttpEntity.class), Mockito.any())).thenReturn(FILE_NAME);
        XmEntity storedEntity = xmEntityRepository.findOne(otherId);
        assertThat(storedEntity.getAvatarUrl()).isNull();

        MockMultipartFile file =
            new MockMultipartFile("file", FILE_NAME, "image/jpg", "TEST".getBytes());
        HttpEntity<Resource> avatarEntity = XmHttpEntityUtils.buildAvatarHttpEntity(file);

        URI uri = xmEntityAvatarService.updateAvatar(IdOrKey.of(otherId), avatarEntity);
        assertThat(uri.getPath()).isEqualTo(FILE_NAME);

        storedEntity = xmEntityRepository.findOne(otherId);
        assertThat(storedEntity.getAvatarUrl()).isEqualTo(FILE_NAME);
    }

}
