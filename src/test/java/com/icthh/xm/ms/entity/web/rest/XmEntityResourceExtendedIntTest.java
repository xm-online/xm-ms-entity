package com.icthh.xm.ms.entity.web.rest;

import static com.google.common.collect.ImmutableMap.of;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_AUTH_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import static com.icthh.xm.commons.tenant.TenantContextUtils.getRequiredTenantKeyValue;
import static com.icthh.xm.ms.entity.config.TenantConfigMockConfiguration.getXmEntityTemplatesSpec;
import static com.icthh.xm.ms.entity.web.rest.TestUtil.sameInstant;
import static java.lang.Long.valueOf;
import static java.time.Instant.now;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Objects.requireNonNull;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hibernate.validator.internal.util.CollectionHelper.asSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.icthh.xm.ms.entity.AbstractSpringBootTest;
import com.icthh.xm.ms.entity.config.ApplicationProperties;
import com.icthh.xm.ms.entity.config.Constants;
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
import com.icthh.xm.ms.entity.domain.serializer.XmSquigglyInterceptor;
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
import com.icthh.xm.ms.entity.service.XmEntityService;
import com.icthh.xm.ms.entity.service.XmEntitySpecService;
import com.icthh.xm.ms.entity.service.XmEntityTemplatesSpecService;
import com.icthh.xm.ms.entity.service.impl.StartUpdateDateGenerationStrategy;
import com.icthh.xm.ms.entity.service.impl.XmEntityServiceImpl;
import com.jayway.jsonpath.JsonPath;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.persistence.EntityManager;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
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

/**
 * Extension Test class for the XmEntityResource REST controller. Contains additional test apart from Jhipster generated
 * logic
 *
 * @see XmEntityResource
 */
@Slf4j
@Transactional
public class XmEntityResourceExtendedIntTest extends AbstractSpringBootTest {

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

    @Autowired
    XmSquigglyInterceptor xmSquigglyInterceptor;

    @Mock
    private XmAuthenticationContextHolder authContextHolder;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private InternalTransactionService transactionService;

    @Mock
    private XmAuthenticationContext context;

    @Spy
    private StartUpdateDateGenerationStrategy startUpdateDateGenerationStrategy;

    private MockMvc restXmEntityMockMvc;

    private XmEntity xmEntityIncoming;

    @BeforeTransaction
    public void beforeTransaction() {
        TenantContextUtils.setTenant(tenantContextHolder, "RESINTTEST");
    }

    @Before
    public void setup() {
        log.info("Init setup");

        TenantContextUtils.setTenant(tenantContextHolder, "RESINTTEST");
        //xmEntitySearchRepository.deleteAll();

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
                                                                      eventRepository);

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
                                                  .addMappedInterceptors(WebMvcConfiguration.getJsonFilterAllowedURIs(),
                                                                         xmSquigglyInterceptor)
                                                  .build();

        xmEntityIncoming = createEntityComplexIncoming();

        objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    @After
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
     * Creates XmEntiy with tags and persist to DB. Emulates existing XM entity manipulation
     *
     * @param em - Entity manager
     * @return XmEntity persisted in DB
     */
    public static XmEntity createEntityComplexPersisted(EntityManager em) {

        XmEntity entity = createEntity();
        em.persist(entity);
        em.flush();

        entity.addTags(new Tag()
                           .typeKey(DEFAULT_TAG_KEY)
                           .startDate(DEFAULT_START_DATE)
                           .name(DEFAULT_TAG_NAME));
        em.persist(entity);
        em.flush();
        return entity;
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
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
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
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
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
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
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
    @WithMockUser(authorities = "SUPER-ADMIN")
    public void getXmEntitySourcesLinksByTypeKey() throws Exception {

        XmEntity target = xmEntityService.save(createEntity().name("TARGET"));
        XmEntity source1 = xmEntityService.save(createEntity().name("SOURCE1").createdBy("admin"));
        XmEntity source2 = xmEntityService.save(createEntity().name("SOURCE2").createdBy("admin"));

        // should not appear in output
        XmEntity targetOther = xmEntityService.save(createEntity().name("TARGET_OTHER"));

        String lnkName = DEFAULT_NAME;
        String lnkDescription = DEFAULT_DESCRIPTION;
        Instant lnkStart = Instant.now();
        Instant lnkEnd = lnkStart.plusMillis(1000);

        source1.getTargets().clear();
        source1.getTargets().add(new Link().typeKey("LINK1")
                                           .name(lnkName).description(lnkDescription)
                                           .startDate(lnkStart).endDate(lnkEnd)
                                           .source(source1).target(target));
        source1.getTargets().add(new Link().typeKey("LINK1")
                                           .name(lnkName).description(lnkDescription)
                                           .startDate(lnkStart).endDate(lnkEnd)
                                           .source(source1).target(targetOther));

        source2.getTargets().clear();
        source2.getTargets().add(new Link().typeKey("LINK1")
                                           .name(lnkName).description(lnkDescription)
                                           .startDate(lnkStart).endDate(lnkEnd)
                                           .source(source2).target(target));
        source2.getTargets().add(new Link().typeKey("LINK2")
                                           .name(lnkName).description(lnkDescription)
                                           .startDate(lnkStart).endDate(lnkEnd)
                                           .source(source2).target(target));

        assertNotNull(target.getId());
        assertNotNull(source1.getId());
        assertNotNull(source2.getId());

        Integer targetId = target.getId().intValue();
        Integer srcId1 = source1.getId().intValue();
        Integer srcId2 = source2.getId().intValue();

        // Get the xmEntityPersisted with tag by ID
        performGet("/api/v2/xm-entities/{id}/links/sources?typeKeys={typeKeys}", targetId, "LINK1")
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$.[*].id", hasSize(2)))
            .andExpect(jsonPath("$.[*].typeKey", containsInAnyOrder(two("LINK1"))))
            .andExpect(jsonPath("$.[*].name", containsInAnyOrder(two(lnkName))))
            .andExpect(jsonPath("$.[*].description", containsInAnyOrder(two(lnkDescription))))
            .andExpect(jsonPath("$.[*].startDate", containsInAnyOrder(two(lnkStart.toString()))))
            .andExpect(jsonPath("$.[*].endDate", containsInAnyOrder(two(lnkEnd.toString()))))
            .andExpect(jsonPath("$.[*].target", containsInAnyOrder(two(targetId))))

            .andExpect(jsonPath("$.[*].source").exists())
            .andExpect(jsonPath("$.[*].source.id").value(containsInAnyOrder(srcId1, srcId2)))
            .andExpect(jsonPath("$.[*].source.name").value(containsInAnyOrder("SOURCE1", "SOURCE2")))
            .andExpect(jsonPath("$.[*].source.key", containsInAnyOrder(two(DEFAULT_KEY))))
            .andExpect(jsonPath("$.[*].source.typeKey", containsInAnyOrder(two(DEFAULT_TYPE_KEY))))
            .andExpect(jsonPath("$.[*].source.stateKey", containsInAnyOrder(two(DEFAULT_STATE_KEY))))
            .andExpect(jsonPath("$.[*].source.startDate", containsInAnyOrder(two(MOCKED_START_DATE.toString()))))
            .andExpect(jsonPath("$.[*].source.endDate", containsInAnyOrder(two(DEFAULT_END_DATE.toString()))))
            .andExpect(jsonPath("$.[*].source.updateDate", containsInAnyOrder(two(MOCKED_UPDATE_DATE.toString()))))
            .andExpect(jsonPath("$.[*].source.description", containsInAnyOrder(two(DEFAULT_DESCRIPTION))))
            .andExpect(jsonPath("$.[*].source.createdBy", containsInAnyOrder(two("admin"))))
            .andExpect(jsonPath("$.[*].source.removed", containsInAnyOrder(two(false))))
            .andExpect(jsonPath("$.[*].source.data").exists())
            .andExpect(jsonPath("$.[*].source.data.AAAAAAAAAA", containsInAnyOrder(two("BBBBBBBBBB"))))

            .andExpect(jsonPath("$.[*].source.targets").doesNotExist())
            .andExpect(jsonPath("$.[*].source.sources").doesNotExist())
            .andExpect(jsonPath("$.[*].source.avatarUrlRelative").doesNotExist())
            .andExpect(jsonPath("$.[*].source.avatarUrlFull").doesNotExist())
            .andExpect(jsonPath("$.[*].source.version").doesNotExist())
            .andExpect(jsonPath("$.[*].source.attachments").doesNotExist())
            .andExpect(jsonPath("$.[*].source.locations").doesNotExist())
            .andExpect(jsonPath("$.[*].source.tags").doesNotExist())
            .andExpect(jsonPath("$.[*].source.calendars").doesNotExist())
            .andExpect(jsonPath("$.[*].source.ratings").doesNotExist())
            .andExpect(jsonPath("$.[*].source.comments").doesNotExist())
            .andExpect(jsonPath("$.[*].source.votes").doesNotExist())
            .andExpect(jsonPath("$.[*].source.functionContexts").doesNotExist())
            .andExpect(jsonPath("$.[*].source.events").doesNotExist())
            .andExpect(jsonPath("$.[*].source.uniqueFields").doesNotExist())
        ;

    }

    @Test
    @WithMockUser(authorities = "SUPER-ADMIN")
    public void getXmEntitySourcesLinksByTypeKeysIn() throws Exception {

        XmEntity target = xmEntityService.save(createEntity().name("TARGET"));
        XmEntity source1 = xmEntityService.save(createEntity().name("SOURCE1"));
        XmEntity source2 = xmEntityService.save(createEntity()).name("SOURCE2");

        source1.getTargets().clear();
        source1.getTargets().add(new Link().typeKey("LINK1").source(source1).target(target));

        source2.getTargets().clear();
        source2.getTargets().add(new Link().typeKey("LINK1").source(source2).target(target));
        source2.getTargets().add(new Link().typeKey("LINK2").source(source2).target(target));
        source2.getTargets().add(new Link().typeKey("LINK3").source(source2).target(target));

        assertNotNull(target.getId());
        assertNotNull(source1.getId());
        assertNotNull(source2.getId());

        Integer targetId = target.getId().intValue();
        Integer srcId1 = source1.getId().intValue();
        Integer srcId2 = source2.getId().intValue();

        // Get the xmEntityPersisted with tag by ID
        performGet("/api/v2/xm-entities/{id}/links/sources?typeKeys={typeKeys}", target.getId(), "LINK1,LINK2")
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$", hasSize(3)))
            .andExpect(jsonPath("$.[*].typeKey").value(containsInAnyOrder("LINK1", "LINK1", "LINK2")))
            .andExpect(jsonPath("$.[*].target").value(containsInAnyOrder(targetId, targetId, targetId)))
            .andExpect(jsonPath("$.[*].source.id").value(containsInAnyOrder(srcId1, srcId2, srcId2)))
            .andExpect(jsonPath("$.[*].source.name").value(containsInAnyOrder("SOURCE1", "SOURCE2", "SOURCE2")))
            .andExpect(jsonPath("$.[*].source.targets").doesNotExist())
        ;

        // Get the xmEntityPersisted with tag by ID
        performGet("/api/v2/xm-entities/{id}/links/sources?typeKeys={typeKey}&typeKeys={typeKey}",
                   target.getId(),
                   "LINK1",
                   "LINK2")
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$", hasSize(3)))
            .andExpect(jsonPath("$.[*].typeKey").value(containsInAnyOrder("LINK1", "LINK1", "LINK2")))
            .andExpect(jsonPath("$.[*].target").value(containsInAnyOrder(targetId, targetId, targetId)))
            .andExpect(jsonPath("$.[*].source.id").value(containsInAnyOrder(srcId1, srcId2, srcId2)))
            .andExpect(jsonPath("$.[*].source.name").value(containsInAnyOrder("SOURCE1", "SOURCE2", "SOURCE2")))
            .andExpect(jsonPath("$.[*].source.targets").doesNotExist())
        ;
    }

    @Test
    @WithMockUser(authorities = "SUPER-ADMIN")
    public void getXmEntitySourcesLinksWithAnyTypeKay() throws Exception {

        XmEntity target = xmEntityService.save(createEntity().name("TARGET"));
        XmEntity source1 = xmEntityService.save(createEntity().name("SOURCE1"));
        XmEntity source2 = xmEntityService.save(createEntity()).name("SOURCE2");

        source1.getTargets().clear();
        source1.getTargets().add(new Link().typeKey("LINK1").source(source1).target(target));

        source2.getTargets().clear();
        source2.getTargets().add(new Link().typeKey("LINK2").source(source2).target(target));

        assertNotNull(target.getId());
        assertNotNull(source1.getId());
        assertNotNull(source2.getId());

        Integer targetId = target.getId().intValue();
        Integer srcId1 = source1.getId().intValue();
        Integer srcId2 = source2.getId().intValue();

        // Get the xmEntityPersisted with tag by ID
        performGet("/api/v2/xm-entities/{id}/links/sources", target.getId())
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$.[*].typeKey").value(containsInAnyOrder("LINK1", "LINK2")))
            .andExpect(jsonPath("$.[*].target").value(containsInAnyOrder(targetId, targetId)))
            .andExpect(jsonPath("$.[*].source.id").value(containsInAnyOrder(srcId1, srcId2)))
            .andExpect(jsonPath("$.[*].source.name").value(containsInAnyOrder("SOURCE1", "SOURCE2")))
            .andExpect(jsonPath("$.[*].source.targets").doesNotExist())
        ;

    }

    @Test
    @WithMockUser(authorities = "SUPER-ADMIN")
    public void getXmEntitySourcesLinksWithAnyTypeKayAndSort() throws Exception {

        XmEntity target = xmEntityService.save(createEntity().name("TARGET"));
        XmEntity source1 = xmEntityService.save(createEntity().name("SOURCE1"));
        XmEntity source2 = xmEntityService.save(createEntity()).name("SOURCE2");

        source1.getTargets().clear();
        source1.getTargets().add(new Link().typeKey("LINK1").source(source1).target(target));

        source2.getTargets().clear();
        source2.getTargets().add(new Link().typeKey("LINK2").source(source2).target(target));

        assertNotNull(target.getId());
        assertNotNull(source1.getId());
        assertNotNull(source2.getId());

        Integer targetId = target.getId().intValue();
        Integer srcId1 = source1.getId().intValue();
        Integer srcId2 = source2.getId().intValue();

        // Get the xmEntityPersisted with tag by ID
        performGet("/api/v2/xm-entities/{id}/links/sources?sort=typeKey,asc", target.getId())
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$.[*].typeKey").value(contains("LINK1", "LINK2")))
            .andExpect(jsonPath("$.[*].target").value(contains(targetId, targetId)))
            .andExpect(jsonPath("$.[*].source.id").value(contains(srcId1, srcId2)))
            .andExpect(jsonPath("$.[*].source.name").value(contains("SOURCE1", "SOURCE2")))
            .andExpect(jsonPath("$.[*].source.targets").doesNotExist())
        ;

        // Get the xmEntityPersisted with tag by ID
        performGet("/api/v2/xm-entities/{id}/links/sources?sort=typeKey,desc", target.getId())
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$.[*].typeKey").value(contains("LINK2", "LINK1")))
            .andExpect(jsonPath("$.[*].target").value(contains(targetId, targetId)))
            .andExpect(jsonPath("$.[*].source.id").value(contains(srcId2, srcId1)))
            .andExpect(jsonPath("$.[*].source.name").value(contains("SOURCE2", "SOURCE1")))
            .andExpect(jsonPath("$.[*].source.targets").doesNotExist())
        ;

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
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
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
    public void checkJsonShemeDateTypeProperties() throws Exception {

        int databaseSizeBeforeTest = xmEntityRepository.findAll().size();

        XmEntity entity = xmEntityIncoming;

        entity.setData(of("numberProperties", "5"));

        performPut(entity)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("error.validation"));

        // Validate the XmEntity in the database
        validateEntityInDB(databaseSizeBeforeTest);

        entity.setData(of("numberProperties", "testse"));

        performPut(entity)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("error.validation"));

        // Validate the XmEntity in the database
        validateEntityInDB(databaseSizeBeforeTest);

        entity.setData(of("numberProperties", Boolean.FALSE));

        performPut(entity)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("error.validation"));

        // Validate the XmEntity in the database
        validateEntityInDB(databaseSizeBeforeTest);

    }

    @Test
    public void checkJsonShemeDateIsRequired() throws Exception {
        int databaseSizeBeforeTest = xmEntityRepository.findAll().size();

        xmEntityIncoming.setData(emptyMap());

        performPut(xmEntityIncoming)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("error.validation"));

        validateEntityInDB(databaseSizeBeforeTest);
    }

    @Test
    @WithMockUser(authorities = "SUPER-ADMIN")
    public void checkValidationNameAndKey() throws Exception {
        int databaseSizeBeforeTest = xmEntityRepository.findAll().size();

        XmEntity entity = createEntity();
        entity.setTypeKey("ACCOUNT");
        entity.setName(null);
        entity.setKey(null);

        performPost("/api/xm-entities", entity)
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("error.validation"));

        XmEntity entity2 = createEntity();
        entity2.setTypeKey("ENTITY2");
        entity2.setName(null);
        entity2.setKey(null);
        entity2.setData(null);
        entity.setStateKey(null);

        performPost("/api/xm-entities", entity2)
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("error.validation"));

        validateEntityInDB(databaseSizeBeforeTest);
    }

    @Test
    @WithMockUser(authorities = "SUPER-ADMIN")
    public void checkValidationNotRequiredNameAndKey() throws Exception {
        long databaseSizeBeforeTest = xmEntityRepository.count();

        XmEntity entity = createEntity();
        entity.setTypeKey("ENTITY1");
        entity.setName(null);
        entity.setKey(null);
        entity.setData(null);
        entity.setStateKey(null);

        performPost("/api/xm-entities", entity)
            .andDo(print())
            .andExpect(status().is2xxSuccessful());

        assertThat(xmEntityRepository.count()).isEqualTo(databaseSizeBeforeTest + 1);
    }

    @Test
    @WithMockUser(authorities = "SUPER-ADMIN")
    public void getAllXmEntitiesByTypeKey() throws Exception {

        XmEntity entity = createEntityComplexPersisted(em);

        assertNotNull(entity.getId());

        // Get all the xmEntityList
        performGet("/api/xm-entities?sort=id,desc&typeKey=ACCOUNT")
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(entity.getId().intValue())))
            .andExpect(jsonPath("$.[*].typeKey").value(hasItem(DEFAULT_TYPE_KEY)));
    }

    @Test
    @WithMockUser(authorities = "SUPER-ADMIN")
    public void getAllXmEntitiesByTypeKeyNo() throws Exception {

        createEntityComplexPersisted(em);

        // Get all the xmEntityList
        performGet("/api/xm-entities?sort=id,desc&typeKey=PRICE")
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(everyItem(nullValue())));
    }

    @Test
    @WithMockUser(authorities = "SUPER-ADMIN")
    public void testSearchByTypeKeyAndQuery() throws Exception {

        prepareSearch();

        String urlTemplate = "/api/_search-with-typekey/xm-entities?typeKey=ACCOUNT&size=5";

        performGet(urlTemplate)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$.[0].tags[0].name").value(DEFAULT_TAG_NAME))
            .andExpect(jsonPath("$.[1].tags[0].name").value(DEFAULT_TAG_NAME));

        performGet(urlTemplate + "&query=" + UNIQ_DESCRIPTION)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$.[0].tags[0].name").value(DEFAULT_TAG_NAME));

        performGet(urlTemplate + "&query=" + NOT_PRESENT_UNIQ_DESCRIPTION)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));

        deleteData();
    }

    @Test
    @WithMockUser(authorities = "SUPER-ADMIN")
    public void testSearchByTypeKeyAndTemplate() throws Exception {

        prepareSearch();

        String urlTemplate = "/api/_search-with-typekey-and-template/xm-entities?typeKey=ACCOUNT&size=5";

        performGet(urlTemplate)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$.[0].tags[0].name").value(DEFAULT_TAG_NAME))
            .andExpect(jsonPath("$.[1].tags[0].name").value(DEFAULT_TAG_NAME));

        performGet(urlTemplate + "&template=UNIQ_DESCRIPTION&templateParams[description]=" + UNIQ_DESCRIPTION)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$.[0].tags[0].name").value(DEFAULT_TAG_NAME));

        performGet(
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

    @Test
    @WithMockUser(authorities = {"SUPER-ADMIN"})
    public void xmEntityFieldsNoRelationFields() throws Exception {

        assertThat(xmEntityIncoming.getAttachments().size()).isGreaterThan(0);
        assertThat(xmEntityIncoming.getTags().size()).isGreaterThan(0);
        assertThat(xmEntityIncoming.getLocations().size()).isGreaterThan(0);

        // Create the XmEntity with tag
        MvcResult result = performPost("/api/xm-entities", xmEntityIncoming)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.key").value(DEFAULT_KEY))
            .andReturn();

        int id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");

        List<XmEntity> xmEntityList = xmEntityRepository.findAll();
        em.detach(xmEntityList.get(xmEntityList.size() - 1));
        //);

        performGet("/api/xm-entities/{id}?embed=id", id)
            .andExpect(status().isOk())
            .andDo(this::printMvcResult)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.id").value(id))
            .andExpect(jsonPath("$.key").value(DEFAULT_KEY))
            .andExpect(jsonPath("$.typeKey").value(DEFAULT_TYPE_KEY))
            .andExpect(jsonPath("$.stateKey").value(DEFAULT_STATE_KEY))
            .andExpect(jsonPath("$.name").value(DEFAULT_NAME))
            .andExpect(jsonPath("$.startDate").value(sameInstant(MOCKED_START_DATE)))
            .andExpect(jsonPath("$.updateDate").value(sameInstant(MOCKED_UPDATE_DATE)))
            .andExpect(jsonPath("$.endDate").value(sameInstant(DEFAULT_END_DATE)))
            .andExpect(jsonPath("$.description").value(DEFAULT_DESCRIPTION))
            .andExpect(jsonPath("$.data.AAAAAAAAAA").value("BBBBBBBBBB"))
            .andExpect(jsonPath("$.attachments").value(nullValue()))
            .andExpect(jsonPath("$.tags").value(nullValue()))
            .andExpect(jsonPath("$.locations").value(nullValue()));

        performGet("/api/xm-entities-by-ids?ids={id}&embed=id", id)
            .andExpect(status().isOk())
            .andDo(this::printMvcResult)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$").value(hasSize(1)))
            .andExpect(jsonPath("$.[0].id").value(id))
            .andExpect(jsonPath("$.[0].key").value(DEFAULT_KEY))
            .andExpect(jsonPath("$.[0].typeKey").value(DEFAULT_TYPE_KEY))
            .andExpect(jsonPath("$.[0].stateKey").value(DEFAULT_STATE_KEY))
            .andExpect(jsonPath("$.[0].name").value(DEFAULT_NAME))
            .andExpect(jsonPath("$.[0].startDate").value(sameInstant(MOCKED_START_DATE)))
            .andExpect(jsonPath("$.[0].updateDate").value(sameInstant(MOCKED_UPDATE_DATE)))
            .andExpect(jsonPath("$.[0].endDate").value(sameInstant(DEFAULT_END_DATE)))
            .andExpect(jsonPath("$.[0].description").value(DEFAULT_DESCRIPTION))
            .andExpect(jsonPath("$.[0].data.AAAAAAAAAA").value("BBBBBBBBBB"))
            .andExpect(jsonPath("$.[0].attachments").value(nullValue()))
            .andExpect(jsonPath("$.[0].tags").value(nullValue()))
            .andExpect(jsonPath("$.[0].locations").value(nullValue()));
    }

    @Test
    @WithMockUser(authorities = {"SUPER-ADMIN"})
    public void xmEntityFieldsTwoFields() throws Exception {

        assertThat(xmEntityIncoming.getAttachments().size()).isGreaterThan(0);
        assertThat(xmEntityIncoming.getTags().size()).isGreaterThan(0);
        assertThat(xmEntityIncoming.getLocations().size()).isGreaterThan(0);

        // Create the XmEntity with tag
        MvcResult result = performPost("/api/xm-entities", xmEntityIncoming)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.key").value(DEFAULT_KEY))
            .andReturn();

        int id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");

        List<XmEntity> xmEntityList = xmEntityRepository.findAll();
        em.detach(xmEntityList.get(xmEntityList.size() - 1));
        //);

        performGet("/api/xm-entities/{id}?embed=id,attachments,tags", id)
            .andExpect(status().isOk())
            .andDo(this::printMvcResult)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.id").value(id))
            .andExpect(jsonPath("$.key").value(DEFAULT_KEY))
            .andExpect(jsonPath("$.typeKey").value(DEFAULT_TYPE_KEY))
            .andExpect(jsonPath("$.stateKey").value(DEFAULT_STATE_KEY))
            .andExpect(jsonPath("$.name").value(DEFAULT_NAME))
            .andExpect(jsonPath("$.startDate").value(sameInstant(MOCKED_START_DATE)))
            .andExpect(jsonPath("$.updateDate").value(sameInstant(MOCKED_UPDATE_DATE)))
            .andExpect(jsonPath("$.endDate").value(sameInstant(DEFAULT_END_DATE)))
            .andExpect(jsonPath("$.description").value(DEFAULT_DESCRIPTION))
            .andExpect(jsonPath("$.data.AAAAAAAAAA").value("BBBBBBBBBB"))
            .andExpect(jsonPath("$.attachments.length()").value(1))
            .andExpect(jsonPath("$.tags.length()").value(1))
            .andExpect(jsonPath("$.locations").value(nullValue()));

        performGet("/api/xm-entities-by-ids?ids={id}&embed=id,attachments,tags", id)
            .andExpect(status().isOk())
            .andDo(this::printMvcResult)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$").value(hasSize(1)))
            .andExpect(jsonPath("$.[0].id").value(id))
            .andExpect(jsonPath("$.[0].key").value(DEFAULT_KEY))
            .andExpect(jsonPath("$.[0].typeKey").value(DEFAULT_TYPE_KEY))
            .andExpect(jsonPath("$.[0].stateKey").value(DEFAULT_STATE_KEY))
            .andExpect(jsonPath("$.[0].name").value(DEFAULT_NAME))
            .andExpect(jsonPath("$.[0].startDate").value(sameInstant(MOCKED_START_DATE)))
            .andExpect(jsonPath("$.[0].updateDate").value(sameInstant(MOCKED_UPDATE_DATE)))
            .andExpect(jsonPath("$.[0].endDate").value(sameInstant(DEFAULT_END_DATE)))
            .andExpect(jsonPath("$.[0].description").value(DEFAULT_DESCRIPTION))
            .andExpect(jsonPath("$.[0].data.AAAAAAAAAA").value("BBBBBBBBBB"))
            .andExpect(jsonPath("$.[0].attachments.length()").value(1))
            .andExpect(jsonPath("$.[0].tags.length()").value(1))
            .andExpect(jsonPath("$.[0].locations").value(nullValue()));
    }

    @Test
    @WithMockUser(authorities = {"SUPER-ADMIN"})
    public void xmEntityFieldsDefaultFields() throws Exception {

        assertThat(xmEntityIncoming.getAttachments().size()).isGreaterThan(0);
        assertThat(xmEntityIncoming.getTags().size()).isGreaterThan(0);
        assertThat(xmEntityIncoming.getLocations().size()).isGreaterThan(0);

        // Create the XmEntity with tag
        MvcResult result = performPost("/api/xm-entities", xmEntityIncoming)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.key").value(DEFAULT_KEY))
            .andReturn();

        int id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");

        List<XmEntity> xmEntityList = xmEntityRepository.findAll();
        em.detach(xmEntityList.get(xmEntityList.size() - 1));
        //);

        performGet("/api/xm-entities/{id}", id)
            .andExpect(status().isOk())
            .andDo(this::printMvcResult)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.id").value(id))
            .andExpect(jsonPath("$.key").value(DEFAULT_KEY))
            .andExpect(jsonPath("$.typeKey").value(DEFAULT_TYPE_KEY))
            .andExpect(jsonPath("$.stateKey").value(DEFAULT_STATE_KEY))
            .andExpect(jsonPath("$.name").value(DEFAULT_NAME))
            .andExpect(jsonPath("$.startDate").value(sameInstant(MOCKED_START_DATE)))
            .andExpect(jsonPath("$.updateDate").value(sameInstant(MOCKED_UPDATE_DATE)))
            .andExpect(jsonPath("$.endDate").value(sameInstant(DEFAULT_END_DATE)))
            .andExpect(jsonPath("$.description").value(DEFAULT_DESCRIPTION))
            .andExpect(jsonPath("$.data.AAAAAAAAAA").value("BBBBBBBBBB"))
            .andExpect(jsonPath("$.attachments.length()").value(1))
            .andExpect(jsonPath("$.tags.length()").value(1))
            .andExpect(jsonPath("$.locations.length()").value(1));

        performGet("/api/xm-entities-by-ids?ids={id}", id)
            .andExpect(status().isOk())
            .andDo(this::printMvcResult)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$").value(hasSize(1)))
            .andExpect(jsonPath("$.[0].id").value(id))
            .andExpect(jsonPath("$.[0].key").value(DEFAULT_KEY))
            .andExpect(jsonPath("$.[0].typeKey").value(DEFAULT_TYPE_KEY))
            .andExpect(jsonPath("$.[0].stateKey").value(DEFAULT_STATE_KEY))
            .andExpect(jsonPath("$.[0].name").value(DEFAULT_NAME))
            .andExpect(jsonPath("$.[0].startDate").value(sameInstant(MOCKED_START_DATE)))
            .andExpect(jsonPath("$.[0].updateDate").value(sameInstant(MOCKED_UPDATE_DATE)))
            .andExpect(jsonPath("$.[0].endDate").value(sameInstant(DEFAULT_END_DATE)))
            .andExpect(jsonPath("$.[0].description").value(DEFAULT_DESCRIPTION))
            .andExpect(jsonPath("$.[0].data.AAAAAAAAAA").value("BBBBBBBBBB"))
            .andExpect(jsonPath("$.[0].attachments.length()").value(1))
            .andExpect(jsonPath("$.[0].tags.length()").value(1))
            .andExpect(jsonPath("$.[0].locations.length()").value(1));
    }

    @Test
    @WithMockUser(authorities = "SUPER-ADMIN")
    public void xmEntityFildsCalendars() throws Exception {
        int id = prepareCalendar();

        performGet("/api/xm-entities/{id}?embed=calendars", id)
            .andExpect(status().isOk())
            .andDo(this::printMvcResult)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.id").value(id))
            .andExpect(jsonPath("$.key").value(DEFAULT_KEY))
            .andExpect(jsonPath("$.typeKey").value(DEFAULT_TYPE_KEY))
            .andExpect(jsonPath("$.stateKey").value(DEFAULT_STATE_KEY))
            .andExpect(jsonPath("$.name").value(DEFAULT_NAME))
            .andExpect(jsonPath("$.startDate").value(sameInstant(MOCKED_START_DATE)))
            .andExpect(jsonPath("$.updateDate").value(sameInstant(MOCKED_UPDATE_DATE)))
            .andExpect(jsonPath("$.endDate").value(sameInstant(DEFAULT_END_DATE)))
            .andExpect(jsonPath("$.description").value(DEFAULT_DESCRIPTION))
            .andExpect(jsonPath("$.data.AAAAAAAAAA").value("BBBBBBBBBB"))
            .andExpect(jsonPath("$.calendars.length()").value(1))
            .andExpect(jsonPath("$.calendars[0].events").value(nullValue()));

        performGet("/api/xm-entities-by-ids?ids={id}&embed=calendars", id)
            .andExpect(status().isOk())
            .andDo(this::printMvcResult)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$").value(hasSize(1)))
            .andExpect(jsonPath("$.[0].id").value(id))
            .andExpect(jsonPath("$.[0].key").value(DEFAULT_KEY))
            .andExpect(jsonPath("$.[0].typeKey").value(DEFAULT_TYPE_KEY))
            .andExpect(jsonPath("$.[0].stateKey").value(DEFAULT_STATE_KEY))
            .andExpect(jsonPath("$.[0].name").value(DEFAULT_NAME))
            .andExpect(jsonPath("$.[0].startDate").value(sameInstant(MOCKED_START_DATE)))
            .andExpect(jsonPath("$.[0].updateDate").value(sameInstant(MOCKED_UPDATE_DATE)))
            .andExpect(jsonPath("$.[0].endDate").value(sameInstant(DEFAULT_END_DATE)))
            .andExpect(jsonPath("$.[0].description").value(DEFAULT_DESCRIPTION))
            .andExpect(jsonPath("$.[0].data.AAAAAAAAAA").value("BBBBBBBBBB"))
            .andExpect(jsonPath("$.[0].calendars.length()").value(1))
            .andExpect(jsonPath("$.[0].calendars[0].events").value(nullValue()));
    }

    private int prepareCalendar() throws Exception {
        // Create the XmEntity with tag
        MvcResult result = performPost("/api/xm-entities", xmEntityIncoming)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.key").value(DEFAULT_KEY))
            .andReturn();

        int id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");

        xmEntityIncoming.setId((long) id);

        val calendar = new Calendar().name("name").typeKey("TYPEKEY").startDate(Instant.now())
                                     .xmEntity(xmEntityIncoming);

        MvcResult resultSaveCalendar = performPost("/api/calendars", calendar)
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        int calendarId = JsonPath.read(resultSaveCalendar.getResponse().getContentAsString(), "$.id");

        calendar.setId((long) calendarId);

        val event = new Event().typeKey("TYPEKEY").title("title").startDate(Instant.now())
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

    @Test
    @WithMockUser(authorities = "SUPER-ADMIN")
    public void xmEntityFieldsCalendarsWithEvents() throws Exception {
        int id = prepareCalendar();

        performGet("/api/xm-entities/{id}?embed=calendars.events", id)
            .andExpect(status().isOk())
            .andDo(this::printMvcResult)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.id").value(id))
            .andExpect(jsonPath("$.key").value(DEFAULT_KEY))
            .andExpect(jsonPath("$.typeKey").value(DEFAULT_TYPE_KEY))
            .andExpect(jsonPath("$.stateKey").value(DEFAULT_STATE_KEY))
            .andExpect(jsonPath("$.name").value(DEFAULT_NAME))
            .andExpect(jsonPath("$.startDate").value(sameInstant(MOCKED_START_DATE)))
            .andExpect(jsonPath("$.updateDate").value(sameInstant(MOCKED_UPDATE_DATE)))
            .andExpect(jsonPath("$.endDate").value(sameInstant(DEFAULT_END_DATE)))
            .andExpect(jsonPath("$.description").value(DEFAULT_DESCRIPTION))
            .andExpect(jsonPath("$.data.AAAAAAAAAA").value("BBBBBBBBBB"))
            .andExpect(jsonPath("$.calendars.length()").value(1))
            .andExpect(jsonPath("$.calendars[0].events.length()").value(1));

        performGet("/api/xm-entities-by-ids?ids={id}&embed=calendars.events", id)
            .andExpect(status().isOk())
            .andDo(this::printMvcResult)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$").value(hasSize(1)))
            .andExpect(jsonPath("$.[0].id").value(id))
            .andExpect(jsonPath("$.[0].key").value(DEFAULT_KEY))
            .andExpect(jsonPath("$.[0].typeKey").value(DEFAULT_TYPE_KEY))
            .andExpect(jsonPath("$.[0].stateKey").value(DEFAULT_STATE_KEY))
            .andExpect(jsonPath("$.[0].name").value(DEFAULT_NAME))
            .andExpect(jsonPath("$.[0].startDate").value(sameInstant(MOCKED_START_DATE)))
            .andExpect(jsonPath("$.[0].updateDate").value(sameInstant(MOCKED_UPDATE_DATE)))
            .andExpect(jsonPath("$.[0].endDate").value(sameInstant(DEFAULT_END_DATE)))
            .andExpect(jsonPath("$.[0].description").value(DEFAULT_DESCRIPTION))
            .andExpect(jsonPath("$.[0].data.AAAAAAAAAA").value("BBBBBBBBBB"))
            .andExpect(jsonPath("$.[0].calendars.length()").value(1))
            .andExpect(jsonPath("$.[0].calendars[0].events.length()").value(1));
    }

    @Test
    @WithMockUser(authorities = "SUPER-ADMIN")
    public void updateTenantXmEntity() throws Exception {
        // Initialize the database
        XmEntity tenantEntity = createEntity();
        tenantEntity.setTypeKey(Constants.TENANT_TYPE_KEY);
        tenantEntity.setStateKey(null);
        tenantEntity.setData(null);
        tenantEntity = xmEntityRepository.save(tenantEntity);

        int databaseSizeBeforeUpdate = xmEntityRepository.findAll().size();

        // Update the xmEntity
        XmEntity updatedTenantEntity = createEntity();
        updatedTenantEntity.setId(tenantEntity.getId());
        updatedTenantEntity.setTypeKey(Constants.TENANT_TYPE_KEY);
        updatedTenantEntity.setStateKey(null);
        updatedTenantEntity.setData(null);
        updatedTenantEntity.setName("updatedName");
        updatedTenantEntity.setVersion(null);

        restXmEntityMockMvc.perform(put("/api/xm-entities")
                                        .contentType(TestUtil.APPLICATION_JSON_UTF8)
                                        .content(TestUtil.convertObjectToJsonBytes(updatedTenantEntity)))
                           .andExpect(status().isOk());

        // Validate the XmEntity in the database
        List<XmEntity> xmEntityList = xmEntityRepository.findAll();
        assertThat(xmEntityList).hasSize(databaseSizeBeforeUpdate);
        XmEntity testXmEntity = xmEntityList.get(xmEntityList.size() - 1);
        assertThat(testXmEntity.getName()).isEqualTo(DEFAULT_NAME);
    }

    @Test
    public void checkUpdateDateIsRequiredInDb() {

        // set the field null
        when(startUpdateDateGenerationStrategy.generateUpdateDate()).thenReturn(null);

        // Create the XmEntity.
        xmEntityService.save(xmEntityIncoming);

        try {
            xmEntityRepository.flush();
            fail("DataIntegrityViolationException exception was expected!");
        } catch (DataIntegrityViolationException e) {
            assertThat(e.getMostSpecificCause().getMessage())
                .containsIgnoringCase("NULL not allowed for column \"UPDATE_DATE\"");
        }

    }

    @Test
    public void checkStartDateIsNotRequired() throws Exception {
        int databaseSizeBeforeTest = xmEntityRepository.findAll().size();
        // set the field null
        xmEntityIncoming.setStartDate(null);

        // Create the XmEntity.

        restXmEntityMockMvc.perform(post("/api/xm-entities")
                                        .contentType(TestUtil.APPLICATION_JSON_UTF8)
                                        .content(TestUtil.convertObjectToJsonBytes(xmEntityIncoming)))
                           .andExpect(status().isCreated())
                           .andExpect(jsonPath("$.startDate").value(MOCKED_START_DATE.toString()))
                           .andExpect(jsonPath("$.updateDate").value(MOCKED_UPDATE_DATE.toString()))
        ;

        List<XmEntity> xmEntityList = xmEntityRepository.findAll();
        assertThat(xmEntityList).hasSize(databaseSizeBeforeTest + 1);
    }

    @Test
    public void checkUpdateDateIsNotRequired() throws Exception {
        int databaseSizeBeforeTest = xmEntityRepository.findAll().size();
        // set the field null
        xmEntityIncoming.setUpdateDate(null);

        // Create the XmEntity.

        restXmEntityMockMvc.perform(post("/api/xm-entities")
                                        .contentType(TestUtil.APPLICATION_JSON_UTF8)
                                        .content(TestUtil.convertObjectToJsonBytes(xmEntityIncoming)))
                           .andExpect(status().isCreated())
                           .andExpect(jsonPath("$.startDate").value(MOCKED_START_DATE.toString()))
                           .andExpect(jsonPath("$.updateDate").value(MOCKED_UPDATE_DATE.toString()))
        ;

        List<XmEntity> xmEntityList = xmEntityRepository.findAll();
        assertThat(xmEntityList).hasSize(databaseSizeBeforeTest + 1);
    }

    @Test
    public void testNoCycleJson() throws Exception {
        XmEntity target = new XmEntity().typeKey("TARGET");
        XmEntity source = new XmEntity().typeKey("SOURCE");
        source.targets(Collections.singleton(
            new Link().typeKey("LINK1")
                      .source(source)
                      .target(target)
        )).setId(1L);
        target.targets(Collections.singleton(
            new Link().typeKey("LINK2")
                      .source(target)
                      .target(source)
        )).setId(2L);
        String targetJson = jacksonMessageConverter.getObjectMapper().writeValueAsString(target);
        log.info("Target JSON {}", targetJson);
        String sourceJson = jacksonMessageConverter.getObjectMapper().writeValueAsString(source);
        log.info("Source JSON {}", sourceJson);
        assertEquals(Integer.valueOf(1), JsonPath.read(targetJson, "$.targets[0].target.id"));
        assertEquals(Integer.valueOf(2), JsonPath.read(targetJson, "$.id"));
        assertEquals(Integer.valueOf(2), JsonPath.read(sourceJson, "$.targets[0].target.id"));
        assertEquals(Integer.valueOf(1), JsonPath.read(sourceJson, "$.id"));
    }

    @Test
    public void testNoCycleErrorOnSave() throws Exception {
        XmEntity target = xmEntityRepository.save(createEntity());
        XmEntity source = xmEntityRepository.save(createEntity());
        source.getTargets().clear();
        source.getTargets().add(new Link().typeKey("LINK1")
                                          .source(source)
                                          .target(target));

        target.getTargets().clear();
        target.getTargets().add(new Link().typeKey("LINK2")
                                          .source(target)
                                          .target(source));

        String targetJson = jacksonMessageConverter.getObjectMapper().writeValueAsString(target);
        log.info("Target JSON {}", targetJson);
        String sourceJson = jacksonMessageConverter.getObjectMapper().writeValueAsString(source);
        log.info("Source JSON {}", sourceJson);
        restXmEntityMockMvc.perform(put("/api/xm-entities")
                                        .contentType(TestUtil.APPLICATION_JSON_UTF8)
                                        .content(targetJson.getBytes()))
                           .andDo(r -> log.info(r.getResponse().getContentAsString()))
                           .andExpect(status().is2xxSuccessful());

        restXmEntityMockMvc.perform(put("/api/xm-entities")
                                        .contentType(TestUtil.APPLICATION_JSON_UTF8)
                                        .content(sourceJson.getBytes()))
                           .andDo(r -> log.info(r.getResponse().getContentAsString()))
                           .andExpect(status().is2xxSuccessful());
    }

    @Test
    public void testAttachmentStartDate() throws Exception {

        Attachment attachment = new Attachment().typeKey("A").name("1");
        XmEntity entity = new XmEntity().name(" ").key(randomUUID()).typeKey("TEST_DELETE").attachments(asSet(
            attachment
        ));

        MutableObject<Instant> startDate = new MutableObject<>();

        MutableObject<XmEntity> entityHolder = new MutableObject<>();

        byte[] content = TestUtil.convertObjectToJsonBytes(entity);
        restXmEntityMockMvc.perform(post("/api/xm-entities")
                                        .contentType(TestUtil.APPLICATION_JSON_UTF8)
                                        .content(content))
                           .andDo(r -> entityHolder.setValue(readValue(r)))
                           .andDo(r -> log.info(r.getResponse().getContentAsString()))
                           .andExpect(status().is2xxSuccessful());

        Long id = entityHolder.getValue().getId();
        restXmEntityMockMvc.perform(get("/api/xm-entities/" + id))
                           .andDo(r -> startDate.setValue(readValue(r).getAttachments()
                                                                      .iterator()
                                                                      .next()
                                                                      .getStartDate()))
                           .andDo(r -> log.info(r.getResponse().getContentAsString()))
                           .andExpect(status().is2xxSuccessful());

        assertNotNull(startDate.getValue());

        content = TestUtil.convertObjectToJsonBytes(entityHolder.getValue());
        restXmEntityMockMvc.perform(put("/api/xm-entities")
                                        .contentType(TestUtil.APPLICATION_JSON_UTF8)
                                        .content(content))
                           .andDo(r -> log.info(r.getResponse().getContentAsString()))
                           .andExpect(status().is2xxSuccessful());

        restXmEntityMockMvc.perform(get("/api/xm-entities/" + id))
                           .andDo(r -> log.info(r.getResponse().getContentAsString()))
                           .andDo(r -> assertEquals(startDate.getValue(),
                                                    readValue(r).getAttachments().iterator().next().getStartDate()))
                           .andExpect(status().is2xxSuccessful());
    }

    @Test
    @WithMockUser(authorities = "SUPER-ADMIN")
    public void testExportCsvFile() throws Exception {
        createEntityComplexPersisted(em);
        performGet("/api/xm-entities/export?typeKey=ACCOUNT&fileFormat=csv").andExpect(
            status().isOk()).andExpect(
            content().contentType(MediaType.parseMediaType("text/csv")));
    }

    @Test
    @WithMockUser(authorities = "SUPER-ADMIN")
    public void testExportExcelFile() throws Exception {
        createEntityComplexPersisted(em);
        performGet("/api/xm-entities/export?typeKey=ACCOUNT&fileFormat=xlsx")
            .andExpect(status().isOk())
            .andExpect(content()
                           .contentType(MediaType
                                            .parseMediaType(
                                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")));
    }

    @SneakyThrows
    private XmEntity readValue(MvcResult r) {
        return jacksonMessageConverter.getObjectMapper()
                                      .readValue(r.getResponse().getContentAsString(), XmEntity.class);
    }

    @Test
    public void testJsonWithTwoPojoAndSameId() throws Exception {
        int databaseSizeBeforeCreate = xmEntityRepository.findAll().size();
        XmEntity target = xmEntityService.save(createEntity());

        XmEntity entity = xmEntityIncoming;
        entity.getTargets().add(new Link()
                                    .typeKey(DEFAULT_LN_TARGET_KEY)
                                    .name(DEFAULT_LN_TARGET_NAME)
                                    .startDate(DEFAULT_LN_TARGET_START_DATE)
                                    .target(target)
        );
        entity.getTargets().add(new Link()
                                    .typeKey(DEFAULT_LN_TARGET_KEY)
                                    .name(DEFAULT_LN_TARGET_NAME)
                                    .startDate(DEFAULT_LN_TARGET_START_DATE)
                                    .target(target)
        );

        performPost("/api/xm-entities", entity)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.key").value(DEFAULT_KEY))
            .andReturn();

        validateEntityInDB(databaseSizeBeforeCreate + 2);
    }

    @Test
    @SneakyThrows
    public void testSaveNewEntityWithLinkToExistsEntity() {
        XmEntity entity = createEntity();
        entity.setTypeKey("TEST_SAVE_WITH_LINK_EXISTS_ENTITY");
        entity.setData(null);
        entity.setStateKey(null);
        Long id = xmEntityService.save(entity).getId();

        String sourceLinkRequest = IOUtils.toString(
            requireNonNull(this.getClass()
                               .getClassLoader()
                               .getResourceAsStream("testrequests/newEntityWithLinkToExistsEntity.json")),
            "UTF-8"
        );

        restXmEntityMockMvc.perform(post("/api/xm-entities")
                                        .contentType(TestUtil.APPLICATION_JSON_UTF8)
                                        .content(String.format(sourceLinkRequest, id)))
                           .andDo(this::printMvcResult)
                           .andExpect(status().isCreated());

        em.flush();
        em.refresh(entity);

        assertEquals(1, entity.getTargets().size());
        Link link = entity.getTargets().iterator().next();
        assertEquals(link.getName(), "some link name");
        assertEquals(link.getTypeKey(), "TEST_SAVE_WITH_LINK_LINK_KEY");
        assertEquals(link.getTarget().getName(), DEFAULT_NAME);
        assertEquals(link.getTarget().getKey(), DEFAULT_KEY);
        assertEquals(link.getTarget().getTypeKey(), "TEST_SAVE_WITH_LINK_NEW_ENTITY");

    }

    @Test
    @SneakyThrows
    public void doubleSaveUniqueField() {

        XmEntity entity = new XmEntity().typeKey("TEST_UNIQUE_FIELD").key(randomUUID())
                                        .name("name").startDate(now()).updateDate(now()).data(of(
                "uniqueField", "value",
                "uniqueField2", "value2"
            ));

        MvcResult result = performPost("/api/xm-entities", entity)
            .andExpect(status().isCreated())
            .andReturn();

        String contentAsString = result.getResponse().getContentAsString();

        int id = JsonPath.read(contentAsString, "$.id");
        Integer version = JsonPath.read(contentAsString, "$.version");

        entity.setId((long) id);
        entity.setVersion(version);

        em.flush();

        performPut(entity)
            .andExpect(status().is2xxSuccessful())
            .andReturn();

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
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
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
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
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
    @Transactional
    @SneakyThrows
    public void testGetLinkTargetsDefaultSerialization() {

        String tgtTypeKey = "ACCOUNT.USER";
        String tgtCreatedBy = "admin";

        XmEntity source = xmEntityRepository.saveAndFlush(createComplexEntityPersistable().typeKey("ACCOUNT.ADMIN"));
        XmEntity target1 = xmEntityRepository.saveAndFlush(createComplexEntityPersistable().typeKey(tgtTypeKey)
                                                                                           .createdBy(tgtCreatedBy));
        XmEntity target2 = xmEntityRepository.saveAndFlush(createComplexEntityPersistable().typeKey(tgtTypeKey)
                                                                                           .createdBy(tgtCreatedBy));

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
                                    .target(target1)
                                    .source(source));

        source.getTargets().add(new Link()
                                    .typeKey(lnTypekey)
                                    .name(lnName)
                                    .description(defDescription)
                                    .startDate(lnStartDate)
                                    .endDate(lnEndDate)
                                    .target(target2)
                                    .source(source)
        );

        xmEntityRepository.saveAndFlush(source);

        assertNotNull(source.getId());
        assertNotNull(target1.getId());
        assertNotNull(target2.getId());
        Integer[] linIds = source.getTargets().stream()
                                 .map(Link::getId)
                                 .map(Long::intValue)
                                 .toArray(Integer[]::new);

        int srcId = source.getId().intValue();
        int target1Id = target1.getId().intValue();
        int target2Id = target2.getId().intValue();

        String tgtStartDate = DEFAULT_START_DATE.toString();
        String tgtUpdateDate = DEFAULT_UPDATE_DATE.toString();
        String tgtEndDate = DEFAULT_END_DATE.toString();

        performGet("/api/xm-entities/{id}/links/targets?typeKey={typeKey}", srcId, tgtTypeKey)
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$.[*].id").value(containsInAnyOrder(linIds)))
            .andExpect(jsonPath("$.[*].typeKey", containsInAnyOrder(two(lnTypekey))))
            .andExpect(jsonPath("$.[*].name", containsInAnyOrder(two(lnName))))
            .andExpect(jsonPath("$.[*].description", containsInAnyOrder(two(defDescription))))
            .andExpect(jsonPath("$.[*].startDate", containsInAnyOrder(two(lnStartDate.toString()))))
            .andExpect(jsonPath("$.[*].endDate", containsInAnyOrder(two(lnEndDate.toString()))))
            .andExpect(jsonPath("$.[*].source", containsInAnyOrder(two(srcId))))

            .andExpect(jsonPath("$.[*].target").exists())
            .andExpect(jsonPath("$.[*].target.id").value(containsInAnyOrder(target1Id, target2Id)))
            .andExpect(jsonPath("$.[*].target.key", containsInAnyOrder(two(DEFAULT_KEY))))
            .andExpect(jsonPath("$.[*].target.typeKey", containsInAnyOrder(two(tgtTypeKey))))
            .andExpect(jsonPath("$.[*].target.stateKey", containsInAnyOrder(two(DEFAULT_STATE_KEY))))
            .andExpect(jsonPath("$.[*].target.name", containsInAnyOrder(two(DEFAULT_NAME))))
            .andExpect(jsonPath("$.[*].target.startDate", containsInAnyOrder(two(tgtStartDate))))
            .andExpect(jsonPath("$.[*].target.endDate", containsInAnyOrder(two(tgtEndDate))))
            .andExpect(jsonPath("$.[*].target.updateDate", containsInAnyOrder(two(tgtUpdateDate))))
            .andExpect(jsonPath("$.[*].target.description", containsInAnyOrder(two(defDescription))))
            .andExpect(jsonPath("$.[*].target.createdBy", containsInAnyOrder(two(tgtCreatedBy))))
            .andExpect(jsonPath("$.[*].target.removed", containsInAnyOrder(two(false))))
            .andExpect(jsonPath("$.[*].target.data").exists())
            .andExpect(jsonPath("$.[*].target.data.AAAAAAAAAA", containsInAnyOrder(two("BBBBBBBBBB"))))

            .andExpect(jsonPath("$.[*].target.avatarUrlRelative").doesNotExist())
            .andExpect(jsonPath("$.[*].target.avatarUrlFull").doesNotExist())
            .andExpect(jsonPath("$.[*].target.version").doesNotExist())
            .andExpect(jsonPath("$.[*].target.targets").doesNotExist())
            .andExpect(jsonPath("$.[*].target.sources").doesNotExist())
            .andExpect(jsonPath("$.[*].target.attachments").doesNotExist())
            .andExpect(jsonPath("$.[*].target.locations").doesNotExist())
            .andExpect(jsonPath("$.[*].target.tags").doesNotExist())
            .andExpect(jsonPath("$.[*].target.calendars").doesNotExist())
            .andExpect(jsonPath("$.[*].target.ratings").doesNotExist())
            .andExpect(jsonPath("$.[*].target.comments").doesNotExist())
            .andExpect(jsonPath("$.[*].target.votes").doesNotExist())
            .andExpect(jsonPath("$.[*].target.functionContexts").doesNotExist())
            .andExpect(jsonPath("$.[*].target.events").doesNotExist())
            .andExpect(jsonPath("$.[*].target.uniqueFields").doesNotExist())
        ;

    }

    @Test
    @Transactional
    @SneakyThrows
    public void testGetLinkTargetsFilteredSerialization() {

        String tgtTypeKey = "ACCOUNT.USER";
        String tgtCreatedBy = "admin";

        XmEntity source = xmEntityRepository.saveAndFlush(createComplexEntityPersistable().typeKey("ACCOUNT.ADMIN"));
        XmEntity target1 = xmEntityRepository.saveAndFlush(createComplexEntityPersistable().typeKey(tgtTypeKey)
                                                                                           .createdBy(tgtCreatedBy));
        XmEntity target2 = xmEntityRepository.saveAndFlush(createComplexEntityPersistable().typeKey(tgtTypeKey)
                                                                                           .createdBy(tgtCreatedBy));

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
                                    .target(target1)
                                    .source(source));

        source.getTargets().add(new Link()
                                    .typeKey(lnTypekey)
                                    .name(lnName)
                                    .description(defDescription)
                                    .startDate(lnStartDate)
                                    .endDate(lnEndDate)
                                    .target(target2)
                                    .source(source)
        );

        xmEntityRepository.saveAndFlush(source);

        assertNotNull(source.getId());
        assertNotNull(target1.getId());
        assertNotNull(target2.getId());
        Integer[] linIds = source.getTargets().stream()
                                 .map(Link::getId)
                                 .map(Long::intValue)
                                 .toArray(Integer[]::new);

        int srcId = source.getId().intValue();

        String fields = "id,typeKey,name,source,target.attachments.contentUrl,target.attachments.data";

        performGet("/api/xm-entities/{id}/links/targets?typeKey={typeKey}&fields={fields}", srcId, tgtTypeKey, fields)
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$.[*].id").value(containsInAnyOrder(linIds)))
            .andExpect(jsonPath("$.[*].typeKey", containsInAnyOrder(two(lnTypekey))))
            .andExpect(jsonPath("$.[*].name", containsInAnyOrder(two(lnName))))
            .andExpect(jsonPath("$.[*].description").doesNotExist())
            .andExpect(jsonPath("$.[*].startDate").doesNotExist())
            .andExpect(jsonPath("$.[*].endDate").doesNotExist())
            .andExpect(jsonPath("$.[*].source", containsInAnyOrder(two(srcId))))

            .andExpect(jsonPath("$.[*].target").exists())
            .andExpect(jsonPath("$.[*].target.id").doesNotExist())
            .andExpect(jsonPath("$.[*].target.key").doesNotExist())
            .andExpect(jsonPath("$.[*].target.typeKey").doesNotExist())
            .andExpect(jsonPath("$.[*].target.stateKey").doesNotExist())
            .andExpect(jsonPath("$.[*].target.name").doesNotExist())
            .andExpect(jsonPath("$.[*].target.startDate").doesNotExist())
            .andExpect(jsonPath("$.[*].target.endDate").doesNotExist())
            .andExpect(jsonPath("$.[*].target.updateDate").doesNotExist())
            .andExpect(jsonPath("$.[*].target.description").doesNotExist())
            .andExpect(jsonPath("$.[*].target.createdBy").doesNotExist())
            .andExpect(jsonPath("$.[*].target.removed").doesNotExist())
            .andExpect(jsonPath("$.[*].target.data").doesNotExist())
            .andExpect(jsonPath("$.[*].target.data.AAAAAAAAAA").doesNotExist())

            .andExpect(jsonPath("$.[*].target.avatarUrlRelative").doesNotExist())
            .andExpect(jsonPath("$.[*].target.avatarUrlFull").doesNotExist())
            .andExpect(jsonPath("$.[*].target.version").doesNotExist())
            .andExpect(jsonPath("$.[*].target.targets").doesNotExist())
            .andExpect(jsonPath("$.[*].target.sources").doesNotExist())
            .andExpect(jsonPath("$.[*].target.attachments").exists())
            .andExpect(jsonPath("$.[*].target.attachments.[*].contentUrl", containsInAnyOrder(two("content url"))))
            .andExpect(jsonPath("$.[*].target.locations").doesNotExist())
            .andExpect(jsonPath("$.[*].target.tags").doesNotExist())
            .andExpect(jsonPath("$.[*].target.calendars").doesNotExist())
            .andExpect(jsonPath("$.[*].target.ratings").doesNotExist())
            .andExpect(jsonPath("$.[*].target.comments").doesNotExist())
            .andExpect(jsonPath("$.[*].target.votes").doesNotExist())
            .andExpect(jsonPath("$.[*].target.functionContexts").doesNotExist())
            .andExpect(jsonPath("$.[*].target.events").doesNotExist())
            .andExpect(jsonPath("$.[*].target.uniqueFields").doesNotExist())
        ;

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

        assertNotNull(source.getId());
        Integer srcId = source.getId().intValue();
        String tgtStartDate = DEFAULT_START_DATE.toString();
        String tgtUpdateDate = DEFAULT_UPDATE_DATE.toString();
        String tgtEndDate = DEFAULT_END_DATE.toString();

        performGet("/api/_search/xm-entities?query=id:{id}", source.getId())
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
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
            .andExpect(jsonPath("$.[*].targets.[*].target.removed").doesNotExist())
            .andExpect(jsonPath("$.[*].targets.[*].target.data").exists())
            .andExpect(jsonPath("$.[*].targets.[*].target.data.AAAAAAAAAA", containsInAnyOrder("BBBBBBBBBB")))

            .andExpect(jsonPath("$.[*].targets.[*].target.calendars").exists())
            .andExpect(jsonPath("$.[*].targets.[*].target.ratings").exists())
            .andExpect(jsonPath("$.[*].targets.[*].target.comments").exists())
            .andExpect(jsonPath("$.[*].targets.[*].target.attachments.[*].id").exists())
            .andExpect(jsonPath("$.[*].targets.[*].target.locations.[*].id").exists())
            .andExpect(jsonPath("$.[*].targets.[*].target.tags.[*].id").exists())

            .andExpect(jsonPath("$.[*].targets.[*].target.targets").doesNotExist())
            .andExpect(jsonPath("$.[*].targets.[*].target.sources").doesNotExist())
        ;

    }

    private XmEntity createComplexEntityPersistable() {

        XmEntity entity = createEntityComplexIncoming();

        entity.getAttachments().forEach(inner -> inner.setXmEntity(entity));
        entity.getTags().forEach(inner -> inner.setXmEntity(entity));
        entity.getLocations().forEach(inner -> inner.setXmEntity(entity));

        return entity;

    }

    private static Object[] two(Object single) {
        return new Object[]{single, single};
    }

}
