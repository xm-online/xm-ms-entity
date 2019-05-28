package com.icthh.xm.ms.entity.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.icthh.xm.commons.config.client.service.TenantConfigService;
import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.security.XmAuthenticationContext;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.ms.entity.AbstractSpringBootTest;
import com.icthh.xm.ms.entity.config.ApplicationProperties;
import com.icthh.xm.ms.entity.domain.Link;
import com.icthh.xm.ms.entity.domain.Profile;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.repository.LinkRepository;
import com.icthh.xm.ms.entity.repository.SpringXmEntityRepository;
import com.icthh.xm.ms.entity.repository.UniqueFieldRepository;
import com.icthh.xm.ms.entity.repository.XmEntityRepositoryInternal;
import com.icthh.xm.ms.entity.repository.search.XmEntityPermittedSearchRepository;
import com.icthh.xm.ms.entity.repository.search.XmEntitySearchRepository;
import com.icthh.xm.ms.entity.service.AttachmentService;
import com.icthh.xm.ms.entity.service.LifecycleLepStrategyFactory;
import com.icthh.xm.ms.entity.service.LinkService;
import com.icthh.xm.ms.entity.service.ProfileService;
import com.icthh.xm.ms.entity.service.StorageService;
import com.icthh.xm.ms.entity.service.XmEntitySpecService;
import com.icthh.xm.ms.entity.service.XmEntityTemplatesSpecService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_AUTH_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import static com.icthh.xm.commons.tenant.TenantContextUtils.getRequiredTenantKeyValue;
import static com.icthh.xm.ms.entity.config.TenantConfigMockConfiguration.getXmEntityTemplatesSpec;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EntityServiceImpMaxLinkIntTest extends AbstractSpringBootTest {

    private XmEntityServiceImpl xmEntityService;

    @Autowired
    private ApplicationProperties applicationProperties;

    @Autowired
    private XmEntityRepositoryInternal xmEntityRepository;

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
    private TenantConfigService tenantConfigService;

    @Mock
    private ProfileService profileService;

    @Mock
    private StorageService storageService;

    @Mock
    private XmAuthenticationContextHolder authContextHolder;

    @Mock
    private XmAuthenticationContext context;

    private Profile self;

    private static final String TEST_LINK_KEY = "TEST.LINK";

    private static final String TARGET_TYPE_KEY = "ACCOUNT.USER";

    private static boolean elasticInited = false;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeTransaction
    public void beforeTransaction() {

        TenantContextUtils.setTenant(tenantContextHolder, "RESINTTEST");

        if (!elasticInited) {
            initElasticsearch();
            elasticInited = true;
        }
        cleanElasticsearch();

        MockitoAnnotations.initMocks(this);
        when(authContextHolder.getContext()).thenReturn(context);
        when(context.getRequiredUserKey()).thenReturn("userKey");

        String tenantName = getRequiredTenantKeyValue(tenantContextHolder);
        String config = getXmEntityTemplatesSpec(tenantName);
        String key = applicationProperties.getSpecificationTemplatesPathPattern().replace("{tenantName}", tenantName);
        xmEntityTemplatesSpecService.onRefresh(key, config);

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
            springXmEntityRepository, linkRepository);
        xmEntityService.setSelf(xmEntityService);

        lepManager.beginThreadContext(ctx -> {
            ctx.setValue(THREAD_CONTEXT_KEY_TENANT_CONTEXT, tenantContextHolder.getContext());
            ctx.setValue(THREAD_CONTEXT_KEY_AUTH_CONTEXT, authContextHolder.getContext());
        });
    }

    @Before
    public void before() {
        XmEntity sourceEntity = xmEntityRepository.save(createEntity(1l, TARGET_TYPE_KEY));
        self = new Profile();
        self.setXmentity(sourceEntity);
        when(profileService.getSelfProfile()).thenReturn(self);
    }

    @After
    public void afterTest() {
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
    }

    @Test
    @Transactional
    public void testUpdateTargetsOk() {
        XmEntity savedEntity = createEntity(null, "ACCOUNT.TEST_MAX_LINK");

        XmEntity target1 = xmEntityRepository.save(createEntity(null, "ACCOUNT.USER"));
        XmEntity target2 = xmEntityRepository.save(createEntity(null, "ACCOUNT.USER"));
        XmEntity target3 = xmEntityRepository.save(createEntity(null, "ACCOUNT.USER"));

        Set<Link> targets = new HashSet<>();
        targets.add(createLink(savedEntity, target1, "LINK_KEY_1"));
        targets.add(createLink(savedEntity, target2, "LINK_KEY_2"));
        targets.add(createLink(savedEntity, target3, "LINK_KEY_2"));
        savedEntity.setTargets(targets);

        xmEntityService.save(savedEntity);
    }

    @Test
    @Transactional
    public void testUpdateTargetsOk2() {
        XmEntity savedEntity = createEntity(null, "ACCOUNT.TEST_MAX_LINK_2");

        XmEntity target1 = xmEntityRepository.save(createEntity(2l, "ACCOUNT.USER"));
        XmEntity target2 = xmEntityRepository.save(createEntity(3l, "ACCOUNT.USER"));
        XmEntity target3 = xmEntityRepository.save(createEntity(4l, "ACCOUNT.USER"));

        Set<Link> targets = new HashSet<>();
        targets.add(createLink(savedEntity, target1, "LINK_KEY_1"));
        targets.add(createLink(savedEntity, target2, "LINK_KEY_1"));
        targets.add(createLink(savedEntity, target3, "LINK_KEY_2"));
        targets.add(createLink(savedEntity, target3, "LINK_KEY_2"));
        savedEntity.setTargets(targets);

        xmEntityService.save(savedEntity);
    }

    @Test(expected = BusinessException.class)
    @Transactional
    public void testUpdateTargetsError() {
        XmEntity savedEntity = xmEntityRepository.save(createEntity(null, "ACCOUNT.TEST_MAX_LINK"));
        XmEntity target1 = xmEntityRepository.save(createEntity(1l, "ACCOUNT.USER"));
        XmEntity target2 = xmEntityRepository.save(createEntity(2l, "ACCOUNT.USER"));
        XmEntity target3 = xmEntityRepository.save(createEntity(3l, "ACCOUNT.USER"));
        savedEntity.addTargets(createLink(savedEntity, target1, "LINK_KEY_1"));
        xmEntityRepository.save(savedEntity);
        Set<Link> targets = new HashSet<>();
        targets.add(createLink(savedEntity, target1, "LINK_KEY_1"));
        targets.add(createLink(savedEntity, target2, "LINK_KEY_2"));
        targets.add(createLink(savedEntity, target3, "LINK_KEY_2"));
        savedEntity.setTargets(targets);

        xmEntityService.save(savedEntity);
    }

    @Test(expected = BusinessException.class)
    @Transactional
    public void testUpdateTargetsError2() {
        XmEntity savedEntity = xmEntityRepository.save(createEntity(1l, "ACCOUNT.TEST_MAX_LINK_1"));
        XmEntity target1 = xmEntityRepository.save(createEntity(2l, "ACCOUNT.USER"));
        XmEntity target2 = xmEntityRepository.save(createEntity(3l, "ACCOUNT.USER"));
        XmEntity target3 = xmEntityRepository.save(createEntity(4l, "ACCOUNT.USER"));
        Set<Link> targets = new HashSet<>();
        targets.add(linkRepository.save(createLink(savedEntity, target1, "LINK_KEY_1")));
        targets.add(linkRepository.save(createLink(savedEntity, target2, "LINK_KEY_1")));
        targets.add(linkRepository.save(createLink(savedEntity, target2, "LINK_KEY_2")));
        targets.add(linkRepository.save(createLink(savedEntity, target3, "LINK_KEY_2")));
        savedEntity.setTargets(targets);
        xmEntityRepository.save(savedEntity);

        savedEntity.addTargets(createLink(savedEntity, target1, "LINK_KEY_3"));
        savedEntity.addTargets(createLink(savedEntity, target1, "LINK_KEY_1"));
        savedEntity.addTargets(createLink(savedEntity, target1, "LINK_KEY_3"));
        xmEntityService.save(savedEntity);
    }

    @Test(expected = BusinessException.class)
    @Transactional
    public void testUpdateSourceEmptyBDError() {
        XmEntity savedEntity = createEntity(null,"ACCOUNT.SOURCE_B");

        createGraph(savedEntity, "ACCOUNT.SOURCE_B", "ACCOUNT.SOURCE_B",
            "ACCOUNT.SOURCE_A", "ACCOUNT.SOURCE_A", false, "");

        xmEntityService.save(savedEntity);

    }

    @Test
    @Transactional
    public void testUpdateSourceEmptyBDOk() {
        XmEntity savedEntity = createEntity(null, "ACCOUNT.SOURCE_B");
        createGraph(savedEntity, "ACCOUNT.SOURCE_B", "ACCOUNT.SOURCE_B",
            "ACCOUNT.SOURCE_A_2", "ACCOUNT.SOURCE_A_2", false, "");
        xmEntityService.save(savedEntity);
    }

    @Test(expected = BusinessException.class)
    @Transactional
    public void testUpdateSourceInBDGraphError1() {
        XmEntity entity = createEntity(null, "ACCOUNT.SOURCE_B");
        xmEntityRepository.save(entity);

        XmEntity source4 = createGraph(entity, "ACCOUNT.SOURCE_B", "ACCOUNT.SOURCE_B",
            "ACCOUNT.SOURCE_A_2", "ACCOUNT.SOURCE_A_2", true, "source4");

        XmEntity savedEntity = createEntity(null, "ACCOUNT.SOURCE_B");
        Link link = createLink(source4, savedEntity, "LINK_KEY_2");
        savedEntity.addSources(link);
        xmEntityService.save(savedEntity);
    }

    @Test
    @Transactional
    public void testUpdateSourceOk1() {
        XmEntity entity = createEntity(null, "ACCOUNT.SOURCE_B");
        xmEntityRepository.save(entity);

        XmEntity source1 = createGraph(entity, "ACCOUNT.SOURCE_B_2", "ACCOUNT.SOURCE_B",
            "ACCOUNT.SOURCE_A_2", "ACCOUNT.TEST_MAX_LINK_1", true, "source1");

        XmEntity savedEntity = createEntity(null, "ACCOUNT.SOURCE_B");
        XmEntity source5 = xmEntityRepository.save(createEntity(6l, "ACCOUNT.SOURCE_B"));
        savedEntity.addSources(createLink(source1, savedEntity, "LINK_KEY_3"));
        savedEntity.addSources(createLink(source5, savedEntity, "LINK_KEY_3"));
        xmEntityService.save(savedEntity);
    }

    @Test
    @Transactional
    public void testUpdateSourceOk2() {
        XmEntity entity = createEntity(null, "ACCOUNT.SOURCE_B");
        xmEntityRepository.save(entity);

        XmEntity source4 = createGraph(entity, "ACCOUNT.SOURCE_B_2", "ACCOUNT.SOURCE_B",
            "ACCOUNT.SOURCE_A_2", "ACCOUNT.TEST_MAX_LINK_1", true, "source4");

        XmEntity savedEntity2 = createEntity(null, "ACCOUNT.SOURCE_B");
        Link link = createLink(source4, savedEntity2, "LINK_KEY_2");
        savedEntity2.addSources(link);
        xmEntityService.save(savedEntity2);
    }

    private XmEntity createGraph(XmEntity target, String sourceTypeKey1, String sourceTypeKey2, String sourceTypeKey3,
                                 String sourceTypeKey4, boolean saveLinks, String returnEntity) {

        XmEntity source1 = xmEntityRepository.save(createEntity(2l, sourceTypeKey1));
        XmEntity source2 = xmEntityRepository.save(createEntity(3l, sourceTypeKey2));
        XmEntity source3 = xmEntityRepository.save(createEntity(4l, sourceTypeKey3));
        XmEntity source4 = xmEntityRepository.save(createEntity(5l, sourceTypeKey4));
        Set<Link> sources = new HashSet<>();
        if (saveLinks) {
            sources.add(linkRepository.save(createLink(source1, target, "LINK_KEY_3")));
            sources.add(linkRepository.save(createLink(source2, target, "LINK_KEY_3")));
            sources.add(linkRepository.save(createLink(source3, target, "LINK_KEY_2")));

            sources.add(linkRepository.save(createLink(source4, target, "LINK_KEY_1")));
            sources.add(linkRepository.save(createLink(source4, target, "LINK_KEY_1")));
            sources.add(linkRepository.save(createLink(source4, target, "LINK_KEY_2")));
            target.setSources(sources);
            xmEntityRepository.save(target);
        } else {
            sources.add(createLink(source1, target, "LINK_KEY_3"));
            sources.add(createLink(source2, target, "LINK_KEY_3"));
            sources.add(createLink(source3, target, "LINK_KEY_2"));

            sources.add(createLink(source4, target, "LINK_KEY_1"));
            sources.add(createLink(source4, target, "LINK_KEY_1"));
            sources.add(createLink(source4, target, "LINK_KEY_2"));
            target.setSources(sources);
        }
        if (returnEntity.equals("source1")) {
            return source1;
        } else if (returnEntity.equals("source4")) {
            return source4;
        }
        return null;
    }

    private Link createLink(XmEntity source, XmEntity target, String linkTypeKey) {
        Link link = new Link();
        link.setSource(source);
        link.setTarget(target);
        link.setStartDate(new Date().toInstant());
        link.setTypeKey(linkTypeKey);
        return link;
    }

    private XmEntity createEntity(Long id, String typeKey) {
        XmEntity entity = new XmEntity();
        if (id != null) {
            entity.setId(id);
        }
        entity.setName("Name");
        entity.setTypeKey(typeKey);
        entity.setStartDate(new Date().toInstant());
        entity.setUpdateDate(new Date().toInstant());
        entity.setStateKey("STATE1");
        entity.setKey("KEY-" + id);
        entity.setData(ImmutableMap.<String, Object>builder()
            .put("AAAAAAAAAA", "BBBBBBBBBB").build());
        return entity;
    }


}
