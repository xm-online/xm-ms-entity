package com.icthh.xm.ms.entity.elasticsearch.web.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.icthh.xm.commons.i18n.error.web.ExceptionTranslator;
import com.icthh.xm.commons.security.XmAuthenticationContext;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.service.FunctionServiceFacade;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.ms.entity.config.ApplicationProperties;
import com.icthh.xm.ms.entity.config.InternalTransactionService;
import com.icthh.xm.ms.entity.config.XmEntityTenantConfigService;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.elasticsearch.AbstractElasticSpringBootTest;
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
import com.icthh.xm.ms.entity.service.json.JsonValidationService;
import com.icthh.xm.ms.entity.web.rest.TestUtil;
import com.icthh.xm.ms.entity.web.rest.XmEntityResource;
import com.icthh.xm.ms.entity.web.rest.XmEntitySearchResource;
import jakarta.persistence.EntityManager;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Validator;

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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test class for the XmEntityResource REST controller.
 *
 * @see XmEntityResource
 */
@Slf4j
@WithMockUser(authorities = {"SUPER-ADMIN"})
public class XmEntityResourceElasticsearchTest extends AbstractElasticSpringBootTest {

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

        //initialize index before test - put valid mapping
        if (!elasticInited) {
            initElasticsearch(tenantContextHolder);
            elasticInited = true;
        }
        cleanElasticsearch(tenantContextHolder);

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
        xmEntitySearchRepository.refresh();
        // Validate the XmEntity in Elasticsearch
        XmEntity xmEntityEs = xmEntitySearchRepository.findById(dbXmEntity.getId())
            .orElseThrow(NullPointerException::new);
        assertThat(xmEntityEs).isEqualToIgnoringGivenFields(dbXmEntity, "avatarUrlRelative", "avatarUrlFull");
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
    public void updateXmEntity() throws Exception {
        XmEntity dbXmEntity = transactionService.inNestedTransaction(() -> {

            // Initialize the database
            xmEntity = xmEntityServiceImpl.save(xmEntity);

            int databaseSizeBeforeUpdate = xmEntityRepository.findAll().size();

            // Update the xmEntity
            XmEntity updatedXmEntity = xmEntityRepository.findById(xmEntity.getId())
                .orElseThrow(NullPointerException::new);

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
        xmEntitySearchRepository.refresh();
        // Validate the XmEntity in Elasticsearch
        XmEntity xmEntityEs = xmEntitySearchRepository.findById(dbXmEntity.getId())
            .orElseThrow(NullPointerException::new);
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
        boolean xmEntityExistsInEs = xmEntitySearchRepository.existsById(xmEntity.getId());
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
        xmEntitySearchRepository.refresh();

        // Search the xmEntity
        restXmEntitySearchMockMvc.perform(get("/api/_search/xm-entities?query=id:" + xmEntity.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
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
    public void searchXmEntityWithExcludeFields() throws Exception {
        xmEntity = transactionService.inNestedTransaction(() -> {
            // Initialize the database
            return xmEntityServiceImpl.save(xmEntity);
        }, this::setup);
        xmEntitySearchRepository.refresh();

        // Search the xmEntity
        restXmEntitySearchMockMvc.perform(get("/api/_search/v2/xm-entities?query=id:" + xmEntity.getId()
            +"&excludes=id&excludes=key&excludes=typeKey"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.[*].id").value(hasItem(nullValue())))
            .andExpect(jsonPath("$.[*].key").value(hasItem(nullValue())))
            .andExpect(jsonPath("$.[*].typeKey").value(hasItem(nullValue())))
            .andExpect(jsonPath("$.[*].stateKey").value(hasItem(DEFAULT_STATE_KEY)))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME)))
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
    public void searchXmEntityWithIncludeFields() throws Exception {
        xmEntity = transactionService.inNestedTransaction(() -> {
            // Initialize the database
            return xmEntityServiceImpl.save(xmEntity);
        }, this::setup);
        xmEntitySearchRepository.refresh();

        // Search the xmEntity
        restXmEntitySearchMockMvc.perform(get("/api/_search/v2/xm-entities?query=id:" + xmEntity.getId()
            +"&includes=id&includes=key&includes=typeKey"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.[*].id").value(hasItem(xmEntity.getId().intValue())))
            .andExpect(jsonPath("$.[*].key").value(hasItem(DEFAULT_KEY)))
            .andExpect(jsonPath("$.[*].typeKey").value(hasItem(DEFAULT_TYPE_KEY)))
            .andExpect(jsonPath("$.[*].stateKey").value(hasItem(nullValue())))
            .andExpect(jsonPath("$.[*].name").value(hasItem(nullValue())))
            .andExpect(jsonPath("$.[*].startDate").value(hasItem(nullValue())))
            .andExpect(jsonPath("$.[*].updateDate").value(hasItem(nullValue())))
            .andExpect(jsonPath("$.[*].endDate").value(hasItem(nullValue())))
            .andExpect(jsonPath("$.[*].avatarUrl").value(hasItem(nullValue())))
            .andExpect(jsonPath("$.[*].description").value(hasItem(nullValue())))
            .andExpect(jsonPath("$.[*].removed").value(hasItem(nullValue())));
    }



    @Test
    @Transactional
    public void searchXmEntityWithTemplate() throws Exception {
        xmEntity = transactionService.inNestedTransaction(() -> {
            // Initialize the database
            return xmEntityServiceImpl.save(xmEntity);
        }, this::setup);
        xmEntitySearchRepository.refresh();
        // Search the xmEntity
        restXmEntitySearchMockMvc.perform(get("/api/_search-with-template/xm-entities?template=BY_TYPEKEY_AND_ID&templateParams[typeKey]=" + xmEntity.getTypeKey() + "&templateParams[id]=" + xmEntity.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
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
}
