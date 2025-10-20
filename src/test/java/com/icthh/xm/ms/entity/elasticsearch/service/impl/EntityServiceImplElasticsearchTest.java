package com.icthh.xm.ms.entity.elasticsearch.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.icthh.xm.commons.security.XmAuthenticationContext;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.ms.entity.config.ApplicationProperties;
import com.icthh.xm.ms.entity.config.XmEntityTenantConfigService;
import com.icthh.xm.ms.entity.domain.Link;
import com.icthh.xm.ms.entity.domain.Profile;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.ext.IdOrKey;
import com.icthh.xm.ms.entity.domain.template.TemplateParamsHolder;
import com.icthh.xm.ms.entity.elasticsearch.AbstractElasticSpringBootTest;
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
import com.icthh.xm.ms.entity.service.impl.StartUpdateDateGenerationStrategy;
import com.icthh.xm.ms.entity.service.impl.XmEntityProjectionServiceImpl;
import com.icthh.xm.ms.entity.service.impl.XmEntityServiceImpl;
import com.icthh.xm.ms.entity.service.impl.XmeStorageServiceFacadeImpl;
import com.icthh.xm.ms.entity.service.json.JsonValidationService;
import com.icthh.xm.ms.entity.service.storage.AvatarStorageService;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_AUTH_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import static com.icthh.xm.commons.tenant.TenantContextUtils.getRequiredTenantKeyValue;
import static com.icthh.xm.ms.entity.config.TenantConfigMockConfiguration.getXmEntityTemplatesSpec;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Slf4j
public class EntityServiceImplElasticsearchTest extends AbstractElasticSpringBootTest {

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
    private AvatarStorageService avatarStorageService;

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

        if (!elasticInited) {
            initElasticsearch(tenantContextHolder);
            elasticInited = true;
        }
        cleanElasticsearch(tenantContextHolder);

        MockitoAnnotations.initMocks(this);
        when(authContextHolder.getContext()).thenReturn(context);
        when(context.getRequiredUserKey()).thenReturn("userKey");

        String tenantName = getRequiredTenantKeyValue(tenantContextHolder);
        String config = getXmEntityTemplatesSpec(tenantName);
        String key = applicationProperties.getSpecificationTemplatesPathPattern().replace("{tenantName}", tenantName);
        xmEntityTemplatesSpecService.onRefresh(key, config);

        XmEntityProjectionService xmEntityProjectionService = new XmEntityProjectionServiceImpl(xmEntityProjectionRepository, profileService);

        XmeStorageServiceFacadeImpl storageServiceFacade = new XmeStorageServiceFacadeImpl(storageService, avatarStorageService, attachmentService);

        xmEntityService = new XmEntityServiceImpl(
            xmEntitySpecService,
            xmEntityTemplatesSpecService,
            xmEntityRepository,
            lifecycleService,
            null,
            profileService,
            linkService,
            storageServiceFacade,
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
    @Rollback
    @Transactional
    public void whenLazySetAndProcessingDisabledSourcesWillNotBeSet() {
        XmEntity e1 = xmEntityService.save(new XmEntity().typeKey("TEST_NO_PROCESSING_REFS").name("someName").key("somKey"));
        XmEntity e2 = xmEntityService.save(new XmEntity().typeKey("TEST_NO_PROCESSING_REFS").name("someName").key("somKey"));
        try {
            e1 = xmEntityRepository.findOneById(e1.getId());
            e1.getTargets().add(new Link().typeKey("TEST_NO_PROCESSING_REFS_LINK_KEY").target(e2));
            XmEntity saved = xmEntityService.save(e1);
            xmEntityRepository.saveAndFlush(saved); // init validation
            fail("Expected TransactionSystemException");
        } catch (ConstraintViolationException e) {
            String message = "[ConstraintViolationImpl{interpolatedMessage='must not be null', propertyPath=source, rootBeanClass=class com.icthh.xm.ms.entity.domain.Link, messageTemplate='{jakarta.validation.constraints.NotNull.message}'}]";
            assertEquals(message, e.getConstraintViolations().toString());
        }
        xmEntityRepository.deleteAll(List.of(e1, e2));
        xmEntitySearchRepository.deleteAll();
    }

    @Test
    @Transactional
    @WithMockUser(authorities = "SUPER-ADMIN")
    public void searchByQuery() {
        long id = 101L;
        XmEntity given = createEntity(id, "ACCOUNT.USER");
        given.setId(id);
        xmEntitySearchRepository.save(given);
        xmEntitySearchRepository.refresh();
        Page<XmEntity> result = xmEntityService.search("typeKey:ACCOUNT.USER AND id:" + id, Pageable.unpaged(), null);
        assertThat(result.getContent().size()).isEqualTo(1);
        assertThat(result.getContent().get(0)).isEqualTo(given);
    }

    @Test
    @Transactional
    @WithMockUser(authorities = "SUPER-ADMIN")
    public void searchByTemplate() {
        long id = 102L;
        XmEntity given = createEntity(id, "ACCOUNT.USER");
        given.setId(id);
        xmEntitySearchRepository.save(given);
        xmEntitySearchRepository.refresh();
        TemplateParamsHolder templateParamsHolder = new TemplateParamsHolder();
        templateParamsHolder.getTemplateParams().put("typeKey", "ACCOUNT.USER");
        templateParamsHolder.getTemplateParams().put("id", String.valueOf(id));
        Page<XmEntity> result = xmEntityService.search("BY_TYPEKEY_AND_ID", templateParamsHolder, Pageable.unpaged(), null);
        assertThat(result.getContent().size()).isEqualTo(1);
        assertThat(result.getContent().get(0)).isEqualTo(given);
    }

    @Test
    @Transactional
    @WithMockUser(authorities = "SUPER-ADMIN")
    public void searchByQueryAndTypeKey() {
        long id = 103L;
        XmEntity given = createEntity(id, "ACCOUNT.USER");
        given.setId(id);
        xmEntitySearchRepository.save(given);
        xmEntitySearchRepository.refresh();
        Page<XmEntity> result = xmEntityService.searchByQueryAndTypeKey("103", "ACCOUNT", Pageable.unpaged(), null);
        assertThat(result.getContent().size()).isEqualTo(1);
        assertThat(result.getContent().get(0)).isEqualTo(given);
    }

    @Test
    @Transactional
    @WithMockUser(authorities = "SUPER-ADMIN")
    public void searchByTemplateAndTypeKey() {
        long id = 103L;
        XmEntity given = createEntity(id, "ACCOUNT.USER");
        given.setId(id);
        xmEntitySearchRepository.save(given);
        xmEntitySearchRepository.refresh();
        TemplateParamsHolder templateParamsHolder = new TemplateParamsHolder();
        templateParamsHolder.getTemplateParams().put("typeKey", "ACCOUNT.USER");
        Page<XmEntity> result = xmEntityService.searchByQueryAndTypeKey("BY_TYPEKEY", templateParamsHolder, "ACCOUNT", Pageable.unpaged(), null);
        assertThat(result.getContent().size()).isEqualTo(1);
        assertThat(result.getContent().get(0)).isEqualTo(given);
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
}
