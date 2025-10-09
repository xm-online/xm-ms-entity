package com.icthh.xm.ms.entity.web.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.icthh.xm.commons.i18n.error.web.ExceptionTranslator;
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
import com.icthh.xm.ms.entity.config.Constants;
import com.icthh.xm.ms.entity.config.InternalTransactionService;
import static com.icthh.xm.ms.entity.config.TenantConfigMockConfiguration.getXmEntityTemplatesSpec;
import com.icthh.xm.ms.entity.config.XmEntityTenantConfigService;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.spec.StateSpec;
import com.icthh.xm.ms.entity.lep.keyresolver.TypeKeyWithExtends;
import com.icthh.xm.ms.entity.repository.EventRepository;
import com.icthh.xm.ms.entity.repository.SpringXmEntityRepository;
import com.icthh.xm.ms.entity.repository.UniqueFieldRepository;
import com.icthh.xm.ms.entity.repository.XmEntityPermittedRepository;
import com.icthh.xm.ms.entity.repository.XmEntityRepositoryInternal;
import com.icthh.xm.ms.entity.repository.kafka.ProfileEventProducer;
import com.icthh.xm.ms.entity.repository.search.XmEntityPermittedSearchRepository;
import com.icthh.xm.ms.entity.repository.search.XmEntitySearchRepository;
import com.icthh.xm.ms.entity.service.AttachmentService;
import com.icthh.xm.ms.entity.service.LifecycleLepStrategyFactory;
import com.icthh.xm.ms.entity.service.LinkService;
import com.icthh.xm.ms.entity.service.ProfileService;
import com.icthh.xm.ms.entity.service.SimpleTemplateProcessor;
import com.icthh.xm.ms.entity.service.StorageService;
import com.icthh.xm.ms.entity.service.TenantService;
import com.icthh.xm.ms.entity.service.XmEntityProjectionService;
import com.icthh.xm.ms.entity.service.XmEntitySpecService;
import com.icthh.xm.ms.entity.service.XmEntityTemplatesSpecService;
import com.icthh.xm.ms.entity.service.impl.StartUpdateDateGenerationStrategy;
import com.icthh.xm.ms.entity.service.impl.XmEntityFunctionServiceFacade;
import com.icthh.xm.ms.entity.service.impl.XmEntityServiceImpl;
import com.icthh.xm.ms.entity.service.impl.XmeStorageServiceFacadeImpl;
import com.icthh.xm.ms.entity.service.json.JsonValidationService;
import com.icthh.xm.ms.entity.service.storage.AvatarStorageService;
import jakarta.persistence.EntityManager;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Validator;

import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static software.amazon.awssdk.utils.StringUtils.repeat;

/**
 * Test class for the XmEntityResource REST controller.
 *
 * @see XmEntityResource
 */
@Slf4j
@WithMockUser(authorities = {"SUPER-ADMIN"})
public class XmEntityResourceIntTest extends AbstractJupiterSpringBootTest {

    private static final String DEFAULT_KEY = "AAAAAAAAAA";

    public static final String DEFAULT_TYPE_KEY = "ACCOUNT.ADMIN";

    private static final String DEFAULT_STATE_KEY = "STATE2";

    private static final String DEFAULT_NAME = "AAAAAAAAAA";

    private static final Instant DEFAULT_START_DATE = Instant.ofEpochMilli(0L);

    private static final Instant DEFAULT_UPDATE_DATE = Instant.ofEpochMilli(0L);

    private static final Instant MOCKED_START_DATE = Instant.ofEpochMilli(42L);
    private static final Instant MOCKED_UPDATE_DATE = Instant.ofEpochMilli(84L);

    private static final Instant DEFAULT_END_DATE = Instant.ofEpochMilli(0L);

    private static final String DEFAULT_AVATAR_URL = "http://hello.rgw.icthh.test/aaaaa.jpg";

    private static final String DEFAULT_DESCRIPTION = "AAAAAAAAAA";

    private static final Map<String, Object> DEFAULT_DATA = ImmutableMap.<String, Object>builder()
        .put("AAAAAAAAAA", "BBBBBBBBBB").build();

    private static final Boolean DEFAULT_REMOVED = false;

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
    private XmEntityFunctionServiceFacade functionService;

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
    AvatarStorageService avatarStorageService;

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
    private MockMvc restXmEntitySearchMockMvc;

    private XmEntity xmEntity;

    @Autowired
    private Validator validator;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private InternalTransactionService transactionService;

    @Autowired
    private XmEntityProjectionService xmEntityProjectionService;

    @BeforeTransaction
    public void beforeTransaction() {
        TenantContextUtils.setTenant(tenantContextHolder, "RESINTTEST");
    }

    @SneakyThrows
    @BeforeEach
    public void setup() {

        TenantContextUtils.setTenant(tenantContextHolder, "RESINTTEST");

        MockitoAnnotations.initMocks(this);
        when(authContextHolder.getContext()).thenReturn(context);
        when(context.getUserKey()).thenReturn(Optional.of("userKey"));

        when(startUpdateDateGenerationStrategy.generateStartDate()).thenReturn(MOCKED_START_DATE);
        when(startUpdateDateGenerationStrategy.generateUpdateDate()).thenReturn(MOCKED_UPDATE_DATE);

        String tenantName = getRequiredTenantKeyValue(tenantContextHolder);
        String config = getXmEntityTemplatesSpec(tenantName);
        String key = applicationProperties.getSpecificationTemplatesPathPattern().replace("{tenantName}", tenantName);
        xmEntityTemplatesSpecService.onRefresh(key, config);
        xmEntityTemplatesSpecService.refreshFinished(List.of(key));

        XmeStorageServiceFacadeImpl storageServiceFacade = new XmeStorageServiceFacadeImpl(storageService, avatarStorageService, attachmentService);

        XmEntityServiceImpl xmEntityServiceImpl = new XmEntityServiceImpl(xmEntitySpecService,
                                                      xmEntityTemplatesSpecService,
                                                      xmEntityRepository,
                                                      lifeCycleService,
                                                      xmEntityPermittedRepository,
                                                      profileService,
                                                      linkService,
                                                      storageServiceFacade,
                                                      xmEntityPermittedSearchRepository,
                                                      startUpdateDateGenerationStrategy,
                                                      authContextHolder,
                                                      objectMapper,
                                                      mock(UniqueFieldRepository.class),
                                                      springXmEntityRepository,
                                                      new TypeKeyWithExtends(tenantConfigService),
                                                      new SimpleTemplateProcessor(objectMapper),
                                                      eventRepository,
                                                      mock(JsonValidationService.class),
                                                      xmEntityProjectionService);

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

        this.restXmEntitySearchMockMvc = MockMvcBuilders.standaloneSetup(new XmEntitySearchResource(xmEntityServiceImpl))
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setValidator(validator)
            .setMessageConverters(jacksonMessageConverter).build();

        xmEntity = createEntity();
    }

    @AfterEach
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

    @BeforeEach
    public void initTest() {
        //    xmEntitySearchRepository.deleteAll();
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
    @Disabled("see XmEntityResourceExtendedIntTest.checkStartDateIsNotRequired instead")
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
    @Disabled("see XmEntityResourceExtendedIntTest.checkUpdateDateIsNotRequired instead")
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
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
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
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
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
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
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
    public void searchXmEntityWithoutTemplate() throws Exception {
        // Initialize the database
        xmEntityServiceImpl.save(xmEntity);
        // Search the xmEntity
        restXmEntitySearchMockMvc.perform(get("/api/_search-with-template/xm-entities"))
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
    public void saveWithLongName() {
        XmEntity entity = createEntity();
        String name = repeat("some", 250);
        entity.setName(name);
        xmEntityServiceImpl.save(xmEntity);
        XmEntity saved = xmEntityServiceImpl.save(entity);
        em.flush();
        assertThat(saved.getName().length()).isEqualTo(1000);
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
