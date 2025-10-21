package com.icthh.xm.ms.entity.service.impl;

import com.icthh.xm.commons.lep.api.LepManagementService;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.ms.entity.AbstractJupiterSpringBootTest;
import com.icthh.xm.ms.entity.config.ApplicationProperties;
import com.icthh.xm.ms.entity.domain.Profile;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.ext.IdOrKey;
import com.icthh.xm.ms.entity.repository.XmEntityRepositoryInternal;
import com.icthh.xm.ms.entity.service.StorageService;
import com.icthh.xm.ms.entity.service.XmeStorageServiceFacade;
import com.icthh.xm.ms.entity.service.storage.AvatarStorageResponse;
import com.icthh.xm.ms.entity.service.storage.AvatarStorageServiceImpl;
import com.icthh.xm.ms.entity.util.EntityUtils;
import com.icthh.xm.ms.entity.util.XmHttpEntityUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@Slf4j
@ActiveProfiles("avatar-file")
public class XmEntityAvatarFIleSrvIntTest extends AbstractJupiterSpringBootTest {

    private static final String FILE_NAME = "test.jpg";

    private XmEntityAvatarService xmEntityAvatarService;

    @Autowired
    private StorageService storageService;

    @Mock
    private XmEntityServiceImpl xmEntityService;

    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Autowired
    private XmEntityRepositoryInternal xmEntityRepository;

    @Autowired
    private ApplicationProperties applicationProperties;

    @Autowired
    private AvatarStorageServiceImpl avatarStorageService;

    @Autowired
    private LepManagementService lepManagementService;

    private AutoCloseable mockito;

    private Profile self;

    private Long otherId = 0L;

    Locale locale;

    private static final String KEY1 = "KEY-" + 1L;
    private static final String KEY2 = "KEY-" + 2L;

    @BeforeAll
    public void setup() {
        TenantContextUtils.setTenant(tenantContextHolder, "RESINTTEST");
        lepManagementService.beginThreadContext();
    }

    @BeforeTransaction
    public void beforeTransaction() throws IOException {
        mockito = MockitoAnnotations.openMocks(this);
        XmeStorageServiceFacade facade = new XmeStorageServiceFacadeImpl(storageService, avatarStorageService, null);
        xmEntityAvatarService = new XmEntityAvatarService(xmEntityService, applicationProperties, facade);
    }

    @AfterAll
    public void tearDown() throws Exception {
        lepManagementService.endThreadContext();
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
    }


    @AfterTransaction
    public void afterTransaction() {
        try {
            mockito.close();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    @BeforeEach
    public void before() {

        //Create SELF profile
        XmEntity sourceEntity = xmEntityRepository.save(
            EntityUtils.newEntity(entity -> {
              entity.setKey(KEY1);
              entity.setTypeKey("ACCOUNT.USER");
              entity.setStateKey("STATE1");
              entity.setName(KEY1);
              entity.getData().put("AAAAAAAAAA","AAAAAAAAAA");
              entity.startDate(Instant.now());
              entity.updateDate(Instant.now());
            }));
        self = new Profile();
        self.setXmentity(sourceEntity);

        //when(profileService.getSelfProfile()).thenReturn(self);
        when(xmEntityService.findOne(eq(IdOrKey.SELF), eq(List.of()))).thenReturn(sourceEntity);

        //Create other entity
        XmEntity otherEntity = xmEntityRepository.save(
            EntityUtils.newEntity(entity -> {
                entity.setKey(KEY2);
                entity.setTypeKey("ACCOUNT.USER");
                entity.setStateKey("STATE1");
                entity.setName(KEY2);
                entity.getData().put("AAAAAAAAAA","AAAAAAAAAA");
                entity.startDate(Instant.now());
                entity.updateDate(Instant.now());
            }));

        otherId = otherEntity.getId();
        when(xmEntityService.findOne(eq(IdOrKey.of(otherId)), eq(List.of()))).thenReturn(otherEntity);

        locale = Locale.getDefault();
        Locale.setDefault(Locale.US);
    }

    @Test
    @Transactional
    @WithMockUser(authorities = "SUPER-ADMIN")
    public void getSelfAvatarTest() throws Exception {
        AvatarStorageResponse selfAvatar = xmEntityAvatarService.getAvatar(IdOrKey.SELF);
        assertThat(selfAvatar).isNotNull();
        assertThat(selfAvatar.uri().toString()).isEqualTo("http://xm-avatar.rgw.icthh.test:7480/assets/img/anonymous.png");
        assertThat(selfAvatar.avatarResource()).isNull();
        //update avatar
        MockMultipartFile file =
            new MockMultipartFile("file", FILE_NAME, "image/jpg", "TEST".getBytes());
        HttpEntity<Resource> avatarEntity = XmHttpEntityUtils.buildAvatarHttpEntity(file);

        URI uri = xmEntityAvatarService.updateAvatar(IdOrKey.SELF, avatarEntity);
        //
        selfAvatar = xmEntityAvatarService.getAvatar(IdOrKey.SELF);
        assertThat(selfAvatar).isNotNull();
        assertThat(selfAvatar.uri()).isNotNull();
        assertThat(selfAvatar.avatarResource()).isNotNull();
    }

    @Test
    @Transactional
    @WithMockUser(authorities = "SUPER-ADMIN")
    public void getAvatarForEntityTest() throws Exception {
        AvatarStorageResponse selfAvatar = xmEntityAvatarService.getAvatar(IdOrKey.of(otherId));
        assertThat(selfAvatar).isNotNull();
        assertThat(selfAvatar.uri().toString()).isEqualTo("http://xm-avatar.rgw.icthh.test:7480/assets/img/anonymous.png");
        assertThat(selfAvatar.avatarResource()).isNull();
        //update avatar
        MockMultipartFile file =
            new MockMultipartFile("file", FILE_NAME, "image/jpg", "TEST".getBytes());
        HttpEntity<Resource> avatarEntity = XmHttpEntityUtils.buildAvatarHttpEntity(file);

        URI uri = xmEntityAvatarService.updateAvatar(IdOrKey.of(otherId), avatarEntity);
        //
        selfAvatar = xmEntityAvatarService.getAvatar(IdOrKey.of(otherId));
        assertThat(selfAvatar).isNotNull();
        assertThat(selfAvatar.uri()).isNotNull();
        assertThat(selfAvatar.avatarResource()).isNotNull();
    }

    @Test
    @Transactional
    @WithMockUser(authorities = "SUPER-ADMIN")
    public void updateSelfAvatarTest() throws Exception {
        MockMultipartFile file =
            new MockMultipartFile("file", FILE_NAME, "image/jpg", "TEST".getBytes());
        HttpEntity<Resource> avatarEntity = XmHttpEntityUtils.buildAvatarHttpEntity(file);

        URI uri = xmEntityAvatarService.updateAvatar(IdOrKey.SELF, avatarEntity);
        assertThat(uri.toString()).contains(FILE_NAME);

        XmEntity storedEntity = xmEntityRepository.findOne(self.getXmentity().getId());
        assertThat(storedEntity.getAvatarUrl()).contains(FILE_NAME);

        if (applicationProperties.getObjectStorage().getStorageType() == ApplicationProperties.StorageType.FILE) {
            assertThat(storedEntity.getAvatarUrl()).startsWith("file://");
        }
    }

    @Test
    @Transactional
    @WithMockUser(authorities = "SUPER-ADMIN")
    public void updateEntityAvatarTest() throws Exception {
        XmEntity storedEntity = xmEntityRepository.findOne(otherId);
        assertThat(storedEntity.getAvatarUrl()).isNull();

        MockMultipartFile file =
            new MockMultipartFile("file", FILE_NAME, "image/jpg", "TEST".getBytes());
        HttpEntity<Resource> avatarEntity = XmHttpEntityUtils.buildAvatarHttpEntity(file);

        URI uri = xmEntityAvatarService.updateAvatar(IdOrKey.of(otherId), avatarEntity);
        assertThat(uri.toString()).contains(FILE_NAME);

        storedEntity = xmEntityRepository.findOne(otherId);
        assertThat(storedEntity.getAvatarUrl()).contains(FILE_NAME);

        if (ApplicationProperties.StorageType.FILE.equals(applicationProperties.getObjectStorage().getStorageType())) {
            assertThat(storedEntity.getAvatarUrl()).startsWith("file://");
        }
    }

}
