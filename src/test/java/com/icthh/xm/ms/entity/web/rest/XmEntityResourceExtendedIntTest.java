package com.icthh.xm.ms.entity.web.rest;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.collect.ImmutableMap;
import com.icthh.xm.commons.config.client.service.TenantConfigService;
import com.icthh.xm.commons.i18n.error.web.ExceptionTranslator;
import com.icthh.xm.commons.security.XmAuthenticationContext;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.ms.entity.EntityApp;
import com.icthh.xm.ms.entity.config.ApplicationProperties;
import com.icthh.xm.ms.entity.config.Constants;
import com.icthh.xm.ms.entity.config.InternalTransactionService;
import com.icthh.xm.ms.entity.config.LepConfiguration;
import com.icthh.xm.ms.entity.config.SecurityBeanOverrideConfiguration;
import com.icthh.xm.ms.entity.config.tenant.WebappTenantOverrideConfiguration;
import com.icthh.xm.ms.entity.domain.*;
import com.icthh.xm.ms.entity.domain.ext.IdOrKey;
import com.icthh.xm.ms.entity.repository.XmEntityPermittedRepository;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
import com.icthh.xm.ms.entity.repository.kafka.ProfileEventProducer;
import com.icthh.xm.ms.entity.repository.search.XmEntityPermittedSearchRepository;
import com.icthh.xm.ms.entity.repository.search.XmEntitySearchRepository;
import com.icthh.xm.ms.entity.service.*;
import com.icthh.xm.ms.entity.service.impl.StartUpdateDateGenerationStrategy;
import com.icthh.xm.ms.entity.service.impl.XmEntityServiceImpl;
import com.jayway.jsonpath.JsonPath;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Validator;

import javax.persistence.EntityManager;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.hamcrest.Matchers.*;
import static org.hibernate.validator.internal.util.CollectionHelper.asSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Extension Test class for the XmEntityResource REST controller. Contains additional test apart from Jhipster generated
 * logic
 *
 * @see XmEntityResource
 */
@Slf4j
@Transactional
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
    EntityApp.class,
    SecurityBeanOverrideConfiguration.class,
    WebappTenantOverrideConfiguration.class,
    LepConfiguration.class
})
public class XmEntityResourceExtendedIntTest {

    private static final String DEFAULT_KEY = "AAAAAAAAAA";

    private static final String DEFAULT_TYPE_KEY = "ACCOUNT.ADMIN";
    private static final String UPDATED_TYPE_KEY = "ACCOUNT.USER";

    private static final String DEFAULT_STATE_KEY = "STATE2";

    private static final String DEFAULT_NAME = "AAAAAAAAAA";

    private static final Instant DEFAULT_START_DATE = Instant.ofEpochMilli(0L);

    private static final Instant DEFAULT_UPDATE_DATE = Instant.ofEpochMilli(0L);

    private static final Instant DEFAULT_END_DATE = Instant.ofEpochMilli(0L);

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

    @Autowired
    private ApplicationProperties applicationProperties;

    @Autowired
    private CalendarResource calendarResource;

    @Autowired
    private EventResource eventResource;

    @Autowired
    private XmEntityResource xmEntityResource;

    @Autowired
    private XmEntityRepository xmEntityRepository;

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
    private ElasticsearchTemplate elasticsearchTemplate;

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
    TenantConfigService tenantConfigService;

    @Autowired
    XmEntityPermittedSearchRepository xmEntityPermittedSearchRepository;

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

        XmEntityServiceImpl xmEntityService = new XmEntityServiceImpl(xmEntitySpecService,
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
                                                                      tenantConfigService);

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
                                                  .setMessageConverters(jacksonMessageConverter).build();

        xmEntityIncoming = createEntityComplexIncoming();

    }

    @After
    @Override
    public void finalize() {
        xmEntityRepository.deleteAll();
        lepManager.endThreadContext();
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
    }

    /**
     * Create an entity for this test.
     * <p>
     * This is a static method, as tests for other entities might also need it, if they test an entity which requires
     * the current entity.
     */
    public static XmEntity createEntity() {
        XmEntity xmEntity = new XmEntity()
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

        return xmEntity;
    }

    /**
     * Creates incoming Entity as from HTTP request. Potentially cam be moved to DTO
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
     * @param url     URL template
     * @param content Content object
     */
    private ResultActions performPut(String url, Object content) throws Exception {

        String json = new String(convertObjectToJsonBytesByFields(content));
        log.info("perform PUT: {} with content: {}", url, json);
        return restXmEntityMockMvc.perform(put(url)
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

    private static XmEntity convertJsonToOnject(String json) throws IOException {
        ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        JavaTimeModule module = new JavaTimeModule();
        mapper.registerModule(module);
        return mapper.readValue(json, XmEntity.class);
    }

    @Test
    public void createXmEntityWithTagsAttachmentsLocations() throws Exception {
        int databaseSizeBeforeCreate = xmEntityRepository.findAll().size();

        // Create the XmEntity with tag, attachment and location
        MvcResult result = performPost("/api/xm-entities", xmEntityIncoming)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.key").value(DEFAULT_KEY))
            .andReturn();

        Integer id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");

        List<XmEntity> xmEntityList = validateEntityInDB(databaseSizeBeforeCreate + 1);
        XmEntity testXmEntity = xmEntityList.get(xmEntityList.size() - 1);

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
            .andExpect(jsonPath("$.locations[0].xmEntity").value(id))

        // TODO - check not implemented XmEntity references. Replace with correct assertions after implementation
        //.andExpect(jsonPath("$.calendars.length()").value(0))

        // FIXME - content is returned in TEST mode besides LAZY fetchtype. Need to investigate.
        // .andExpect(jsonPath("$.attachments[0].content.id").value(notNullValue()))

        ;

        // Validate the XmEntity in Elasticsearch
        XmEntity xmEntityEs = xmEntitySearchRepository.findOne(valueOf(id.toString()));
        assertThat(xmEntityEs).isEqualToIgnoringGivenFields(testXmEntity, "avatarUrlRelative");

    }

    @Test
    public void createXmEntityWithLinks() throws Exception {

        XmEntity presaved = xmEntityService.save(createEntity());

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

        // Validate the XmEntity in Elasticsearch
        XmEntity xmEntityEs = xmEntitySearchRepository.findOne(valueOf(id.toString()));
        assertThat(xmEntityEs).isEqualToIgnoringGivenFields(testXmEntity, "avatarUrlRelative");

    }

    @Test
    public void createXmEntityWithSourceLinks() throws Exception {

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
        XmEntity testXmEntity = xmEntityList.get(xmEntityList.size() - 1);

        presaved = xmEntityService.findOne(IdOrKey.of(presaved.getId()));

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

        // Validate the XmEntity in Elasticsearch
        XmEntity xmEntityEs = xmEntitySearchRepository.findOne(valueOf(id.toString()));
        // TODO - may be compare avatarUrl too ?
        assertThat(xmEntityEs).isEqualToIgnoringGivenFields(testXmEntity, "sources", "avatarUrlRelative");
    }

    @Test
    @WithMockUser(authorities = "SUPER-ADMIN")
    public void getXmEntitySourcesLinksByTypeKey() throws Exception {

        XmEntity target = xmEntityService.save(createEntity().name("TARGET"));
        XmEntity source1 = xmEntityService.save(createEntity().name("SOURCE1"));
        XmEntity source2 = xmEntityService.save(createEntity()).name("SOURCE2");

        // should not appear in output
        XmEntity targetOther = xmEntityService.save(createEntity().name("TARGET_OTHER"));

        source1.getTargets().clear();
        source1.getTargets().add(new Link().typeKey("LINK1").source(source1).target(target));
        source1.getTargets().add(new Link().typeKey("LINK1").source(source1).target(targetOther));

        source2.getTargets().clear();
        source2.getTargets().add(new Link().typeKey("LINK1").source(source2).target(target));
        source2.getTargets().add(new Link().typeKey("LINK2").source(source2).target(target));

        Integer targetId = target.getId().intValue();
        Integer srcId1 = source1.getId().intValue();
        Integer srcId2 = source2.getId().intValue();

        // Get the xmEntityPersisted with tag by ID
        performGet("/api/v2/xm-entities/{id}/links/sources?typeKeys={typeKeys}", targetId, "LINK1")
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$.[*].typeKey").value(hasItem("LINK1")))
            .andExpect(jsonPath("$.[*].target").value(containsInAnyOrder(targetId, targetId)))
            .andExpect(jsonPath("$.[*].source.id").value(containsInAnyOrder(srcId1, srcId2)))
            .andExpect(jsonPath("$.[*].source.name").value(containsInAnyOrder("SOURCE1", "SOURCE2")))
            .andExpect(jsonPath("$.[*].source.targets").doesNotExist())
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

        XmEntity testXmEntity = xmEntityList.get(xmEntityList.size() - 1);
        assertThat(testXmEntity.getData()).isEqualTo(LARGE_DATA);

        // Validate the XmEntity in Elasticsearch
        XmEntity xmEntityEs = xmEntitySearchRepository.findOne(testXmEntity.getId());
        assertThat(xmEntityEs).isEqualToIgnoringGivenFields(testXmEntity, "avatarUrlRelative");
        assertThat(xmEntityEs.getData()).isEqualTo(LARGE_DATA);
    }

    @Test
    public void checkJsonShemeDateTypeProperties() throws Exception {

        int databaseSizeBeforeTest = xmEntityRepository.findAll().size();

        XmEntity entity = xmEntityIncoming;

        entity.setData(of("numberProperties", "5"));

        performPut("/api/xm-entities", entity)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("error.validation"));

        // Validate the XmEntity in the database
        validateEntityInDB(databaseSizeBeforeTest);

        entity.setData(of("numberProperties", "testse"));

        performPut("/api/xm-entities", entity)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("error.validation"));

        // Validate the XmEntity in the database
        validateEntityInDB(databaseSizeBeforeTest);

        entity.setData(of("numberProperties", Boolean.FALSE));

        performPut("/api/xm-entities", entity)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("error.validation"));

        // Validate the XmEntity in the database
        validateEntityInDB(databaseSizeBeforeTest);

    }

    @Test
    public void checkJsonShemeDateIsRequired() throws Exception {
        int databaseSizeBeforeTest = xmEntityRepository.findAll().size();

        xmEntityIncoming.setData(emptyMap());

        performPut("/api/xm-entities", xmEntityIncoming)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("error.validation"));

        validateEntityInDB(databaseSizeBeforeTest);
    }

    @Test
    @WithMockUser(authorities = "SUPER-ADMIN")
    public void getAllXmEntitiesByTypeKey() throws Exception {

        XmEntity entity = createEntityComplexPersisted(em);

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

        XmEntity entity = createEntityComplexPersisted(em);

        // Get all the xmEntityList
        performGet("/api/xm-entities?sort=id,desc&typeKey=PRICE")
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(everyItem(nullValue())));
    }

    @Test
    @WithMockUser(authorities = "SUPER-ADMIN")
    public void testSearchByTypeKeyAndQuery() throws Exception {

        List<Long> ids = prepareSearch();

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

        ids.forEach(xmEntityService::delete);
    }

    @Test
    @WithMockUser(authorities = "SUPER-ADMIN")
    public void testSearchByTypeKeyAndTemplate() throws Exception {

        List<Long> ids = prepareSearch();

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

        ids.forEach(xmEntityService::delete);
    }

    private List<Long> prepareSearch() {
        List<Long> ids = new ArrayList<>();

        transactionService.inNestedTransaction(() -> {
            // FIXME - fails if run test in Idea. But its needed for test running from console. need fix.
            try {
                xmEntitySearchRepository.deleteAll();
            } catch (Exception e) {
                log.warn("Suppress index deletion exception in tenant context: {}", String.valueOf(e));
            }

            // Initialize the database
            ids.add(xmEntityService.save(createEntityComplexIncoming().typeKey(DEFAULT_TYPE_KEY).stateKey(DEFAULT_STATE_KEY)).getId());
            ids.add(xmEntityService.save(createEntityComplexIncoming().typeKey(UPDATED_TYPE_KEY).description(UNIQ_DESCRIPTION).stateKey(DEFAULT_STATE_KEY)).getId());
            ids.add(xmEntityService.save(createEntityComplexIncoming().typeKey(SEARCH_TEST_KEY).stateKey(null).data(null).attachments(emptySet()).tags(emptySet()).locations(emptySet())).getId());

            return null;
        }, this::setup);

        return ids;
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

        MvcResult resultSaveEvent = performPost("/api/events", event)
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
    public void checkUpdateDateIsRequiredInDb() throws Exception {

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

        xmEntityService.delete(xmEntityIncoming.getId());
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
                                        .content(TestUtil.convertObjectToJsonBytes(target)))
                           .andDo(r -> log.info(r.getResponse().getContentAsString()))
                           .andExpect(status().is2xxSuccessful());

        restXmEntityMockMvc.perform(put("/api/xm-entities")
                                        .contentType(TestUtil.APPLICATION_JSON_UTF8)
                                        .content(TestUtil.convertObjectToJsonBytes(source)))
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

        MvcResult result = performPost("/api/xm-entities", entity)
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
            this.getClass().getClassLoader().getResourceAsStream("testrequests/newEntityWithLinkToExistsEntity.json"),
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

        performPut("/api/xm-entities", entity)
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        xmEntityService.delete(entity.getId());
    }

    @Test
    public void manageXmEntityAvatarUrl() throws Exception {

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

        XmEntity toUpdate = convertJsonToOnject(result.getResponse().getContentAsString());
        toUpdate.setAvatarUrl(DEFAULT_AVATAR_URL_PREFIX + "bbbbb.jpg");
        toUpdate.setName("new_name1");

        performPut("/api/xm-entities", toUpdate)
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

        // Validate the XmEntity in Elasticsearch
        XmEntity xmEntityEs = xmEntitySearchRepository.findOne(valueOf(id.toString()));
        XmEntity testXmEntity = convertJsonToOnject(result.getResponse().getContentAsString());
        assertThat(xmEntityEs).isEqualToIgnoringGivenFields(testXmEntity, "version", "avatarUrlRelative", "avatarUrlFull");

        // TODO: fix: old avatarUrl inside elasticsearch after save
//        assertEquals(testXmEntity.getAvatarUrl(), xmEntityEs.getAvatarUrl());
//        assertEquals(testXmEntity.getAvatarUrlRelative(), xmEntityEs.getAvatarUrlRelative());

    }



}
