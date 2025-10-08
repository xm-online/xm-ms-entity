package com.icthh.xm.ms.entity.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.exceptions.EntityNotFoundException;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_AUTH_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import com.icthh.xm.commons.security.XmAuthenticationContext;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import static com.icthh.xm.commons.tenant.TenantContextUtils.getRequiredTenantKeyValue;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.ms.entity.AbstractJupiterSpringBootTest;
import com.icthh.xm.ms.entity.config.ApplicationProperties;
import static com.icthh.xm.ms.entity.config.TenantConfigMockConfiguration.getXmEntityTemplatesSpec;
import com.icthh.xm.ms.entity.config.XmEntityTenantConfigService;
import com.icthh.xm.ms.entity.domain.Attachment;
import com.icthh.xm.ms.entity.domain.Link;
import com.icthh.xm.ms.entity.domain.Profile;
import com.icthh.xm.ms.entity.domain.UniqueField;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.ext.IdOrKey;
import com.icthh.xm.ms.entity.lep.keyresolver.TypeKeyWithExtends;
import com.icthh.xm.ms.entity.repository.EventRepository;
import com.icthh.xm.ms.entity.repository.LinkRepository;
import com.icthh.xm.ms.entity.repository.SpringXmEntityRepository;
import com.icthh.xm.ms.entity.repository.UniqueFieldRepository;
import com.icthh.xm.ms.entity.repository.XmEntityProjectionRepository;
import com.icthh.xm.ms.entity.repository.XmEntityRepositoryInternal;
import com.icthh.xm.ms.entity.repository.search.XmEntityPermittedSearchRepository;
import com.icthh.xm.ms.entity.repository.search.XmEntitySearchRepository;
import com.icthh.xm.ms.entity.service.AttachmentService;
import com.icthh.xm.ms.entity.service.LifecycleLepStrategyFactory;
import com.icthh.xm.ms.entity.service.LinkService;
import com.icthh.xm.ms.entity.service.ProfileService;
import com.icthh.xm.ms.entity.service.SimpleTemplateProcessor;
import com.icthh.xm.ms.entity.service.StorageService;
import com.icthh.xm.ms.entity.service.XmEntityProjectionService;
import com.icthh.xm.ms.entity.service.XmEntitySpecService;
import com.icthh.xm.ms.entity.service.XmEntityTemplatesSpecService;
import com.icthh.xm.ms.entity.service.json.JsonValidationService;
import com.icthh.xm.ms.entity.util.XmHttpEntityUtils;
import static java.time.Instant.now;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Slf4j
public class EntityServiceImplIntTest extends AbstractJupiterSpringBootTest {

    private XmEntityServiceImpl xmEntityService;

    @Autowired
    private ApplicationProperties applicationProperties;

    @Autowired
    private XmEntityRepositoryInternal xmEntityRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private SpringXmEntityRepository springXmEntityRepository;

    @Autowired
    private XmEntitySpecService xmEntitySpecService;

    @Autowired
    private XmEntityTemplatesSpecService xmEntityTemplatesSpecService;

    @Autowired
    private XmEntitySearchRepository xmEntitySearchRepository;

    @Autowired
    private LinkRepository linkRepository;

    @Autowired
    private LinkService linkService;

    @Autowired
    private LifecycleLepStrategyFactory lifecycleService;

    @Autowired
    private AttachmentService attachmentService;

    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Autowired
    private LepManager lepManager;

    @Autowired
    private XmEntityPermittedSearchRepository permittedSearchRepository;

    @Autowired
    private StartUpdateDateGenerationStrategy startUpdateDateGenerationStrategy;

    @Autowired
    private XmEntityTenantConfigService tenantConfigService;

    @Mock
    private ProfileService profileService;

    @Mock
    private StorageService storageService;

    @Mock
    private XmAuthenticationContextHolder authContextHolder;

    @Mock
    private XmAuthenticationContext context;

    @Autowired
    private XmEntityProjectionRepository xmEntityProjectionRepository;

    private Profile self;

    private static final String TEST_LINK_KEY = "TEST.LINK";

    private static final String TARGET_TYPE_KEY = "ACCOUNT.USER";

    private static boolean elasticInited = false;

    @Autowired
    private ObjectMapper objectMapper;

    Locale locale;

    @BeforeTransaction
    public void beforeTransaction() throws IOException {

        TenantContextUtils.setTenant(tenantContextHolder, "RESINTTEST");

        MockitoAnnotations.initMocks(this);
        when(authContextHolder.getContext()).thenReturn(context);
        when(context.getRequiredUserKey()).thenReturn("userKey");

        String tenantName = getRequiredTenantKeyValue(tenantContextHolder);
        String config = getXmEntityTemplatesSpec(tenantName);
        String key = applicationProperties.getSpecificationTemplatesPathPattern().replace("{tenantName}", tenantName);
        xmEntityTemplatesSpecService.onRefresh(key, config);

        XmEntityProjectionService xmEntityProjectionService = new XmEntityProjectionServiceImpl(xmEntityProjectionRepository, profileService);

        xmEntityService = new XmEntityServiceImpl(
            xmEntitySpecService,
            xmEntityTemplatesSpecService,
            xmEntityRepository,
            lifecycleService,
            null,
            profileService,
            linkService,
            storageService,
            attachmentService,
            permittedSearchRepository,
            startUpdateDateGenerationStrategy,
            authContextHolder,
            objectMapper,
            mock(UniqueFieldRepository.class),
            springXmEntityRepository,
            new TypeKeyWithExtends(tenantConfigService),
            new SimpleTemplateProcessor(objectMapper),
            eventRepository,
            mock(JsonValidationService.class),
            xmEntityProjectionService
        );
        xmEntityService.setSelf(xmEntityService);

        lepManager.beginThreadContext(ctx -> {
            ctx.setValue(THREAD_CONTEXT_KEY_TENANT_CONTEXT, tenantContextHolder.getContext());
            ctx.setValue(THREAD_CONTEXT_KEY_AUTH_CONTEXT, authContextHolder.getContext());
        });
    }

    @BeforeEach
    public void before() {
        XmEntity sourceEntity = xmEntityRepository.save(createEntity(1l, TARGET_TYPE_KEY));
        self = new Profile();
        self.setXmentity(sourceEntity);
        when(profileService.getSelfProfile()).thenReturn(self);
        locale = Locale.getDefault();
        Locale.setDefault(Locale.US);
    }

    @AfterEach
    public void afterTest() {
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
        Locale.setDefault(locale);
    }

    @Test
    @Transactional
    public void testGetSequenceNextValString() {
        int incrementValue = 50;

        long seq = xmEntityRepository.getSequenceNextValString("hibernate_sequence");
        long seq2 = xmEntityRepository.getSequenceNextValString("hibernate_sequence");

        assertEquals(seq + incrementValue, seq2);
    }


    @Test
    @Rollback
    @Transactional
    public void whenNormalSetAndProcessingDisabledSourcesWillBeSet() {
        XmEntity e1 = xmEntityService.save(new XmEntity().typeKey("TEST_NO_PROCESSING_REFS").name("someName").key("somKey"));
        XmEntity e2 = xmEntityService.save(new XmEntity().typeKey("TEST_NO_PROCESSING_REFS").name("someName").key("somKey"));
        e1.setTargets(new HashSet<>(Set.of(new Link().typeKey("TEST_NO_PROCESSING_REFS_LINK_KEY").target(e2))));
        e1 = xmEntityService.save(e1);
        XmEntity source = e1.getTargets().iterator().next().getSource();
        assertNotNull(source);
    }

    @Test
    @Rollback
    @Transactional
    public void whenNormalSetAndProcessingEnabledSourcesWillBeSet() {
        XmEntity e1 = xmEntityService.save(new XmEntity().typeKey("TEST_PROCESSING_REFS").name("someName").key("somKey"));
        XmEntity e2 = xmEntityService.save(new XmEntity().typeKey("TEST_PROCESSING_REFS").name("someName").key("somKey"));
        e1.setTargets(new HashSet<>(Set.of(new Link().typeKey("TEST_PROCESSING_REFS_LINK_KEY").target(e2))));
        xmEntityService.save(e1);
        XmEntity source = e1.getTargets().iterator().next().getSource();
        assertNotNull(source);
    }

    @Test
    @Rollback
    @Transactional
    public void whenLazySetAndProcessingEnabledSourcesWillBeSet() {
        XmEntity e1 = xmEntityService.save(new XmEntity().typeKey("TEST_PROCESSING_REFS").name("someName").key("somKey"));
        XmEntity e2 = xmEntityService.save(new XmEntity().typeKey("TEST_PROCESSING_REFS").name("someName").key("somKey"));
        e1.getTargets().add(new Link().typeKey("TEST_PROCESSING_REFS_LINK_KEY").target(e2));
        xmEntityService.save(e1);
        XmEntity source = e1.getTargets().iterator().next().getSource();
        assertNotNull(source);
    }

    @Test
    @Transactional
    public void testGetLinkTargetsSelfKey() throws Exception {
        XmEntity targetEntity = xmEntityRepository.save(createEntity(2l, TARGET_TYPE_KEY));
        Link link = createSelfLink(targetEntity);
        linkRepository.save(link);

        List<Link> result = xmEntityService.getLinkTargets(IdOrKey.SELF, TARGET_TYPE_KEY);

        assertThat(result).isNotNull();
        assertThat(result.size()).isEqualTo(BigInteger.ONE.intValue());
    }

    @Test
    @Transactional
    public void testGetLinkSourcesSelfKey() throws Exception {
        XmEntity targetEntity = xmEntityRepository.save(createEntity(2l, "ACCOUNT.USER"));
        Link link = createSelfLink(targetEntity);
        linkRepository.save(link);

        List<Link> result = xmEntityService.getLinkSources(IdOrKey.of(targetEntity.getId()), TEST_LINK_KEY);

        assertThat(result).isNotNull();
        assertThat(result.size()).isEqualTo(BigInteger.ONE.intValue());
    }

    @Test
    @Transactional
    public void testGetLinkTargetsByEntityId() throws Exception {
        XmEntity sourceEntity = xmEntityRepository.save(createEntity(null, "ACCOUNT"));
        XmEntity targetEntity = xmEntityRepository.save(createEntity(2l, TARGET_TYPE_KEY));
        Link link = createLink(sourceEntity, targetEntity);
        linkRepository.save(link);

        List<Link> targetsLinks = xmEntityService.getLinkTargets(IdOrKey.of(sourceEntity.getId()),
            TARGET_TYPE_KEY);

        assertThat(targetsLinks).isNotEmpty();
        assertThat(targetsLinks.size()).isEqualTo(BigInteger.ONE.intValue());
    }

    @Test
    @Transactional
    public void testGetLinkSourcesByEntityId() throws Exception {
        XmEntity sourceEntity = xmEntityRepository.save(createEntity(null, "ACCOUNT"));
        XmEntity targetEntity = xmEntityRepository.save(createEntity(2l, "ACCOUNT.USER"));
        Link link = createLink(sourceEntity, targetEntity);
        linkRepository.save(link);

        List<Link> sourcesLinks = xmEntityService
            .getLinkSources(IdOrKey.of(targetEntity.getId()), TEST_LINK_KEY);

        assertThat(sourcesLinks).isNotNull();
        assertThat(sourcesLinks.size()).isEqualTo(BigInteger.ONE.intValue());
    }

    @Test
    @Transactional
    public void testFailGetLinkTargetsByKey() throws Exception {
        assertThrows(EntityNotFoundException.class, () -> {
            xmEntityService.getLinkTargets(IdOrKey.of("some key"), "ANY");
        });
    }

    @Test
    @Transactional
    public void testFailGetLinkSourcesByKey() throws Exception {
        assertThrows(EntityNotFoundException.class, () -> {
            xmEntityService.getLinkSources(IdOrKey.of("some key"), "ANY");
        });
    }

    @Test
    @Transactional
    @WithMockUser(authorities = "SUPER-ADMIN")
    public void saveSelfLinkTarget() throws Exception {
        when(storageService.store(Mockito.any(MultipartFile.class), Mockito.any())).thenReturn("test.txt");
        int databaseSizeBeforeCreate = linkRepository.findAll().size();

        XmEntity targetEntity = xmEntityRepository.save(createEntity(2l, TARGET_TYPE_KEY));
        Link link = createSelfLink(targetEntity);

        // Create link with attachment
        MockMultipartFile file =
            new MockMultipartFile("file", "test.txt", "text/plain", "TEST".getBytes());

        xmEntityService.saveLinkTarget(IdOrKey.SELF, link, file);

        // Validate the link in the database
        List<Link> linkList = linkRepository.findAll();
        assertThat(linkList).hasSize(databaseSizeBeforeCreate + BigInteger.ONE.intValue());

        //validate attachment in database
        List<Attachment> attachments = attachmentService.findAll(null);
        assertThat(attachments).hasSize(BigInteger.ONE.intValue());
        Attachment attachment = attachments.stream().findFirst().get();
        assertThat(attachment.getContentUrl()).contains("test.txt");
    }

    @Test
    @Transactional
    @WithMockUser(authorities = "SUPER-ADMIN")
    public void addFileAttachment() {
        when(storageService.store(Mockito.any(MultipartFile.class), Mockito.any())).thenReturn("test.txt");

        XmEntity targetEntity = xmEntityRepository.save(createEntity(2l, TARGET_TYPE_KEY));
        MockMultipartFile file =
            new MockMultipartFile("file", "test.txt", "text/plain", "TEST".getBytes());

        xmEntityService.addFileAttachment(targetEntity, file);

        XmEntity foundEntity = xmEntityService.findOne(IdOrKey.of(targetEntity.getId()));

        assertThat(foundEntity).isNotNull();
        assertThat(foundEntity.getAttachments()).hasSize(BigInteger.ONE.intValue());
        assertThat(foundEntity.getAttachments().stream().findFirst().get().getContentUrl()).isEqualTo("test.txt");
    }

    @Test
    @Transactional
    @WithMockUser(authorities = "SUPER-ADMIN")
    @SneakyThrows
    @SuppressWarnings("unchecked")
    public void updateAvatar() throws Exception {
        when(storageService.store(Mockito.any(HttpEntity.class), Mockito.any())).thenReturn("test.txt");
        MockMultipartFile file =
            new MockMultipartFile("file", "test.jpg", "image/jpg", "TEST".getBytes());
        HttpEntity<Resource> avatarEntity = XmHttpEntityUtils.buildAvatarHttpEntity(file);

        URI uri = xmEntityService.updateAvatar(IdOrKey.SELF, avatarEntity);

        assertThat(uri != null);

        XmEntity storedEntity = xmEntityService.findOne(IdOrKey.SELF);

        assertThat(storedEntity.getAvatarUrl() != null);
    }

    @Test
    @Transactional
    @WithMockUser(authorities = "SUPER-ADMIN")
    public void deleteSelfLinkTarget() {
        XmEntity targetEntity = xmEntityRepository.save(createEntity(2l, TARGET_TYPE_KEY));

        Link link = createLink(self.getXmentity(), targetEntity);
        linkRepository.save(link);

        xmEntityService.deleteLinkTarget(IdOrKey.SELF, link.getId().toString());
        assertThat(linkRepository.findAll()).hasSize(BigInteger.ZERO.intValue());
    }

    @Test
    @Transactional
    @WithMockUser(authorities = "SUPER-ADMIN")
    public void deleteForeignLinkTarget() {
        XmEntity sourceEntity = xmEntityRepository.save(createEntity(25l, "ACCOUNT.USER"));
        XmEntity targetEntity = xmEntityRepository.save(createEntity(2l, TARGET_TYPE_KEY));

        Link link = createLink(sourceEntity, targetEntity);
        linkRepository.save(link);

        assertThrows(BusinessException.class, () -> {
            xmEntityService.deleteLinkTarget(IdOrKey.SELF, link.getId().toString());
        });
    }

    private XmEntity createEntity(Long id, String typeKey) {
        XmEntity entity = new XmEntity();
        entity.setName("Name");
        entity.setTypeKey(typeKey);
        entity.setStartDate(new Date().toInstant());
        entity.setUpdateDate(new Date().toInstant());
        entity.setKey("KEY-" + id);
        entity.setStateKey("STATE1");
        entity.setData(ImmutableMap.<String, Object>builder()
            .put("AAAAAAAAAA", "BBBBBBBBBB").build());
        return entity;
    }

    private Link createSelfLink(XmEntity target) {
        return createLink(self.getXmentity(), target);
    }

    private IdOrKey createSelfIdOrKey() {
        return IdOrKey.of(self.getXmentity().getId());
    }

    private Link createLink(XmEntity source, XmEntity target) {
        Link link = new Link();
        link.setSource(source);
        link.setTarget(target);
        link.setStartDate(new Date().toInstant());
        link.setTypeKey(TEST_LINK_KEY);

        return link;
    }

    private List<XmEntity> saveXmEntities() {
        Map<String, Object> xmEntityData = new HashMap<>();
        xmEntityData.put("key", "value");
        List<XmEntity> entityList = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            XmEntity entity = new XmEntity().typeKey("TEST_SEARCH")
                    .name("A-B" + i)
                    .key("UNIQ-E-F" + i)
                    .data(xmEntityData)
                    .startDate(new Date().toInstant())
                    .updateDate(new Date().toInstant());
            entityList.add(entity);
        }
        return xmEntityRepository.saveAll(entityList);
    }

    @Test
    @Transactional
    public void testUniqueField() {

        XmEntity entity = new XmEntity().typeKey("TEST_UNIQUE_FIELD").key(randomUUID())
            .name("name").startDate(now()).updateDate(now());
        entity.setUniqueFields(new HashSet<>(asList(
            new UniqueField(null, "$.uniqueField", "value", entity.getTypeKey(), entity),
            new UniqueField(null, "$.uniqueField2", "value2", entity.getTypeKey(), entity)
        )));
        xmEntityRepository.saveAndFlush(entity);

        XmEntity entity2 = new XmEntity().typeKey("TEST_UNIQUE_FIELD").key(randomUUID())
            .name("name").startDate(now()).updateDate(now());
        entity2.setUniqueFields(new HashSet<>(asList(
            new UniqueField(null, "$.uniqueField", "value", entity2.getTypeKey(), entity2),
            new UniqueField(null, "$.uniqueField2", "value22", entity2.getTypeKey(), entity2)
        )));
        assertThrows(DataIntegrityViolationException.class, () -> {
            xmEntityRepository.saveAndFlush(entity2);
        });
    }

}
