package com.icthh.xm.ms.entity.elasticsearch.web.rest;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.collect.ImmutableMap;
import com.icthh.xm.commons.i18n.error.web.ExceptionTranslator;
import com.icthh.xm.commons.security.XmAuthenticationContext;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.ms.entity.config.ApplicationProperties;
import com.icthh.xm.ms.entity.config.InternalTransactionService;
import com.icthh.xm.ms.entity.config.WebMvcConfiguration;
import com.icthh.xm.ms.entity.config.XmEntityTenantConfigService;
import com.icthh.xm.ms.entity.domain.Attachment;
import com.icthh.xm.ms.entity.domain.Calendar;
import com.icthh.xm.ms.entity.domain.Content;
import com.icthh.xm.ms.entity.domain.Event;
import com.icthh.xm.ms.entity.domain.Link;
import com.icthh.xm.ms.entity.domain.Location;
import com.icthh.xm.ms.entity.domain.Tag;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.ext.IdOrKey;
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
import com.icthh.xm.ms.entity.service.CalendarService;
import com.icthh.xm.ms.entity.service.EventService;
import com.icthh.xm.ms.entity.service.FunctionService;
import com.icthh.xm.ms.entity.service.LifecycleLepStrategyFactory;
import com.icthh.xm.ms.entity.service.LinkService;
import com.icthh.xm.ms.entity.service.ProfileService;
import com.icthh.xm.ms.entity.service.SeparateTransactionExecutor;
import com.icthh.xm.ms.entity.service.SimpleTemplateProcessor;
import com.icthh.xm.ms.entity.service.StorageService;
import com.icthh.xm.ms.entity.service.TenantService;
import com.icthh.xm.ms.entity.service.XmEntityProjectionService;
import com.icthh.xm.ms.entity.service.XmEntityService;
import com.icthh.xm.ms.entity.service.XmEntitySpecService;
import com.icthh.xm.ms.entity.service.XmEntityTemplatesSpecService;
import com.icthh.xm.ms.entity.service.impl.StartUpdateDateGenerationStrategy;
import com.icthh.xm.ms.entity.service.impl.XmEntityServiceImpl;
import com.icthh.xm.ms.entity.service.json.JsonValidationService;
import com.icthh.xm.ms.entity.web.rest.CalendarResource;
import com.icthh.xm.ms.entity.web.rest.EventResource;
import com.icthh.xm.ms.entity.web.rest.TestUtil;
import com.icthh.xm.ms.entity.web.rest.XmEntityResource;
import com.icthh.xm.ms.entity.web.rest.XmEntitySearchResource;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Validator;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_AUTH_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import static com.icthh.xm.commons.tenant.TenantContextUtils.getRequiredTenantKeyValue;
import static com.icthh.xm.ms.entity.config.TenantConfigMockConfiguration.getXmEntityTemplatesSpec;
import static com.icthh.xm.ms.entity.web.rest.TestUtil.sameInstant;
import static java.lang.Long.valueOf;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Extension Test class for the XmEntityResource REST controller. Contains additional test apart from Jhipster generated
 * logic
 *
 * @see XmEntityResource
 */
@Slf4j
@Transactional
public class XmEntityResourceExtendedElasticsearchTest extends AbstractElasticSpringBootTest {

    private static final String DEFAULT_KEY = "AAAAAAAAAA";

    private static final String DEFAULT_TYPE_KEY = "ACCOUNT.ADMIN";
    private static final String UPDATED_TYPE_KEY = "ACCOUNT.USER";

    private static final String DEFAULT_STATE_KEY = "STATE2";

    private static final String DEFAULT_NAME = "AAAAAAAAAA";

    private static final Instant DEFAULT_START_DATE = Instant.ofEpochMilli(1000L);

    private static final Instant DEFAULT_UPDATE_DATE = Instant.ofEpochMilli(2000L);

    private static final Instant DEFAULT_END_DATE = Instant.ofEpochMilli(3000L);

    private static final String DEFAULT_AVATAR_URL_PREFIX = "http://xm-avatar.rgw.icthh.test:7480/";

    private static final String DEFAULT_AVATAR_URL = DEFAULT_AVATAR_URL_PREFIX + "aaaaa.jpg";

    private static final String DEFAULT_DESCRIPTION = "AAAAAAAAAA";

    private static final Boolean DEFAULT_REMOVED = false;

    private static final Map<String, Object> DEFAULT_DATA = ImmutableMap.<String, Object>builder()
        .put("AAAAAAAAAA", "BBBBBBBBBB").build();

    private static final String LARGE_VALUE = StringUtils.repeat("LongStr ", 1024);
    private static final Map<String, Object> LARGE_DATA = ImmutableMap.<String, Object>builder()
        .put("AAAAAAAAAA", LARGE_VALUE).build();

    private static final String DEFAULT_TAG_NAME = "Test";
    private static final String DEFAULT_TAG_KEY = "FAVORIT";
    private static final String SEARCH_TEST_KEY = "ACCOUNTING";
    private static final String UNIQ_DESCRIPTION = "fd47b4dd-9ff9-4248-8d0b-1174350ba06c";
    private static final String NOT_PRESENT_UNIQ_DESCRIPTION = "b3163aab-d6e7-45c3-a0c3-96ac67ad7570";

    private static final String DEFAULT_ATTACHMENT_KEY = "AAAAAAAAAA";
    private static final String DEFAULT_ATTACHMENT_NAME = "Attachment1";
    private static final Instant DEFAULT_ATTACHMENT_START_DATE = Instant.now();
    private static final Instant DEFAULT_ATTACHMENT_END_DATE = Instant.now();

    private static final Instant MOCKED_START_DATE = Instant.ofEpochMilli(42L);
    private static final Instant MOCKED_UPDATE_DATE = Instant.ofEpochMilli(84L);

    private static final String DEFAULT_ATTACHMENT_CONTENT_TYPE = "my-content";
    private static final String DEFAULT_ATTACHMENT_CONTENT_VALUE = "THIS IS CONTENT";
    private static final String DEFAULT_ATTACHMENT_URL = "content url";
    private static final String DEFAULT_ATTACHMENT_DESCRIPTION = "Attachment description";
    private static final Integer DEFAULT_ATTACHMENT_CONTENT_SIZE = DEFAULT_ATTACHMENT_CONTENT_VALUE.length();

    private static final String DEFAULT_LOCATION_KEY = "AAAAAAAAAA";
    private static final String DEFAULT_LOCATION_NAME = "My location";
    private static final String DEFAULT_LOCATION_COUNTRY_KEY = "UA";

    private static final String DEFAULT_LN_TARGET_KEY = "LINK.TARGET";
    private static final String DEFAULT_LN_TARGET_NAME = "My target link";
    private static final Instant DEFAULT_LN_TARGET_START_DATE = Instant.now();

    private static boolean elasticInited = false;

    @Autowired
    private SeparateTransactionExecutor separateTransactionExecutor;

    @Autowired
    private ApplicationProperties applicationProperties;

    @Autowired
    private CalendarResource calendarResource;

    @Autowired
    private EventResource eventResource;

    @Autowired
    private XmEntityResource xmEntityResource;

    @Autowired
    private XmEntityRepositoryInternal xmEntityRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private SpringXmEntityRepository springXmEntityRepository;

    @Autowired
    private ProfileEventProducer profileEventProducer;

    private XmEntityService xmEntityService;

    @Autowired
    private ProfileService profileService;

    @Autowired
    private CalendarService calendarService;

    @Autowired
    private EventService eventService;

    @Autowired
    private XmEntitySearchRepository xmEntitySearchRepository;

    @Autowired
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Autowired
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Autowired
    private FunctionService functionService;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @Autowired
    private EntityManager em;

    @Autowired
    private LepManager lepManager;

    @Autowired
    private Validator validator;

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
    ObjectMapper objectMapper;

    @Autowired
    XmEntityTenantConfigService tenantConfigService;

    @Autowired
    XmEntityPermittedSearchRepository xmEntityPermittedSearchRepository;

    @Mock
    private XmAuthenticationContextHolder authContextHolder;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private InternalTransactionService transactionService;

    @Autowired
    private XmEntityProjectionService xmEntityProjectionService;

    @Mock
    private XmAuthenticationContext context;

    @Spy
    private StartUpdateDateGenerationStrategy startUpdateDateGenerationStrategy;

    private MockMvc restXmEntityMockMvc;

    private MockMvc restXmEntitySearchMockMvc;

    private XmEntity xmEntityIncoming;

    @BeforeTransaction
    public void beforeTransaction() {
        TenantContextUtils.setTenant(tenantContextHolder, "RESINTTEST");
    }

    @BeforeEach
    public void setup() {
        log.info("Init setup");

        TenantContextUtils.setTenant(tenantContextHolder, "RESINTTEST");
        //xmEntitySearchRepository.deleteAll();

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

        XmEntityServiceImpl xmEntityService = new XmEntityServiceImpl(xmEntitySpecService,
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

        xmEntityService.setSelf(xmEntityService);
        this.xmEntityService = xmEntityService;

        lepManager.beginThreadContext(ctx -> {
            ctx.setValue(THREAD_CONTEXT_KEY_TENANT_CONTEXT, tenantContextHolder.getContext());
            ctx.setValue(THREAD_CONTEXT_KEY_AUTH_CONTEXT, authContextHolder.getContext());
        });

        EventResource eventResourceMock = new EventResource(eventService, eventResource);

        XmEntityResource xmEntityResourceMock = new XmEntityResource(xmEntityService,
                                                                     profileService,
                                                                     profileEventProducer,
                                                                     functionService,
                                                                     tenantService,
                                                                     xmEntityResource
        );
        this.restXmEntityMockMvc = MockMvcBuilders.standaloneSetup(
            xmEntityResourceMock, new CalendarResource(calendarService, calendarResource), eventResourceMock)
                                                  .setCustomArgumentResolvers(pageableArgumentResolver)
                                                  .setControllerAdvice(exceptionTranslator)
                                                  .setValidator(validator)
                                                  .setMessageConverters(jacksonMessageConverter)
                                                  .addMappedInterceptors(WebMvcConfiguration.getJsonFilterAllowedURIs())
                                                  .build();

        this.restXmEntitySearchMockMvc = MockMvcBuilders.standaloneSetup(new XmEntitySearchResource(xmEntityService))
                                                  .setCustomArgumentResolvers(pageableArgumentResolver)
                                                  .setControllerAdvice(exceptionTranslator)
                                                  .setValidator(validator)
                                                  .setMessageConverters(jacksonMessageConverter)
                                                  .addMappedInterceptors(WebMvcConfiguration.getJsonFilterAllowedURIs())
                                                  .build();

        xmEntityIncoming = createEntityComplexIncoming();

        objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    @AfterEach
    public void tearDown() {
        lepManager.endThreadContext();
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
    }

    /**
     * Creates incoming Entity as from HTTP request. Potentially can be moved to DTO
     *
     * @return XmEntity for incoming request
     */
    public static XmEntity createEntityComplexIncoming() {
        XmEntity entity = createEntity();

        entity.getTags().add(new Tag()
                                 .typeKey(DEFAULT_TAG_KEY)
                                 .startDate(DEFAULT_START_DATE)
                                 .name(DEFAULT_TAG_NAME));
        entity.getAttachments().add(new Attachment()
                                        .typeKey(DEFAULT_ATTACHMENT_KEY)
                                        .name(DEFAULT_ATTACHMENT_NAME)
                                        .startDate(DEFAULT_ATTACHMENT_START_DATE)
                                        .endDate(DEFAULT_ATTACHMENT_END_DATE)
                                        .valueContentType(DEFAULT_ATTACHMENT_CONTENT_TYPE)
                                        .content(new Content().value(DEFAULT_ATTACHMENT_CONTENT_VALUE.getBytes()))
                                        .contentUrl(DEFAULT_ATTACHMENT_URL)
                                        .description(DEFAULT_ATTACHMENT_DESCRIPTION)
        );
        entity.getLocations().add(new Location()
                                      .typeKey(DEFAULT_LOCATION_KEY)
                                      .name(DEFAULT_LOCATION_NAME)
                                      .countryKey(DEFAULT_LOCATION_COUNTRY_KEY)
        );

        return entity;
    }

    /**
     * Create an entity for this test.
     * <p>
     * This is a static method, as tests for other entities might also need it, if they test an entity which requires
     * the current entity.
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

    /**
     * Performs REST result to log.
     */
    private void printMvcResult(MvcResult result) throws UnsupportedEncodingException {
                log.info("MVC result: {}", result.getResponse().getContentAsString());
    }

    /**
     * Performs HTTP GET.
     *
     * @param url    URL template
     * @param params Temaplte params
     */
    private ResultActions performGet(String url, Object... params) throws Exception {
        return restXmEntityMockMvc.perform(get(url, params))
                                  .andDo(this::printMvcResult);
    }

    private ResultActions performSearchGet(String url, Object... params) throws Exception {
        return restXmEntitySearchMockMvc.perform(get(url, params))
            .andDo(this::printMvcResult);
    }

    /**
     * Performs HTTP PUT.
     *
     * @param content Content object
     */
    private ResultActions performPut(Object content) throws Exception {

        String entityPutUrl = "/api/xm-entities";
        String json = new String(convertObjectToJsonBytesByFields(content));
        log.info("perform PUT: {} with content: {}", entityPutUrl, json);
        return restXmEntityMockMvc.perform(put(entityPutUrl)
                                               .contentType(TestUtil.APPLICATION_JSON_UTF8)
                                               .content(json))
                                  .andDo(this::printMvcResult);
    }

    /**
     * Performs HTTP POST.
     *
     * @param url     URL template
     * @param content content object
     */
    private ResultActions performPost(String url, Object content) throws Exception {
        String json = new String(convertObjectToJsonBytesByFields(content));
        log.info("perform POST: {} with content: {}", url, json);
        return restXmEntityMockMvc.perform(post(url)
                                               .contentType(TestUtil.APPLICATION_JSON_UTF8)
                                               .content(json))
                                  .andDo(this::printMvcResult);
    }

    /**
     * Validates expected entites size in DB.
     *
     * @param expectedSize - expected size
     */
    private List<XmEntity> validateEntityInDB(final int expectedSize) {
        List<XmEntity> xmEntityList = xmEntityRepository.findAll();
        assertThat(xmEntityList).hasSize(expectedSize);
        return xmEntityList;
    }

    /**
     * Convert an object to JSON byte array.
     *
     * @param object the object to convert
     * @return the JSON byte array
     */
    public static byte[] convertObjectToJsonBytesByFields(Object object)
    throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
              .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

        JavaTimeModule module = new JavaTimeModule();
        mapper.registerModule(module);

        return mapper.writeValueAsBytes(object);
    }

    private XmEntity convertJsonToObject(String json) throws IOException {
        return objectMapper.readValue(json, XmEntity.class);
    }

    @Test
    public void createXmEntityWithTagsAttachmentsLocations() throws Exception {

        XmEntity testXmEntity = separateTransactionExecutor.doInSeparateTransaction(() -> {

            int databaseSizeBeforeCreate = xmEntityRepository.findAll().size();

            // Create the XmEntity with tag, attachment and location
            MvcResult result = performPost("/api/xm-entities", xmEntityIncoming)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.key").value(DEFAULT_KEY))
                .andReturn();

            Integer id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");

            List<XmEntity> xmEntityList = validateEntityInDB(databaseSizeBeforeCreate + 1);
            XmEntity resultXmEntity = xmEntityList.get(xmEntityList.size() - 1);

            // Get the xmEntityPersisted by ID
            performGet("/api/xm-entities/{id}", id)
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.key").value(DEFAULT_KEY))
                .andExpect(jsonPath("$.typeKey").value(DEFAULT_TYPE_KEY))
                .andExpect(jsonPath("$.stateKey").value(DEFAULT_STATE_KEY))
                .andExpect(jsonPath("$.name").value(DEFAULT_NAME))
                .andExpect(jsonPath("$.startDate").value(sameInstant(MOCKED_START_DATE)))
                .andExpect(jsonPath("$.updateDate").value(sameInstant(MOCKED_UPDATE_DATE)))
                .andExpect(jsonPath("$.endDate").value(sameInstant(DEFAULT_END_DATE)))
                .andExpect(jsonPath("$.avatarUrl").value(containsString("aaaaa.jpg")))
                .andExpect(jsonPath("$.avatarUrl").value(DEFAULT_AVATAR_URL))
                .andExpect(jsonPath("$.description").value(DEFAULT_DESCRIPTION))
                .andExpect(jsonPath("$.data.AAAAAAAAAA").value("BBBBBBBBBB"))

                .andExpect(jsonPath("$.tags[0].id").value(notNullValue()))
                .andExpect(jsonPath("$.tags[0].name").value(DEFAULT_TAG_NAME))
                .andExpect(jsonPath("$.tags[0].typeKey").value(DEFAULT_TAG_KEY))
                .andExpect(jsonPath("$.tags[0].xmEntity").value(id))

                .andExpect(jsonPath("$.attachments[0].id").value(notNullValue()))
                .andExpect(jsonPath("$.attachments[0].typeKey").value(DEFAULT_ATTACHMENT_KEY))
                .andExpect(jsonPath("$.attachments[0].name").value(DEFAULT_ATTACHMENT_NAME))
                .andExpect(jsonPath("$.attachments[0].contentUrl").value(DEFAULT_ATTACHMENT_URL))
                .andExpect(jsonPath("$.attachments[0].description").value(DEFAULT_ATTACHMENT_DESCRIPTION))
                .andExpect(jsonPath("$.attachments[0].startDate").value(DEFAULT_ATTACHMENT_START_DATE.toString()))
                .andExpect(jsonPath("$.attachments[0].endDate").value(DEFAULT_ATTACHMENT_END_DATE.toString()))
                .andExpect(jsonPath("$.attachments[0].valueContentType").value(DEFAULT_ATTACHMENT_CONTENT_TYPE))
                .andExpect(jsonPath("$.attachments[0].valueContentSize").value(DEFAULT_ATTACHMENT_CONTENT_SIZE))
                .andExpect(jsonPath("$.attachments[0].xmEntity").value(id))

                .andExpect(jsonPath("$.locations[0].id").value(notNullValue()))
                .andExpect(jsonPath("$.locations[0].typeKey").value(DEFAULT_LOCATION_KEY))
                .andExpect(jsonPath("$.locations[0].name").value(DEFAULT_LOCATION_NAME))
                .andExpect(jsonPath("$.locations[0].countryKey").value(DEFAULT_LOCATION_COUNTRY_KEY))
                .andExpect(jsonPath("$.locations[0].xmEntity").value(id));

            // TODO - check not implemented XmEntity references. Replace with correct assertions after implementation
            //.andExpect(jsonPath("$.calendars.length()").value(0))

            // FIXME - content is returned in TEST mode besides LAZY fetchtype. Need to investigate.
            // .andExpect(jsonPath("$.attachments[0].content.id").value(notNullValue()))

            return resultXmEntity;
        });

        xmEntitySearchRepository.refresh();

        // Validate the XmEntity in Elasticsearch
        XmEntity xmEntityEs = xmEntitySearchRepository.findById(valueOf(testXmEntity.getId().toString()))
                                                      .orElseThrow(NullPointerException::new);
        assertThat(xmEntityEs).isEqualToIgnoringGivenFields(testXmEntity, "avatarUrlRelative");
        separateTransactionExecutor.doInSeparateTransaction(() -> {
            xmEntityService.delete(testXmEntity.getId());
            return null;
        });
    }

    @Test
    public void createXmEntityWithLinks() throws Exception {

        XmEntity resultXmEntity = separateTransactionExecutor.doInSeparateTransaction(() -> {

            XmEntity presaved = xmEntityService.save(createEntity());

            assertNotNull(presaved.getId());

            int databaseSizeBeforeCreate = xmEntityRepository.findAll().size();

            XmEntity entity = xmEntityIncoming;
            entity.getTargets().add(new Link()
                                        .typeKey(DEFAULT_LN_TARGET_KEY)
                                        .name(DEFAULT_LN_TARGET_NAME)
                                        .startDate(DEFAULT_LN_TARGET_START_DATE)
                                        .target(presaved)
                                   );

            // Create the XmEntity with tag
            MvcResult result = performPost("/api/xm-entities", entity)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.key").value(DEFAULT_KEY))
                .andReturn();

            Integer id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");

            List<XmEntity> xmEntityList = validateEntityInDB(databaseSizeBeforeCreate + 1);
            XmEntity testXmEntity = xmEntityList.get(xmEntityList.size() - 1);

            // Get the xmEntityPersisted with tag by ID
            performGet("/api/xm-entities/{id}", id)
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.key").value(DEFAULT_KEY))
                .andExpect(jsonPath("$.typeKey").value(DEFAULT_TYPE_KEY))
                .andExpect(jsonPath("$.stateKey").value(DEFAULT_STATE_KEY))
                .andExpect(jsonPath("$.name").value(DEFAULT_NAME))
                .andExpect(jsonPath("$.startDate").value(sameInstant(MOCKED_START_DATE)))
                .andExpect(jsonPath("$.updateDate").value(sameInstant(MOCKED_UPDATE_DATE)))
                .andExpect(jsonPath("$.endDate").value(sameInstant(DEFAULT_END_DATE)))
                .andExpect(jsonPath("$.avatarUrl").value(containsString("aaaaa.jpg")))
                .andExpect(jsonPath("$.avatarUrl").value(DEFAULT_AVATAR_URL))
                .andExpect(jsonPath("$.description").value(DEFAULT_DESCRIPTION))
                .andExpect(jsonPath("$.data.AAAAAAAAAA").value("BBBBBBBBBB"))

                .andExpect(jsonPath("$.targets[0].id").value(notNullValue()))
                .andExpect(jsonPath("$.targets[0].name").value(DEFAULT_LN_TARGET_NAME))
                .andExpect(jsonPath("$.targets[0].typeKey").value(DEFAULT_LN_TARGET_KEY))
                .andExpect(jsonPath("$.targets[0].source").value(id))
                .andExpect(jsonPath("$.targets[0].target.id").value(presaved.getId()))
                .andExpect(jsonPath("$.targets[0].target.typeKey").value(presaved.getTypeKey()));
            return testXmEntity;
        });

        xmEntitySearchRepository.refresh();

        // Validate the XmEntity in Elasticsearch
        XmEntity xmEntityEs = xmEntitySearchRepository.findById(valueOf(resultXmEntity.getId().toString()))
                                                      .orElseThrow(NullPointerException::new);
        assertThat(xmEntityEs).isEqualToIgnoringGivenFields(resultXmEntity, "avatarUrlRelative");
        separateTransactionExecutor.doInSeparateTransaction(() -> {
            xmEntityService.delete(resultXmEntity.getId());
            return null;
        });
    }

    @Test
    public void createXmEntityWithSourceLinks() throws Exception {

        XmEntity testXmEntity = separateTransactionExecutor.doInSeparateTransaction(() -> {

        XmEntity presaved = xmEntityService.save(createEntity());

        int databaseSizeBeforeCreate = xmEntityRepository.findAll().size();

        XmEntity entity = xmEntityIncoming;
        entity.setSources(Collections.singleton(new Link()
                                                    .typeKey(DEFAULT_LN_TARGET_KEY)
                                                    .name(DEFAULT_LN_TARGET_NAME)
                                                    .startDate(DEFAULT_LN_TARGET_START_DATE)
                                                    .source(presaved))
        );

        // Create the XmEntity with tag
        MvcResult result = performPost("/api/xm-entities", entity)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.key").value(DEFAULT_KEY))
            .andReturn();

        Integer id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");

        em.detach(presaved);

        List<XmEntity> xmEntityList = validateEntityInDB(databaseSizeBeforeCreate + 1);
        XmEntity resultXmEntity = xmEntityList.get(xmEntityList.size() - 1);

        presaved = xmEntityService.findOne(IdOrKey.of(presaved.getId()));

        assertNotNull(presaved.getId());

        // Get the xmEntityPersisted with tag by ID
        performGet("/api/xm-entities/{id}", presaved.getId())
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(presaved.getId()))
            .andExpect(jsonPath("$.key").value(DEFAULT_KEY))
            .andExpect(jsonPath("$.typeKey").value(DEFAULT_TYPE_KEY))
            .andExpect(jsonPath("$.stateKey").value(DEFAULT_STATE_KEY))
            .andExpect(jsonPath("$.name").value(DEFAULT_NAME))
            .andExpect(jsonPath("$.startDate").value(sameInstant(MOCKED_START_DATE)))
            .andExpect(jsonPath("$.updateDate").value(sameInstant(MOCKED_UPDATE_DATE)))
            .andExpect(jsonPath("$.endDate").value(sameInstant(DEFAULT_END_DATE)))
            .andExpect(jsonPath("$.avatarUrl").value(containsString("aaaaa.jpg")))
            .andExpect(jsonPath("$.avatarUrl").value(DEFAULT_AVATAR_URL))
            .andExpect(jsonPath("$.description").value(DEFAULT_DESCRIPTION))
            .andExpect(jsonPath("$.data.AAAAAAAAAA").value("BBBBBBBBBB"))

            .andExpect(jsonPath("$.targets[0].id").value(notNullValue()))
            .andExpect(jsonPath("$.targets[0].name").value(DEFAULT_LN_TARGET_NAME))
            .andExpect(jsonPath("$.targets[0].typeKey").value(DEFAULT_LN_TARGET_KEY))
            .andExpect(jsonPath("$.targets[0].source").value(presaved.getId()))
            .andExpect(jsonPath("$.targets[0].target.id").value(id));
            return resultXmEntity;
        });

        xmEntitySearchRepository.refresh();
        // Validate the XmEntity in Elasticsearch
        XmEntity xmEntityEs = xmEntitySearchRepository.findById(valueOf(testXmEntity.getId().toString()))
                                                      .orElseThrow(NullPointerException::new);
        // TODO - may be compare avatarUrl too ?
        assertThat(xmEntityEs).isEqualToIgnoringGivenFields(testXmEntity, "sources", "avatarUrlRelative");


        separateTransactionExecutor.doInSeparateTransaction(() -> {
            testXmEntity.getSources().forEach(it -> linkService.delete(it.getId()));
            xmEntityService.delete(testXmEntity.getId());
            return null;
        });
    }

    @Test
    public void createXmEntityWithLargeData() throws Exception {

        XmEntity testXmEntity = separateTransactionExecutor.doInSeparateTransaction(() -> {

            int databaseSizeBeforeCreate = xmEntityRepository.findAll().size();

            xmEntityIncoming.setData(LARGE_DATA);

            // Create the XmEntity
            MvcResult result = performPost("/api/xm-entities", xmEntityIncoming)
                .andExpect(status().isCreated())
                .andReturn();

            Integer id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");

            // Get the xmEntityPersisted with tag by ID
            performGet("/api/xm-entities/{id}", id)
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.key").value(DEFAULT_KEY))
                .andExpect(jsonPath("$.typeKey").value(DEFAULT_TYPE_KEY))
                .andExpect(jsonPath("$.stateKey").value(DEFAULT_STATE_KEY))
                .andExpect(jsonPath("$.name").value(DEFAULT_NAME))
                .andExpect(jsonPath("$.startDate").value(sameInstant(MOCKED_START_DATE)))
                .andExpect(jsonPath("$.updateDate").value(sameInstant(MOCKED_UPDATE_DATE)))
                .andExpect(jsonPath("$.endDate").value(sameInstant(DEFAULT_END_DATE)))
                .andExpect(jsonPath("$.avatarUrl").value(containsString("aaaaa.jpg")))
                .andExpect(jsonPath("$.avatarUrl").value(DEFAULT_AVATAR_URL))
                .andExpect(jsonPath("$.description").value(DEFAULT_DESCRIPTION))
                .andExpect(jsonPath("$.data.AAAAAAAAAA").value(LARGE_VALUE));

            // Validate the XmEntity in the database
            List<XmEntity> xmEntityList = validateEntityInDB(databaseSizeBeforeCreate + 1);

            XmEntity resultXmEntity = xmEntityList.get(xmEntityList.size() - 1);
            assertNotNull(resultXmEntity.getId());
            assertThat(resultXmEntity.getData()).isEqualTo(LARGE_DATA);
            return resultXmEntity;
        });

        xmEntitySearchRepository.refresh();
        // Validate the XmEntity in Elasticsearch
        XmEntity xmEntityEs = xmEntitySearchRepository.findById(testXmEntity.getId())
                                                      .orElseThrow(NullPointerException::new);
        assertThat(xmEntityEs).isEqualToIgnoringGivenFields(testXmEntity, "avatarUrlRelative");
        assertThat(xmEntityEs.getData()).isEqualTo(LARGE_DATA);
        separateTransactionExecutor.doInSeparateTransaction(() -> {
            xmEntityService.delete(testXmEntity.getId());
            return null;
        });
    }

    @Test
    @WithMockUser(authorities = "SUPER-ADMIN")
    public void testSearchByTypeKeyAndQuery() throws Exception {

        prepareSearch();

        String urlTemplate = "/api/_search-with-typekey/xm-entities?typeKey=ACCOUNT&size=5";

        performSearchGet(urlTemplate)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$.[0].tags[0].name").value(DEFAULT_TAG_NAME))
            .andExpect(jsonPath("$.[1].tags[0].name").value(DEFAULT_TAG_NAME));

        performSearchGet(urlTemplate + "&query=" + UNIQ_DESCRIPTION)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$.[0].tags[0].name").value(DEFAULT_TAG_NAME));

        performSearchGet(urlTemplate + "&query=" + NOT_PRESENT_UNIQ_DESCRIPTION)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));

        deleteData();
    }

    @Test
    @WithMockUser(authorities = "SUPER-ADMIN")
    public void testSearchByTypeKeyAndTemplate() throws Exception {

        prepareSearch();

        String urlTemplate = "/api/_search-with-typekey-and-template/xm-entities?typeKey=ACCOUNT&size=5";

        performSearchGet(urlTemplate)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$.[0].tags[0].name").value(DEFAULT_TAG_NAME))
            .andExpect(jsonPath("$.[1].tags[0].name").value(DEFAULT_TAG_NAME));

        performSearchGet(urlTemplate + "&template=UNIQ_DESCRIPTION&templateParams[description]=" + UNIQ_DESCRIPTION)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$.[0].tags[0].name").value(DEFAULT_TAG_NAME));

        performSearchGet(
            urlTemplate + "&template=UNIQ_DESCRIPTION&templateParams[description]=" + NOT_PRESENT_UNIQ_DESCRIPTION)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));

        deleteData();
    }

    private void deleteData() {
        transactionService.inNestedTransaction(() -> {
            xmEntitySearchRepository.deleteAll();
            xmEntityRepository.deleteAll();
            return null;
        }, this::setup);
    }

    private void prepareSearch() {
        transactionService.inNestedTransaction(() -> {
            // FIXME - fails if run test in Idea. But its needed for test running from console. need fix.
            try {
                xmEntitySearchRepository.deleteAll();
            } catch (Exception e) {
                log.warn("Suppress index deletion exception in tenant context: {}", String.valueOf(e));
            }

            // Initialize the database
            xmEntityService.save(createEntityComplexIncoming().typeKey(DEFAULT_TYPE_KEY).stateKey(DEFAULT_STATE_KEY));
            xmEntityService.save(createEntityComplexIncoming().typeKey(UPDATED_TYPE_KEY)
                                                              .description(UNIQ_DESCRIPTION)
                                                              .stateKey(DEFAULT_STATE_KEY));
            xmEntityService.save(createEntityComplexIncoming().typeKey(SEARCH_TEST_KEY)
                                                              .stateKey(null)
                                                              .data(null)
                                                              .attachments(emptySet())
                                                              .tags(emptySet())
                                                              .locations(emptySet()));

            return null;
        }, this::setup);
        xmEntitySearchRepository.refresh();
    }

    private int prepareCalendar() throws Exception {
        // Create the XmEntity with tag
        MvcResult result = performPost("/api/xm-entities", xmEntityIncoming)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.key").value(DEFAULT_KEY))
            .andReturn();

        int id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");

        xmEntityIncoming.setId((long) id);

        val calendar = new Calendar().name("name").typeKey("TYPEKEY")
                                     .xmEntity(xmEntityIncoming);

        MvcResult resultSaveCalendar = performPost("/api/calendars", calendar)
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        int calendarId = JsonPath.read(resultSaveCalendar.getResponse().getContentAsString(), "$.id");

        calendar.setId((long) calendarId);

        val event = new Event().typeKey("TYPEKEY").title("title")
                               .calendar(calendar).assigned(xmEntityIncoming);

        performPost("/api/events", event)
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        List<XmEntity> xmEntityList = xmEntityRepository.findAll();
        em.detach(xmEntityList.get(xmEntityList.size() - 1));

        List<Calendar> calendarList = calendarService.findAll(null);
        em.detach(calendarList.get(calendarList.size() - 1));
        return id;
    }

    @SneakyThrows
    private XmEntity readValue(MvcResult r) {
        return jacksonMessageConverter.getObjectMapper()
                                      .readValue(r.getResponse().getContentAsString(), XmEntity.class);
    }

    @Test
    @Transactional
    public void manageXmEntityAvatarUrl() throws Exception {

        MvcResult mvcResult = separateTransactionExecutor.doInSeparateTransaction(() -> {

            XmEntity presaved = xmEntityService.save(createEntity());

            int databaseSizeBeforeCreate = xmEntityRepository.findAll().size();

            XmEntity entity = xmEntityIncoming;
            entity
                .avatarUrl(DEFAULT_AVATAR_URL_PREFIX + "ccccc.jpg")
                .getTargets().add(new Link()
                                      .typeKey(DEFAULT_LN_TARGET_KEY)
                                      .name(DEFAULT_LN_TARGET_NAME)
                                      .startDate(DEFAULT_LN_TARGET_START_DATE)
                                      .target(presaved)
                                 );

            // Create the XmEntity with link
            MvcResult result = performPost("/api/xm-entities", entity)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.key").value(DEFAULT_KEY))
                .andExpect(jsonPath("$.avatarUrl").value(DEFAULT_AVATAR_URL_PREFIX + "ccccc.jpg"))
                .andExpect(jsonPath("$.avatarUrlFull").doesNotExist())
                .andExpect(jsonPath("$.avatarUrlRelative").doesNotExist())
                .andReturn();

            Integer id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");

            validateEntityInDB(databaseSizeBeforeCreate + 1);

            assertNotNull(presaved.getId());

            // Get the xmEntityPersisted with tag by ID
            result = performGet("/api/xm-entities/{id}", id)
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value(DEFAULT_NAME))
                .andExpect(jsonPath("$.avatarUrl").value(DEFAULT_AVATAR_URL_PREFIX + "ccccc.jpg"))
                .andExpect(jsonPath("$.avatarUrlFull").doesNotExist())
                .andExpect(jsonPath("$.avatarUrlRelative").doesNotExist())

                .andExpect(jsonPath("$.targets[0].source").value(id))
                .andExpect(jsonPath("$.targets[0].target.id").value(presaved.getId()))
                .andExpect(jsonPath("$.targets[0].target.avatarUrl").value(DEFAULT_AVATAR_URL))
                .andReturn()
            ;

            XmEntity toUpdate = convertJsonToObject(result.getResponse().getContentAsString());
            toUpdate.setAvatarUrl(DEFAULT_AVATAR_URL_PREFIX + "bbbbb.jpg");
            toUpdate.setName("new_name1");

            performPut(toUpdate)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.key").value(DEFAULT_KEY))
                .andExpect(jsonPath("$.name").value("new_name1"))
                .andExpect(jsonPath("$.avatarUrlFull").doesNotExist())
                .andExpect(jsonPath("$.avatarUrlRelative").doesNotExist())
                // TODO fix: old entity was returned as a result
                //            .andExpect(jsonPath("$.avatarUrl").value(DEFAULT_AVATAR_URL_PREFIX+"bbbbb.jpg"))
                .andReturn();

            result = performGet("/api/xm-entities/{id}?embed=targets", id)
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("new_name1"))
                .andExpect(jsonPath("$.avatarUrl").value(DEFAULT_AVATAR_URL_PREFIX + "bbbbb.jpg"))
                .andExpect(jsonPath("$.avatarUrlFull").doesNotExist())
                .andExpect(jsonPath("$.avatarUrlRelative").doesNotExist())

                .andExpect(jsonPath("$.targets[0].source").value(id))
                .andExpect(jsonPath("$.targets[0].target.id").value(presaved.getId()))
                .andExpect(jsonPath("$.targets[0].target.avatarUrl").value(DEFAULT_AVATAR_URL))
                .andReturn()
            ;
            return result;
        });

        Integer id = JsonPath.read(mvcResult.getResponse().getContentAsString(), "$.id");

        xmEntitySearchRepository.refresh();
        // Validate the XmEntity in Elasticsearch
        XmEntity xmEntityEs = xmEntitySearchRepository.findById(valueOf(id.toString()))
                                                      .orElseThrow(NullPointerException::new);
        XmEntity testXmEntity = convertJsonToObject(mvcResult.getResponse().getContentAsString());
        assertThat(xmEntityEs).isEqualToIgnoringGivenFields(testXmEntity,
                                                            "version",
                                                            "avatarUrlRelative",
                                                            "avatarUrlFull");

        // TODO: fix: old avatarUrl inside elasticsearch after save
//        assertEquals(testXmEntity.getAvatarUrl(), xmEntityEs.getAvatarUrl());
//        assertEquals(testXmEntity.getAvatarUrlRelative(), xmEntityEs.getAvatarUrlRelative());
        separateTransactionExecutor.doInSeparateTransaction(() -> {
            xmEntityService.delete(id.longValue());
            return null;
        });
    }

    @Test
    @SneakyThrows
    @WithMockUser(authorities = "SUPER-ADMIN")
    @Transactional
    public void testSaveXmEntityToElasticNoCycleJson() {

        String createdBy = "admin";

        XmEntity source = xmEntityRepository.saveAndFlush(createComplexEntityPersistable().typeKey("ACCOUNT.ADMIN"))
                                            .createdBy(createdBy);
        XmEntity target = xmEntityRepository.saveAndFlush(createComplexEntityPersistable().typeKey("ACCOUNT.USER"))
                                            .createdBy(createdBy);

        String defDescription = DEFAULT_DESCRIPTION;
        String lnTypekey = DEFAULT_LN_TARGET_KEY;
        String lnName = DEFAULT_LN_TARGET_NAME;
        Instant lnStartDate = DEFAULT_LN_TARGET_START_DATE;
        Instant lnEndDate = Instant.ofEpochMilli(lnStartDate.toEpochMilli() + 100L);

        source.getTargets().add(new Link()
                                    .typeKey(lnTypekey)
                                    .name(lnName)
                                    .description(defDescription)
                                    .startDate(lnStartDate)
                                    .endDate(lnEndDate)
                                    .target(target)
                                    .source(source));

        // add cyclic Link
        target.getTargets().add(new Link()
                                    .typeKey(lnTypekey)
                                    .name(lnName)
                                    .description(defDescription)
                                    .startDate(lnStartDate)
                                    .endDate(lnEndDate)
                                    .target(source)
                                    .source(target)
        );

        xmEntitySearchRepository.save(source);

        xmEntitySearchRepository.refresh();

        assertNotNull(source.getId());
        Integer srcId = source.getId().intValue();
        String tgtStartDate = DEFAULT_START_DATE.toString();
        String tgtUpdateDate = DEFAULT_UPDATE_DATE.toString();
        String tgtEndDate = DEFAULT_END_DATE.toString();

        performSearchGet("/api/_search/xm-entities?query=id:{id}", source.getId())
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$.[*].id", containsInAnyOrder(srcId)))
            .andExpect(jsonPath("$.[*].typeKey", containsInAnyOrder("ACCOUNT.ADMIN")))
            .andExpect(jsonPath("$.[*].key", containsInAnyOrder(DEFAULT_KEY)))
            .andExpect(jsonPath("$.[*].name", containsInAnyOrder(DEFAULT_NAME)))
            .andExpect(jsonPath("$.[*].description", containsInAnyOrder(DEFAULT_DESCRIPTION)))
            .andExpect(jsonPath("$.[*].startDate", containsInAnyOrder(tgtStartDate)))
            .andExpect(jsonPath("$.[*].endDate", containsInAnyOrder(tgtEndDate)))
            .andExpect(jsonPath("$.[*].updateDate", containsInAnyOrder(tgtUpdateDate)))
            .andExpect(jsonPath("$.[*].source").doesNotExist())
            .andExpect(jsonPath("$.[*].data.AAAAAAAAAA", containsInAnyOrder("BBBBBBBBBB")))
            .andExpect(jsonPath("$.[*].removed", containsInAnyOrder(false)))
            .andExpect(jsonPath("$.[*].version", containsInAnyOrder(1)))
            .andExpect(jsonPath("$.[*].avatarUrl").exists())

            .andExpect(jsonPath("$.[*].calendars").exists())
            .andExpect(jsonPath("$.[*].ratings").exists())
            .andExpect(jsonPath("$.[*].comments").exists())
            .andExpect(jsonPath("$.[*].attachments.[*].id").exists())
            .andExpect(jsonPath("$.[*].locations.[*].id").exists())
            .andExpect(jsonPath("$.[*].tags.[*].id").exists())

            .andExpect(jsonPath("$.[*].targets", hasSize(1)))
            .andExpect(jsonPath("$.[*].targets.[*].source", containsInAnyOrder(srcId)))
            .andExpect(jsonPath("$.[*].targets.[*].target", hasSize(1)))
            .andExpect(jsonPath("$.[*].targets.[*].target.id", hasSize(1)))
            .andExpect(jsonPath("$.[*].targets.[*].target.key", containsInAnyOrder(DEFAULT_KEY)))
            .andExpect(jsonPath("$.[*].targets.[*].target.typeKey", containsInAnyOrder("ACCOUNT.USER")))
            .andExpect(jsonPath("$.[*].targets.[*].target.stateKey", containsInAnyOrder(DEFAULT_STATE_KEY)))
            .andExpect(jsonPath("$.[*].targets.[*].target.name", containsInAnyOrder(DEFAULT_NAME)))
            .andExpect(jsonPath("$.[*].targets.[*].target.startDate", containsInAnyOrder(tgtStartDate)))
            .andExpect(jsonPath("$.[*].targets.[*].target.endDate", containsInAnyOrder(tgtEndDate)))
            .andExpect(jsonPath("$.[*].targets.[*].target.updateDate", containsInAnyOrder(tgtUpdateDate)))
            .andExpect(jsonPath("$.[*].targets.[*].target.description", containsInAnyOrder(defDescription)))
            .andExpect(jsonPath("$.[*].targets.[*].target.createdBy", containsInAnyOrder(createdBy)))
            .andExpect(jsonPath("$.[*].targets.[*].target.data").exists())
            .andExpect(jsonPath("$.[*].targets.[*].target.data.AAAAAAAAAA", containsInAnyOrder("BBBBBBBBBB")))
        ;

    }

    private XmEntity createComplexEntityPersistable() {

        XmEntity entity = createEntityComplexIncoming();

        entity.getAttachments().forEach(inner -> inner.setXmEntity(entity));
        entity.getTags().forEach(inner -> inner.setXmEntity(entity));
        entity.getLocations().forEach(inner -> inner.setXmEntity(entity));

        return entity;

    }
}
