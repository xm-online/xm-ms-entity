package com.icthh.xm.ms.entity.web.rest;

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_AUTH_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import static com.icthh.xm.commons.tenant.TenantContextUtils.getRequiredTenantKeyValue;
import static com.icthh.xm.ms.entity.config.TenantConfigMockConfiguration.getXmEntityTemplatesSpec;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.icthh.xm.commons.config.client.service.TenantConfigService;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.commons.i18n.error.web.ExceptionTranslator;
import com.icthh.xm.commons.security.XmAuthenticationContext;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.ms.entity.EntityApp;
import com.icthh.xm.ms.entity.config.ApplicationProperties;
import com.icthh.xm.ms.entity.config.Constants;
import com.icthh.xm.ms.entity.config.InternalTransactionService;
import com.icthh.xm.ms.entity.config.LepConfiguration;
import com.icthh.xm.ms.entity.config.SecurityBeanOverrideConfiguration;
import com.icthh.xm.ms.entity.config.tenant.WebappTenantOverrideConfiguration;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.spec.StateSpec;
import com.icthh.xm.ms.entity.repository.UniqueFieldRepository;
import com.icthh.xm.ms.entity.repository.XmEntityPermittedRepository;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
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
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Validator;

import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.persistence.EntityManager;

/**
 * Test class for the XmEntityResource REST controller.
 *
 * @see XmEntityResource
 */
@Slf4j
@RunWith(SpringRunner.class)
@WithMockUser(authorities = {"SUPER-ADMIN"})
@SpringBootTest(classes = {
    EntityApp.class,
    SecurityBeanOverrideConfiguration.class,
    WebappTenantOverrideConfiguration.class,
    LepConfiguration.class
})
public class XmEntityResourceIntTest {

    private static final String DEFAULT_KEY = "AAAAAAAAAA";
    private static final String UPDATED_KEY = "BBBBBBBBBB";

    private static final String DEFAULT_TYPE_KEY = "ACCOUNT.ADMIN";
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

    @Autowired
    private ApplicationProperties applicationProperties;

    @Autowired
    private XmEntityRepository xmEntityRepository;

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
    TenantConfigService tenantConfigService;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

    @Mock
    private XmAuthenticationContextHolder authContextHolder;

    @Mock
    private XmAuthenticationContext context;

    @Spy
    private StartUpdateDateGenerationStrategy startUpdateDateGenerationStrategy;

    @Autowired
    private LepManager lepManager;

    private MockMvc restXmEntityMockMvc;

    private XmEntity xmEntity;

    @Autowired
    private Validator validator;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private InternalTransactionService transactionService;

    @BeforeTransaction
    public void beforeTransaction() {
        TenantContextUtils.setTenant(tenantContextHolder, "RESINTTEST");
    }

    @SneakyThrows
    @Before
    public void setup() {

        TenantContextUtils.setTenant(tenantContextHolder, "RESINTTEST");

        //initialize index before test - put valid mapping
        elasticsearchTemplate.deleteIndex(XmEntity.class);
        elasticsearchTemplate.createIndex(XmEntity.class);
        elasticsearchTemplate.putMapping(XmEntity.class);

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
                                                      xmEntitySearchRepository,
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
                                                      tenantConfigService,
                                                      mock(UniqueFieldRepository.class));
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

        xmEntity = createEntity();
    }

    @After
    @Override
    public void finalize() {
        lepManager.endThreadContext();
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
    }

    /**
     * Create an entity for this test.
     * <p>
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static XmEntity createEntity() {
        return new XmEntity()
            .key(DEFAULT_KEY)
            .typeKey(DEFAULT_TYPE_KEY)
            .stateKey(DEFAULT_STATE_KEY)
            .name(DEFAULT_NAME)
            .startDate(DEFAULT_START_DATE)
            .updateDate(DEFAULT_UPDATE_DATE)
            .endDate(DEFAULT_END_DATE)
            .avatarUrl(DEFAULT_AVATAR_URL)
            .description(DEFAULT_DESCRIPTION)
            .data(DEFAULT_DATA)
            .removed(DEFAULT_REMOVED);
    }

    @Before
    public void initTest() {
        //    xmEntitySearchRepository.deleteAll();
    }

    @Test
    @Transactional
    public void createXmEntity() throws Exception {

        XmEntity dbXmEntity = transactionService.inNestedTransaction(() -> {

            int databaseSizeBeforeCreate = xmEntityRepository.findAll().size();

            // Create the XmEntity
            restXmEntityMockMvc.perform(post("/api/xm-entities")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(xmEntity)))
                .andExpect(status().isCreated());

            // Validate the XmEntity in the database
            List<XmEntity> xmEntityList = xmEntityRepository.findAll();
            assertThat(xmEntityList).hasSize(databaseSizeBeforeCreate + 1);
            XmEntity testXmEntity = xmEntityList.get(xmEntityList.size() - 1);
            assertThat(testXmEntity.getKey()).isEqualTo(DEFAULT_KEY);
            assertThat(testXmEntity.getTypeKey()).isEqualTo(DEFAULT_TYPE_KEY);
            assertThat(testXmEntity.getStateKey()).isEqualTo(DEFAULT_STATE_KEY);
            assertThat(testXmEntity.getName()).isEqualTo(DEFAULT_NAME);
            assertThat(testXmEntity.getStartDate()).isEqualTo(MOCKED_START_DATE);
            assertThat(testXmEntity.getUpdateDate()).isEqualTo(MOCKED_UPDATE_DATE);
            assertThat(testXmEntity.getEndDate()).isEqualTo(DEFAULT_END_DATE);
            assertThat(testXmEntity.getAvatarUrl()).contains("aaaaa.jpg");
            assertThat(testXmEntity.getDescription()).isEqualTo(DEFAULT_DESCRIPTION);
            assertThat(testXmEntity.getData()).isEqualTo(DEFAULT_DATA);
            assertThat(testXmEntity.isRemoved()).isEqualTo(DEFAULT_REMOVED);

            return testXmEntity;
        }, this::setup);

        // Validate the XmEntity in Elasticsearch
        XmEntity xmEntityEs = xmEntitySearchRepository.findOne(dbXmEntity.getId());
        assertThat(xmEntityEs).isEqualToIgnoringGivenFields(dbXmEntity, "avatarUrlRelative", "avatarUrlFull");
    }

    @Test
    @Transactional
    public void createXmEntityWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = xmEntityRepository.findAll().size();

        // Create the XmEntity with an existing ID
        xmEntity.setId(1L);

        // An entity with an existing ID cannot be created, so this API call must fail
        restXmEntityMockMvc.perform(post("/api/xm-entities")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(xmEntity)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("error.business.idexists"))
            .andExpect(jsonPath("$.error_description").value(notNullValue()))
        ;

        // Validate the Alice in the database
        List<XmEntity> xmEntityList = xmEntityRepository.findAll();
        assertThat(xmEntityList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    public void createXmEntityTenantWithWhitespace() throws Exception {
        int databaseSizeBeforeCreate = xmEntityRepository.findAll().size();

        XmEntity tenant = createEntity();
        tenant.setTypeKey(Constants.TENANT_TYPE_KEY);
        tenant.setName("test name");

        // An entity with an existing ID cannot be created, so this API call must fail
        restXmEntityMockMvc.perform(post("/api/xm-entities")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(tenant)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("error.validation"))
            .andExpect(jsonPath("$.error_description").value(notNullValue()))
        ;

        // Validate the Alice in the database
        List<XmEntity> xmEntityList = xmEntityRepository.findAll();
        assertThat(xmEntityList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    public void checkKeyIsRequired() throws Exception {
        int databaseSizeBeforeTest = xmEntityRepository.findAll().size();
        // set the field null
        xmEntity.setKey(null);

        // Create the XmEntity, which fails.

        restXmEntityMockMvc.perform(post("/api/xm-entities")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(xmEntity)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("error.validation"))
            .andExpect(jsonPath("$.error_description").value(notNullValue()))
            .andExpect(jsonPath("$.fieldErrors[0].objectName").value("xmEntity"))
            .andExpect(jsonPath("$.fieldErrors[0].field").value("key"))
            .andExpect(jsonPath("$.fieldErrors[0].message").value("NotNull"))
        ;

        List<XmEntity> xmEntityList = xmEntityRepository.findAll();
        assertThat(xmEntityList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkTypeKeyIsRequired() throws Exception {
        int databaseSizeBeforeTest = xmEntityRepository.findAll().size();
        // set the field null
        xmEntity.setTypeKey(null);

        // Create the XmEntity, which fails.

        restXmEntityMockMvc.perform(post("/api/xm-entities")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(xmEntity)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("error.validation"))
            .andExpect(jsonPath("$.error_description").value(notNullValue()))
            .andExpect(jsonPath("$.fieldErrors[0].objectName").value("xmEntity"))
            .andExpect(jsonPath("$.fieldErrors[0].field").value("typeKey"))
            .andExpect(jsonPath("$.fieldErrors[0].message").value("NotNull"))
        ;

        List<XmEntity> xmEntityList = xmEntityRepository.findAll();
        assertThat(xmEntityList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkNameIsRequired() throws Exception {
        int databaseSizeBeforeTest = xmEntityRepository.findAll().size();
        // set the field null
        xmEntity.setName(null);

        // Create the XmEntity, which fails.

        restXmEntityMockMvc.perform(post("/api/xm-entities")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(xmEntity)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("error.validation"))
            .andExpect(jsonPath("$.error_description").value(notNullValue()))
            .andExpect(jsonPath("$.fieldErrors[0].objectName").value("xmEntity"))
            .andExpect(jsonPath("$.fieldErrors[0].field").value("name"))
            .andExpect(jsonPath("$.fieldErrors[0].message").value("NotNull"))
        ;

        List<XmEntity> xmEntityList = xmEntityRepository.findAll();
        assertThat(xmEntityList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    @Ignore("see XmEntityResourceExtendedIntTest.checkStartDateIsNotRequired instead")
    public void checkStartDateIsRequired() throws Exception {
        int databaseSizeBeforeTest = xmEntityRepository.findAll().size();
        // set the field null
        xmEntity.setStartDate(null);

        // Create the XmEntity, which fails.

        restXmEntityMockMvc.perform(post("/api/xm-entities")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(xmEntity)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("error.validation"))
            .andExpect(jsonPath("$.error_description").value(notNullValue()))
            .andExpect(jsonPath("$.fieldErrors[0].objectName").value("xmEntity"))
            .andExpect(jsonPath("$.fieldErrors[0].field").value("startDate"))
            .andExpect(jsonPath("$.fieldErrors[0].message").value("NotNullTenantAware"))
        ;

        List<XmEntity> xmEntityList = xmEntityRepository.findAll();
        assertThat(xmEntityList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    @Ignore("see XmEntityResourceExtendedIntTest.checkUpdateDateIsNotRequired instead")
    public void checkUpdateDateIsRequired() throws Exception {
        int databaseSizeBeforeTest = xmEntityRepository.findAll().size();
        // set the field null
        xmEntity.setUpdateDate(null);

        // Create the XmEntity, which fails.

        restXmEntityMockMvc.perform(post("/api/xm-entities")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(xmEntity)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("error.validation"))
            .andExpect(jsonPath("$.error_description").value(notNullValue()))
            .andExpect(jsonPath("$.fieldErrors[0].objectName").value("xmEntity"))
            .andExpect(jsonPath("$.fieldErrors[0].field").value("updateDate"))
            .andExpect(jsonPath("$.fieldErrors[0].message").value("NotNullTenantAware"))
        ;

        List<XmEntity> xmEntityList = xmEntityRepository.findAll();
        assertThat(xmEntityList).hasSize(databaseSizeBeforeTest);
    }


    @Test
    @Transactional
    public void getAllXmEntities() throws Exception {
        // Initialize the database
        xmEntity = xmEntityRepository.saveAndFlush(xmEntity);

        // Get all the xmEntityList
        restXmEntityMockMvc.perform(get("/api/xm-entities?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(xmEntity.getId().intValue())))
            .andExpect(jsonPath("$.[*].key").value(hasItem(DEFAULT_KEY.toString())))
            .andExpect(jsonPath("$.[*].typeKey").value(hasItem(DEFAULT_TYPE_KEY.toString())))
            .andExpect(jsonPath("$.[*].stateKey").value(hasItem(DEFAULT_STATE_KEY.toString())))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME.toString())))
            .andExpect(jsonPath("$.[*].startDate").value(hasItem(DEFAULT_START_DATE.toString())))
            .andExpect(jsonPath("$.[*].updateDate").value(hasItem(DEFAULT_UPDATE_DATE.toString())))
            .andExpect(jsonPath("$.[*].endDate").value(hasItem(DEFAULT_END_DATE.toString())))
            .andExpect(jsonPath("$.[*].avatarUrl").value(hasItem(containsString("aaaaa.jpg"))))
            .andExpect(jsonPath("$.[*].description").value(hasItem(DEFAULT_DESCRIPTION.toString())))
            .andExpect(jsonPath("$.[*].data").value(hasItem(DEFAULT_DATA)))
            .andExpect(jsonPath("$.[*].removed").value(hasItem(DEFAULT_REMOVED.booleanValue())))

            // check that tags are not returned foe XmEntities collection
            .andExpect(jsonPath("$.[*].tags.id").value(everyItem(nullValue())));
    }

    @Test
    @Transactional
    public void getXmEntitiesByIds() throws Exception {
        // Initialize the database
        XmEntity en1 = xmEntityRepository.saveAndFlush(createEntity());
        XmEntity en2 = xmEntityRepository.saveAndFlush(createEntity());
        XmEntity en3 = xmEntityRepository.saveAndFlush(createEntity());

        // Get all the xmEntityList
        restXmEntityMockMvc.perform(get("/api/xm-entities-by-ids?ids={ids}&embed=tags&sort=id,desc", en1.getId() + "," + en3.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$").value(hasSize(2)))
            .andExpect(header().longValue("X-Total-Count", 2))
            .andExpect(jsonPath("$.[*].id").value(hasItems(en1.getId().intValue(), en3.getId().intValue())))
            .andExpect(jsonPath("$.[*].key").value(everyItem(is(DEFAULT_KEY))))
            .andExpect(jsonPath("$.[*].typeKey").value(everyItem(is(DEFAULT_TYPE_KEY))))
            .andExpect(jsonPath("$.[*].stateKey").value(everyItem(is(DEFAULT_STATE_KEY))))
            .andExpect(jsonPath("$.[*].name").value(everyItem(is(DEFAULT_NAME))))
            .andExpect(jsonPath("$.[*].startDate").value(everyItem(is(DEFAULT_START_DATE.toString()))))
            .andExpect(jsonPath("$.[*].updateDate").value(everyItem(is(DEFAULT_UPDATE_DATE.toString()))))
            .andExpect(jsonPath("$.[*].endDate").value(everyItem(is(DEFAULT_END_DATE.toString()))))
            .andExpect(jsonPath("$.[*].avatarUrl").value(everyItem(is(containsString("aaaaa.jpg")))))
            .andExpect(jsonPath("$.[*].description").value(everyItem(is(DEFAULT_DESCRIPTION))))
            .andExpect(jsonPath("$.[*].data").value(everyItem(is(DEFAULT_DATA))))
            .andExpect(jsonPath("$.[*].removed").value(everyItem(is(DEFAULT_REMOVED.booleanValue()))))

            // check that tags are not returned foe XmEntities collection
            .andExpect(jsonPath("$.[*].tags.id").value(everyItem(nullValue())));
    }


    @Test
    @Transactional
    public void getXmEntity() throws Exception {
        // Initialize the database
        xmEntity = xmEntityRepository.saveAndFlush(xmEntity);

        // Get the xmEntity
        restXmEntityMockMvc.perform(get("/api/xm-entities/{id}", xmEntity.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.id").value(xmEntity.getId().intValue()))
            .andExpect(jsonPath("$.key").value(DEFAULT_KEY.toString()))
            .andExpect(jsonPath("$.typeKey").value(DEFAULT_TYPE_KEY.toString()))
            .andExpect(jsonPath("$.stateKey").value(DEFAULT_STATE_KEY.toString()))
            .andExpect(jsonPath("$.name").value(DEFAULT_NAME.toString()))
            .andExpect(jsonPath("$.startDate").value(DEFAULT_START_DATE.toString()))
            .andExpect(jsonPath("$.updateDate").value(DEFAULT_UPDATE_DATE.toString()))
            .andExpect(jsonPath("$.endDate").value(DEFAULT_END_DATE.toString()))
            .andExpect(jsonPath("$.avatarUrl").value(containsString("aaaaa.jpg")))
            .andExpect(jsonPath("$.description").value(DEFAULT_DESCRIPTION.toString()))
            .andExpect(jsonPath("$.data.AAAAAAAAAA").value("BBBBBBBBBB"))
            .andExpect(jsonPath("$.removed").value(DEFAULT_REMOVED.booleanValue()));
    }

    @Test
    @Transactional
    public void getNonExistingXmEntity() throws Exception {
        // Get the xmEntity
        restXmEntityMockMvc.perform(get("/api/xm-entities/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("error.notfound"))
            .andExpect(jsonPath("$.error_description").value(notNullValue()))
        ;
    }

    @Test
    @Transactional
    public void updateXmEntity() throws Exception {
        XmEntity dbXmEntity = transactionService.inNestedTransaction(() -> {

            // Initialize the database
            xmEntity = xmEntityServiceImpl.save(xmEntity);

            int databaseSizeBeforeUpdate = xmEntityRepository.findAll().size();

            // Update the xmEntity
            XmEntity updatedXmEntity = xmEntityRepository.findOne(xmEntity.getId());

            em.detach(updatedXmEntity);

            updatedXmEntity
                .key(UPDATED_KEY)
                .typeKey(UPDATED_TYPE_KEY)
                .stateKey(UPDATED_STATE_KEY)
                .name(UPDATED_NAME)
                .startDate(UPDATED_START_DATE)
                .updateDate(UPDATED_UPDATE_DATE)
                .endDate(UPDATED_END_DATE)
                .avatarUrl(UPDATED_AVATAR_URL)
                .description(UPDATED_DESCRIPTION)
                .data(UPDATED_DATA)
                .removed(UPDATED_REMOVED);

            restXmEntityMockMvc.perform(put("/api/xm-entities")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(updatedXmEntity)))
                .andExpect(status().isOk());

            // Validate the XmEntity in the database
            List<XmEntity> xmEntityList = xmEntityRepository.findAll();
            assertThat(xmEntityList).hasSize(databaseSizeBeforeUpdate);
            XmEntity testXmEntity = xmEntityList.get(xmEntityList.size() - 1);
            assertThat(testXmEntity.getKey()).isEqualTo(UPDATED_KEY);
            assertThat(testXmEntity.getTypeKey()).isEqualTo(UPDATED_TYPE_KEY);
            assertThat(testXmEntity.getStateKey()).isEqualTo(UPDATED_STATE_KEY);
            assertThat(testXmEntity.getName()).isEqualTo(UPDATED_NAME);
            assertThat(testXmEntity.getStartDate()).isEqualTo(MOCKED_START_DATE);
            assertThat(testXmEntity.getUpdateDate()).isEqualTo(MOCKED_UPDATE_DATE);
            assertThat(testXmEntity.getEndDate()).isEqualTo(UPDATED_END_DATE);
            assertThat(testXmEntity.getAvatarUrl()).contains("bbbbb.jpg");
            assertThat(testXmEntity.getDescription()).isEqualTo(UPDATED_DESCRIPTION);
            assertThat(testXmEntity.getData()).isEqualTo(UPDATED_DATA);
            assertThat(testXmEntity.isRemoved()).isEqualTo(UPDATED_REMOVED);
            return testXmEntity;
        }, this::setup);

        // Validate the XmEntity in Elasticsearch
        XmEntity xmEntityEs = xmEntitySearchRepository.findOne(dbXmEntity.getId());
        assertThat(xmEntityEs).isEqualToIgnoringGivenFields(dbXmEntity,
                                                            "avatarUrlRelative", "avatarUrlFull",
                                                            "version",
                                                            "attachments",
                                                            "calendars",
                                                            "locations",
                                                            "ratings",
                                                            "votes",
                                                            "events",
                                                            "uniqueFields",
                                                            "sources",
                                                            "tags",
                                                            "comments",
                                                            "targets",
                                                            "functionContexts");
    }

    @Test
    @Transactional
    public void updateNonExistingXmEntity() throws Exception {
        // Create the XmEntity

        // If the entity doesn't have an ID, it will be created instead of just being updated
        restXmEntityMockMvc.perform(put("/api/xm-entities")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(xmEntity)))
            .andExpect(status().isCreated());
    }

    @Test
    @Transactional
    public void deleteXmEntity() throws Exception {
        int databaseSizeBeforeDelete = transactionService.inNestedTransaction(() -> {
            // Initialize the database
            xmEntity = xmEntityServiceImpl.save(xmEntity);

            int databaseSizeBeforeDeleteDb = xmEntityRepository.findAll().size();

            // Get the xmEntity
            restXmEntityMockMvc.perform(delete("/api/xm-entities/{id}", xmEntity.getId())
                .accept(TestUtil.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk());
            return databaseSizeBeforeDeleteDb;
        }, this::setup);
        // Validate Elasticsearch is empty
        boolean xmEntityExistsInEs = xmEntitySearchRepository.exists(xmEntity.getId());
        assertThat(xmEntityExistsInEs).isFalse();

        // Validate the database is empty
        List<XmEntity> xmEntityList = xmEntityRepository.findAll();
        assertThat(xmEntityList).hasSize(databaseSizeBeforeDelete - 1);
    }

    @Test
    @Transactional
    public void searchXmEntity() throws Exception {
        xmEntity = transactionService.inNestedTransaction(() -> {
                // Initialize the database
                return xmEntityServiceImpl.save(xmEntity);
        }, this::setup);

        // Search the xmEntity
        restXmEntityMockMvc.perform(get("/api/_search/xm-entities?query=id:" + xmEntity.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(xmEntity.getId().intValue())))
            .andExpect(jsonPath("$.[*].key").value(hasItem(DEFAULT_KEY.toString())))
            .andExpect(jsonPath("$.[*].typeKey").value(hasItem(DEFAULT_TYPE_KEY.toString())))
            .andExpect(jsonPath("$.[*].stateKey").value(hasItem(DEFAULT_STATE_KEY.toString())))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME.toString())))
            .andExpect(jsonPath("$.[*].startDate").value(hasItem(MOCKED_START_DATE.toString())))
            .andExpect(jsonPath("$.[*].updateDate").value(hasItem(MOCKED_UPDATE_DATE.toString())))
            .andExpect(jsonPath("$.[*].endDate").value(hasItem(DEFAULT_END_DATE.toString())))
            .andExpect(jsonPath("$.[*].avatarUrl").value(hasItem(containsString("aaaaa.jpg"))))
            .andExpect(jsonPath("$.[*].description").value(hasItem(DEFAULT_DESCRIPTION.toString())))
            .andExpect(jsonPath("$.[*].data").value(hasItem(DEFAULT_DATA)))
            .andExpect(jsonPath("$.[*].removed").value(hasItem(DEFAULT_REMOVED.booleanValue())));
    }

    @Test
    @Transactional
    public void searchXmEntityWithTemplate() throws Exception {
        xmEntity = transactionService.inNestedTransaction(() -> {
            // Initialize the database
            return xmEntityServiceImpl.save(xmEntity);
        }, this::setup);

        // Search the xmEntity
        restXmEntityMockMvc.perform(get("/api/_search-with-template/xm-entities?template=BY_TYPEKEY_AND_ID&templateParams[typeKey]=" + xmEntity.getTypeKey() + "&templateParams[id]=" + xmEntity.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(xmEntity.getId().intValue())))
            .andExpect(jsonPath("$.[*].key").value(hasItem(DEFAULT_KEY.toString())))
            .andExpect(jsonPath("$.[*].typeKey").value(hasItem(DEFAULT_TYPE_KEY.toString())))
            .andExpect(jsonPath("$.[*].stateKey").value(hasItem(DEFAULT_STATE_KEY.toString())))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME.toString())))
            .andExpect(jsonPath("$.[*].startDate").value(hasItem(MOCKED_START_DATE.toString())))
            .andExpect(jsonPath("$.[*].updateDate").value(hasItem(MOCKED_UPDATE_DATE.toString())))
            .andExpect(jsonPath("$.[*].endDate").value(hasItem(DEFAULT_END_DATE.toString())))
            .andExpect(jsonPath("$.[*].avatarUrl").value(hasItem(containsString("aaaaa.jpg"))))
            .andExpect(jsonPath("$.[*].description").value(hasItem(DEFAULT_DESCRIPTION.toString())))
            .andExpect(jsonPath("$.[*].data").value(hasItem(DEFAULT_DATA)))
            .andExpect(jsonPath("$.[*].removed").value(hasItem(DEFAULT_REMOVED.booleanValue())));
    }

    @Test
    @Transactional
    public void searchXmEntityWithoutTemplate() throws Exception {
        // Initialize the database
        xmEntityServiceImpl.save(xmEntity);
        // Search the xmEntity
        restXmEntityMockMvc.perform(get("/api/_search-with-template/xm-entities"))
            .andExpect(status().is4xxClientError());
    }

    @Test
    @Transactional
    public void searchXmEntityWithEmptyTemplate() throws Exception {
        // Initialize the database
        xmEntityServiceImpl.save(xmEntity);
        // Search the xmEntity
        restXmEntityMockMvc.perform(get("/api/_search-with-template/xm-entities?template="))
            .andExpect(status().is5xxServerError());
    }

    @Test
    @Transactional
    public void changeStateError() throws Exception {
        XmEntitySpecService xmEntitySpecService = Mockito.mock(XmEntitySpecService.class);
        StateSpec nextSpec = new StateSpec();
        nextSpec.setKey("NEXT_STATE");
        when(xmEntitySpecService.nextStates(eq(DEFAULT_TYPE_KEY), eq(DEFAULT_STATE_KEY)))
            .thenReturn(Collections.singletonList(nextSpec));

        XmEntity tenant = createEntity();
        xmEntityServiceImpl.save(tenant);

        restXmEntityMockMvc.perform(
            put("/api/xm-entities/{idOrKey}/states/{stateKey}", tenant.getId(),
                "INVALID_NEXT_STATE").contentType(
                TestUtil.APPLICATION_JSON_UTF8)).andExpect(
            status().is4xxClientError());
    }

    @Test
    @Transactional
    public void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(XmEntity.class);
        XmEntity xmEntity1 = new XmEntity();
        xmEntity1.setId(1L);
        XmEntity xmEntity2 = new XmEntity();
        xmEntity2.setId(xmEntity1.getId());
        assertThat(xmEntity1).isEqualTo(xmEntity2);
        xmEntity2.setId(2L);
        assertThat(xmEntity1).isNotEqualTo(xmEntity2);
        xmEntity1.setId(null);
        assertThat(xmEntity1).isNotEqualTo(xmEntity2);
    }
}
