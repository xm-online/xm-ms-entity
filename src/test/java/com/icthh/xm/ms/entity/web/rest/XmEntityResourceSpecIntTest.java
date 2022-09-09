package com.icthh.xm.ms.entity.web.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.icthh.xm.commons.i18n.error.web.ExceptionTranslator;
import com.icthh.xm.commons.security.XmAuthenticationContext;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.ms.entity.AbstractSpringBootTest;
import com.icthh.xm.ms.entity.config.ApplicationProperties;
import com.icthh.xm.ms.entity.config.InternalTransactionService;
import com.icthh.xm.ms.entity.config.XmEntityTenantConfigService;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.lep.keyresolver.TypeKeyWithExtends;
import com.icthh.xm.ms.entity.repository.*;
import com.icthh.xm.ms.entity.repository.kafka.ProfileEventProducer;
import com.icthh.xm.ms.entity.repository.search.XmEntityPermittedSearchRepository;
import com.icthh.xm.ms.entity.repository.search.XmEntitySearchRepository;
import com.icthh.xm.ms.entity.service.*;
import com.icthh.xm.ms.entity.service.impl.StartUpdateDateGenerationStrategy;
import com.icthh.xm.ms.entity.service.impl.XmEntityServiceImpl;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Validator;

import javax.persistence.EntityManager;
import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_AUTH_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import static com.icthh.xm.commons.tenant.TenantContextUtils.getRequiredTenantKeyValue;
import static com.icthh.xm.ms.entity.config.TenantConfigMockConfiguration.getXmEntityTemplatesSpec;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test class for the XmEntityResource REST controller.
 *
 * @see XmEntityResource
 */
@Slf4j
@WithMockUser(authorities = {"SUPER-ADMIN"})
public class XmEntityResourceSpecIntTest extends AbstractSpringBootTest {

    private static final String DEFAULT_KEY = "AAAAAAAAAA";
    private static final String UPDATED_KEY = "BBBBBBBBBB";

    public static final String DEFAULT_TYPE_KEY = "ACCOUNT.ADMIN";
    private static final String UPDATED_TYPE_KEY = "ACCOUNT.OWNER";

    private static final String DEFAULT_STATE_KEY = "STATE2";
    private static final String UPDATED_STATE_KEY = "STATE3";

    private static final String DEFAULT_NAME = "AAAAAAAAAA";
    private static final String UPDATED_NAME = "BBBBBBBBBB";

    private static final Instant DEFAULT_START_DATE = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_START_DATE = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_UPDATE_DATE = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_UPDATE_DATE = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant MOCKED_START_DATE = Instant.ofEpochMilli(42L);
    private static final Instant MOCKED_UPDATE_DATE = Instant.ofEpochMilli(84L);

    private static final Instant DEFAULT_END_DATE = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_END_DATE = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String DEFAULT_AVATAR_URL = "http://hello.rgw.icthh.test/aaaaa.jpg";
    private static final String UPDATED_AVATAR_URL = "http://hello.rgw.icthh.test/bbbbb.jpg";

    private static final String DEFAULT_DESCRIPTION = "AAAAAAAAAA";
    private static final String UPDATED_DESCRIPTION = "BBBBBBBBBB";

    private static final Map<String, Object> DEFAULT_DATA = ImmutableMap.<String, Object>builder()
        .put("AAAAAAAAAA", "BBBBBBBBBB").build();
    private static final Map<String, Object> UPDATED_DATA = ImmutableMap.<String, Object>builder()
        .put("AAAAAAAAAA", "CCCCCCCCCC").build();

    private static final Boolean DEFAULT_REMOVED = false;
    private static final Boolean UPDATED_REMOVED = true;

    private static boolean elasticInited = false;

    @Autowired
    private ApplicationProperties applicationProperties;

    @Autowired
    private XmEntityRepositoryInternal xmEntityRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private SpringXmEntityRepository springXmEntityRepository;

    private XmEntityServiceImpl xmEntityServiceImpl;

    @Autowired
    private FunctionService functionService;

    @Autowired
    private ProfileService profileService;

    @Autowired
    private ProfileEventProducer profileEventProducer;

    @Autowired
    private XmEntitySearchRepository xmEntitySearchRepository;

    @Autowired
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Autowired
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @Autowired
    private EntityManager em;

    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Autowired
    XmEntitySpecService xmEntitySpecService;

    @Autowired
    XmEntityTemplatesSpecService xmEntityTemplatesSpecService;

    @Autowired
    LifecycleLepStrategyFactory lifeCycleService;

    @Autowired
    XmEntityPermittedRepository xmEntityPermittedRepository;

    @Autowired
    LinkService linkService;

    @Autowired
    StorageService storageService;

    @Autowired
    AttachmentService attachmentService;

    @Autowired
    XmEntityPermittedSearchRepository xmEntityPermittedSearchRepository;

    @Autowired
    XmEntityTenantConfigService tenantConfigService;

    @Autowired
    ObjectMapper objectMapper;

    @Mock
    private XmAuthenticationContextHolder authContextHolder;

    @Mock
    private XmAuthenticationContext context;

    @Spy
    private StartUpdateDateGenerationStrategy startUpdateDateGenerationStrategy;

    @Autowired
    private LepManager lepManager;

    private MockMvc restXmEntityMockMvc;

    @Autowired
    private Validator validator;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private InternalTransactionService transactionService;

    @Autowired
    private JsonValidationService validationService;

    @BeforeTransaction
    public void beforeTransaction() {
        TenantContextUtils.setTenant(tenantContextHolder, "SPECIFICATIONS");
    }

    @SneakyThrows
    @Before
    public void setup() {

        TenantContextUtils.setTenant(tenantContextHolder, "SPECIFICATIONS");

        //initialize index before test - put valid mapping
        if (!elasticInited) {
            initElasticsearch();
            elasticInited = true;
        }
        cleanElasticsearch();

        MockitoAnnotations.initMocks(this);
        when(authContextHolder.getContext()).thenReturn(context);
        when(context.getUserKey()).thenReturn(Optional.of("userKey"));

        when(startUpdateDateGenerationStrategy.generateStartDate()).thenReturn(MOCKED_START_DATE);
        when(startUpdateDateGenerationStrategy.generateUpdateDate()).thenReturn(MOCKED_UPDATE_DATE);

        String tenantName = getRequiredTenantKeyValue(tenantContextHolder);
        String config = getXmEntityTemplatesSpec(tenantName);
        String key = applicationProperties.getSpecificationTemplatesPathPattern().replace("{tenantName}", tenantName);
        xmEntityTemplatesSpecService.onRefresh(key, config);

        XmEntityServiceImpl xmEntityServiceImpl = new XmEntityServiceImpl(xmEntitySpecService,
            xmEntityTemplatesSpecService,
            xmEntityRepository,
            lifeCycleService,
            xmEntityPermittedRepository,
            profileService,
            linkService,
            storageService,
            attachmentService,
            xmEntityPermittedSearchRepository,
            startUpdateDateGenerationStrategy,
            authContextHolder,
            objectMapper,
            mock(UniqueFieldRepository.class),
            springXmEntityRepository,
            new TypeKeyWithExtends(tenantConfigService),
            new SimpleTemplateProcessor(objectMapper),
            eventRepository,
            validationService);

        xmEntityServiceImpl.setSelf(xmEntityServiceImpl);

        this.xmEntityServiceImpl = xmEntityServiceImpl;

        lepManager.beginThreadContext(ctx -> {
            ctx.setValue(THREAD_CONTEXT_KEY_TENANT_CONTEXT, tenantContextHolder.getContext());
            ctx.setValue(THREAD_CONTEXT_KEY_AUTH_CONTEXT, authContextHolder.getContext());
        });
        XmEntityResource resourceMock = mock(XmEntityResource.class);
        when(resourceMock.createXmEntity(any())).thenReturn(ResponseEntity.created(new URI("")).build());
        XmEntityResource xmEntityResourceMock = new XmEntityResource(xmEntityServiceImpl,
            profileService,
            profileEventProducer,
            functionService,
            tenantService,
            resourceMock
        );
        this.restXmEntityMockMvc = MockMvcBuilders.standaloneSetup(xmEntityResourceMock)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setValidator(validator)
            .setMessageConverters(jacksonMessageConverter).build();
    }

    @After
    public void tearDown() {
        lepManager.endThreadContext();
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
    }

    /**
     * Create an entity for this test.
     * <p>
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static XmEntity createEntity(Map data) {
        return new XmEntity()
            .key("KEY")
            .typeKey("DEMO.TEST")
            .stateKey("NEW")
            .name("user")
            .data(data);
    }

    @Before
    public void initTest() {
        //    xmEntitySearchRepository.deleteAll();
    }

    @Test
    @Transactional
    public void testCreateXmEntity() throws Exception {

        int databaseSizeBeforeCreate = xmEntityRepository.findAll().size();
        XmEntity xmEntity = createEntity(Map.of("tenantKey", "tenantCreated"));

        restXmEntityMockMvc.perform(post("/api/xm-entities")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(xmEntity)))
            .andExpect(status().isCreated());

        List<XmEntity> xmEntityList = xmEntityRepository.findAll();
        assertThat(xmEntityList).hasSize(databaseSizeBeforeCreate + 1);
        XmEntity testXmEntity = xmEntityList.get(xmEntityList.size() - 1);
        assertThat(testXmEntity.getKey()).isEqualTo("KEY");
        assertThat(testXmEntity.getTypeKey()).isEqualTo("DEMO.TEST");
        assertThat(testXmEntity.getStateKey()).isEqualTo("NEW");
        assertThat(testXmEntity.getName()).isEqualTo("user");

    }
    @Test
    @Transactional
    public void testDoesNotCreateXmEntityIfJsonNotValid() throws Exception {

        int databaseSizeBeforeCreate = xmEntityRepository.findAll().size();
        XmEntity xmEntity = createEntity(Map.of("tenantKey", 123));

        restXmEntityMockMvc.perform(post("/api/xm-entities")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(xmEntity)))
            .andExpect(status().is4xxClientError());

        List<XmEntity> xmEntityList = xmEntityRepository.findAll();
        assertThat(xmEntityList).hasSize(databaseSizeBeforeCreate);
    }
}
