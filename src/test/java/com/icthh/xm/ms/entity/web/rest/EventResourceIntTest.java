package com.icthh.xm.ms.entity.web.rest;

import com.google.common.collect.ImmutableMap;
import com.icthh.xm.commons.i18n.error.web.ExceptionTranslator;
import com.icthh.xm.commons.permission.repository.PermittedRepository;
import com.icthh.xm.commons.security.XmAuthenticationContext;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.ms.entity.AbstractSpringBootTest;
import com.icthh.xm.ms.entity.domain.Calendar;
import com.icthh.xm.ms.entity.domain.Event;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.spec.CalendarSpec;
import com.icthh.xm.ms.entity.repository.CalendarRepository;
import com.icthh.xm.ms.entity.repository.EventRepository;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
import com.icthh.xm.ms.entity.repository.search.PermittedSearchRepository;
import com.icthh.xm.ms.entity.service.CalendarService;
import com.icthh.xm.ms.entity.service.EventService;
import com.icthh.xm.ms.entity.service.XmEntityService;
import com.icthh.xm.ms.entity.service.XmEntitySpecService;
import com.icthh.xm.ms.entity.service.impl.StartUpdateDateGenerationStrategy;
import com.icthh.xm.ms.entity.service.query.EventQueryService;
import jakarta.persistence.EntityManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Validator;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_AUTH_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test class for the EventResource REST controller.
 *
 * @see EventResource
 */
@WithMockUser(authorities = {"SUPER-ADMIN"})
public class EventResourceIntTest extends AbstractSpringBootTest {

    private static final String DEFAULT_TYPE_KEY = "TEST_EVENT_TYPEKEY_1";
    private static final String UPDATED_TYPE_KEY = "TEST_EVENT_TYPEKEY_2";

    private static final String DEFAULT_REPEAT_RULE_KEY = "AAAAAAAAAA";
    private static final String UPDATED_REPEAT_RULE_KEY = "BBBBBBBBBB";

    private static final String DEFAULT_TITLE = "AAAAAAAAAA";
    private static final String UPDATED_TITLE = "BBBBBBBBBB";

    private static final String DEFAULT_COLOR = "#000000";
    private static final String UPDATED_COLOR = "#FFFFFF";

    private static final String DEFAULT_DESCRIPTION = "AAAAAAAAAA";
    private static final String UPDATED_DESCRIPTION = "BBBBBBBBBB";

    private static final Instant DEFAULT_START_DATE = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_START_DATE = now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_END_DATE = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_END_DATE = now().truncatedTo(ChronoUnit.MILLIS);

    public static final String DEFAULT_XM_ENTITY_NAME = "name";
    private static final String DEFAULT_EVENT_DATA_REF_TYPE_KEY = "EVENT_DATA_REF_TYPE_KEY";

    private static final Map<String, Object> DEFAULT_EVENT_DATA_REF_DATA = ImmutableMap.<String, Object>builder()
        .put("AAAAAAAAAA", "AAAAAAAAAA").build();
    private static final Map<String, Object> UPDATED_EVENT_DATA_REF_DATA = ImmutableMap.<String, Object>builder()
        .put("AAAAAAAAAA", "BBBBBBBBBB").build();

    @Autowired
    private EventResource eventResource;

    @Autowired
    private EventRepository eventRepository;

    private EventService eventService;

    @Autowired
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Autowired
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @Autowired
    private Validator validator;

    @Autowired
    private EntityManager em;

    private MockMvc restEventMockMvc;

    private Event event;

    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Autowired
    private LepManager lepManager;

    @Autowired
    private XmEntityService xmEntityService;

    @Autowired
    private EventQueryService eventQueryService;

    @Autowired
    private PermittedRepository permittedRepository;

    @Autowired
    private XmEntityRepository xmEntityRepository;

    @Mock
    private XmAuthenticationContext context;

    @Mock
    private XmEntitySpecService xmEntitySpecService;

    @Autowired
    private CalendarRepository calendarRepository;

    @Autowired
    private StartUpdateDateGenerationStrategy startUpdateDateGenerationStrategy;

    private CalendarService calendarService;

    @BeforeTransaction
    public void beforeTransaction() {
        TenantContextUtils.setTenant(tenantContextHolder, "RESINTTEST");
    }

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        calendarService = new CalendarService(calendarRepository,
            permittedRepository,
            startUpdateDateGenerationStrategy,
            xmEntityRepository,
            eventQueryService,
            xmEntitySpecService);

        eventService = new EventService(xmEntityService,
            eventQueryService,
            eventRepository,
            permittedRepository,
            xmEntityRepository,
            calendarService);
        eventService.setSelf(eventService);

        EventResource eventResourceMock = new EventResource(eventService, eventResource);
        this.restEventMockMvc = MockMvcBuilders.standaloneSetup(eventResourceMock)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setValidator(validator)
            .setMessageConverters(jacksonMessageConverter).build();

        lepManager.beginThreadContext(ctx -> {
            ctx.setValue(THREAD_CONTEXT_KEY_TENANT_CONTEXT, tenantContextHolder.getContext());
            ctx.setValue(THREAD_CONTEXT_KEY_AUTH_CONTEXT, context);
        });
    }

    @After
    public void tearDown() {
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
    }

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Event createEntity(EntityManager em) {
        XmEntity assigned = new XmEntity()
            .typeKey("TARGET_ENTITY")
            .startDate(now())
            .updateDate(now())
            .name(DEFAULT_XM_ENTITY_NAME)
            .key(randomUUID());
        em.persist(assigned);
        XmEntity eventDataRef = new XmEntity()
            .typeKey(DEFAULT_EVENT_DATA_REF_TYPE_KEY)
            .startDate(now())
            .updateDate(now())
            .key(randomUUID())
            .name(DEFAULT_XM_ENTITY_NAME)
            .data(DEFAULT_EVENT_DATA_REF_DATA);
        em.persist(eventDataRef);
        return new Event()
            .typeKey(DEFAULT_TYPE_KEY)
            .repeatRuleKey(DEFAULT_REPEAT_RULE_KEY)
            .title(DEFAULT_TITLE)
            .description(DEFAULT_DESCRIPTION)
            .startDate(DEFAULT_START_DATE)
            .endDate(DEFAULT_END_DATE)
            .assigned(assigned)
            .color(DEFAULT_COLOR)
            .eventDataRef(eventDataRef);
    }

    @Before
    public void initTest() {
        event = createEntity(em);
    }

    @Test
    @Transactional
    public void createEvent() throws Exception {
        int databaseSizeBeforeCreate = eventRepository.findAll().size();

        // Create the Event
        restEventMockMvc.perform(post("/api/events")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(event)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.eventDataRef.typeKey").value(DEFAULT_EVENT_DATA_REF_TYPE_KEY))
            .andExpect(jsonPath("$.eventDataRef.data").value(is(DEFAULT_EVENT_DATA_REF_DATA)));

        // Validate the Event in the database
        List<Event> eventList = eventRepository.findAll();
        assertThat(eventList).hasSize(databaseSizeBeforeCreate + 1);
        Event testEvent = eventList.get(eventList.size() - 1);
        assertThat(testEvent.getTypeKey()).isEqualTo(DEFAULT_TYPE_KEY);
        assertThat(testEvent.getRepeatRuleKey()).isEqualTo(DEFAULT_REPEAT_RULE_KEY);
        assertThat(testEvent.getTitle()).isEqualTo(DEFAULT_TITLE);
        assertThat(testEvent.getColor()).isEqualTo(DEFAULT_COLOR);
        assertThat(testEvent.getDescription()).isEqualTo(DEFAULT_DESCRIPTION);
        assertThat(testEvent.getStartDate()).isEqualTo(DEFAULT_START_DATE);
        assertThat(testEvent.getEndDate()).isEqualTo(DEFAULT_END_DATE);
        assertThat(testEvent.getEventDataRef().getTypeKey()).isEqualTo(DEFAULT_EVENT_DATA_REF_TYPE_KEY);
        assertThat(testEvent.getEventDataRef().getData()).isEqualTo(DEFAULT_EVENT_DATA_REF_DATA);
    }

    @Test
    @Transactional
    public void updateEventReadOnlyCalendar() throws Exception {
        CalendarSpec calendarSpec = new CalendarSpec();
        calendarSpec.setReadonly(true);
        when(xmEntitySpecService.findCalendar(eq(XmEntityResourceIntTest.DEFAULT_TYPE_KEY),
                                              eq(CalendarResourceIntTest.DEFAULT_TYPE_KEY)))
            .thenReturn(Optional.of(calendarSpec));

        Calendar calendar = CalendarResourceIntTest.createEntity(em);
        em.persist(calendar);
        event.setCalendar(calendar);
        em.persist(event);

        em.detach(event);
        em.detach(event.getEventDataRef());
        event
            .typeKey(UPDATED_TYPE_KEY)
            .repeatRuleKey(UPDATED_REPEAT_RULE_KEY)
            .title(UPDATED_TITLE)
            .description(UPDATED_DESCRIPTION)
            .startDate(UPDATED_START_DATE)
            .endDate(UPDATED_END_DATE)
            .color(UPDATED_COLOR)
            .calendar(calendar)
            .getEventDataRef().setData(UPDATED_EVENT_DATA_REF_DATA);

        restEventMockMvc.perform(put("/api/events")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(event)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("error.read.only.calendar"))
            .andExpect(jsonPath("$.error_description").value(notNullValue()));
    }

    @Test
    @Transactional
    public void createEventReadOnlyCalendar() throws Exception {
        CalendarSpec calendarSpec = new CalendarSpec();
        calendarSpec.setReadonly(true);
        when(xmEntitySpecService.findCalendar(eq(XmEntityResourceIntTest.DEFAULT_TYPE_KEY),
                                              eq(CalendarResourceIntTest.DEFAULT_TYPE_KEY)))
            .thenReturn(Optional.of(calendarSpec));

        Calendar calendar = CalendarResourceIntTest.createEntity(em);
        em.persist(calendar);
        event.setCalendar(calendar);

        restEventMockMvc.perform(post("/api/events")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(event)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("error.read.only.calendar"))
            .andExpect(jsonPath("$.error_description").value(notNullValue()));
    }

    @Test
    @Transactional
    public void createEventWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = eventRepository.findAll().size();

        // Create the Event with an existing ID
        event.setId(1L);

        // An entity with an existing ID cannot be created, so this API call must fail
        restEventMockMvc.perform(post("/api/events")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(event)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("error.business.idexists"))
            .andExpect(jsonPath("$.error_description").value(notNullValue()))
        ;

        // Validate the Alice in the database
        List<Event> eventList = eventRepository.findAll();
        assertThat(eventList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    public void createEventWithNotExistingTypeKey() throws Exception {
        event.setTypeKey("NOT_EXISTED_TYPE_KEY");

        restEventMockMvc.perform(post("/api/events")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(event)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("error.validation"))
            .andExpect(jsonPath("$.error_description").value(notNullValue()))
            .andExpect(jsonPath("$.fieldErrors[*].objectName").value("event"))
            .andExpect(jsonPath("$.fieldErrors[*].field")
                .value(Event.class.getDeclaredField("typeKey").getName()))
            .andExpect(jsonPath("$.fieldErrors[*].message").value("EventDataTypeKey"))
            .andExpect(jsonPath("$.fieldErrors[*].description")
                .value("Event specification not found by key: " + event.getTypeKey()));
    }

    @Test
    @Transactional
    public void createEventWithoutDataRef() throws Exception {
        String typeKey = "EVENT_WITHOUT_DATA_REF";
        event.setTypeKey(typeKey);

        restEventMockMvc.perform(post("/api/events")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(event)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("error.validation"))
            .andExpect(jsonPath("$.error_description").value(notNullValue()))
            .andExpect(jsonPath("$.fieldErrors[*].objectName").value("event"))
            .andExpect(jsonPath("$.fieldErrors[*].field")
                .value(Event.class.getDeclaredField("typeKey").getName()))
            .andExpect(jsonPath("$.fieldErrors[*].message").value("EventDataTypeKey"))
            .andExpect(jsonPath("$.fieldErrors[*].description").value("Data type key not configured for Event with type key: " + typeKey));
    }

    @Test
    @Transactional
    public void createEventWithNotExistedDataRef() throws Exception {
        String typeKey = "EVENT_WITH_NOT_EXISTED_DATA_REF";
        String eventDataRefTypeKey = "NOT_EXISTED_DATA_REF";
        event.setTypeKey(typeKey);
        event.getEventDataRef().setTypeKey(eventDataRefTypeKey);

        restEventMockMvc.perform(post("/api/events")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(event)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("error.validation"))
            .andExpect(jsonPath("$.error_description").value(notNullValue()))
            .andExpect(jsonPath("$.fieldErrors[*].objectName").value("event"))
            .andExpect(jsonPath("$.fieldErrors[*].field")
                .value(Event.class.getDeclaredField("typeKey").getName()))
            .andExpect(jsonPath("$.fieldErrors[*].message").value("EventDataTypeKey"))
            .andExpect(jsonPath("$.fieldErrors[*].description")
                .value("Type specification not found by key: " + eventDataRefTypeKey));
    }


    @Test
    @Transactional
    public void createEventWithNotMatchingDataRef() throws Exception {
        event.getEventDataRef().setTypeKey("NOT_MATCHING_EVENT_DATA_REF_TYPE_KEY");

        restEventMockMvc.perform(post("/api/events")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(event)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("error.validation"))
            .andExpect(jsonPath("$.error_description").value(notNullValue()))
            .andExpect(jsonPath("$.fieldErrors[*].objectName").value("event"))
            .andExpect(jsonPath("$.fieldErrors[*].field")
                .value(Event.class.getDeclaredField("eventDataRef").getName()))
            .andExpect(jsonPath("$.fieldErrors[*].message").value("EventDataTypeKey"))
            .andExpect(jsonPath("$.fieldErrors[*].description")
                .value("Specified event data ref type key not matched with configured"));
    }

    @Test(expected = DataIntegrityViolationException.class)
    @Transactional
    public void createEventWithAlreadyAssignedEventDataRef() throws Exception {
        eventRepository.saveAndFlush(event);
        em.clear();

        Event eventWithAlreadyAssignedEventDataRef = event;
        eventWithAlreadyAssignedEventDataRef.setId(null);

        restEventMockMvc.perform(post("/api/events")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(eventWithAlreadyAssignedEventDataRef)));
        eventRepository.flush();// New event with already assigned event data ref must fail by unique constraint
    }

    @Test
    @Transactional
    public void checkTitleIsRequired() throws Exception {
        int databaseSizeBeforeTest = eventRepository.findAll().size();
        // set the field null
        event.setTitle(null);

        // Create the Event, which fails.

        restEventMockMvc.perform(post("/api/events")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(event)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("error.validation"))
            .andExpect(jsonPath("$.error_description").value(notNullValue()))
            .andExpect(jsonPath("$.fieldErrors[0].objectName").value("event"))
            .andExpect(jsonPath("$.fieldErrors[0].field").value("title"))
            .andExpect(jsonPath("$.fieldErrors[0].message").value("NotNull"))
        ;

        List<Event> eventList = eventRepository.findAll();
        assertThat(eventList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    @WithMockUser(authorities = "SUPER-ADMIN")
    public void getAllEvents() throws Exception {
        // Initialize the database
        eventRepository.saveAndFlush(event);

        // Get all the eventList
        restEventMockMvc.perform(get("/api/events?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.[*].id").value(hasItem(event.getId().intValue())))
            .andExpect(jsonPath("$.[*].typeKey").value(hasItem(DEFAULT_TYPE_KEY)))
            .andExpect(jsonPath("$.[*].repeatRuleKey").value(hasItem(DEFAULT_REPEAT_RULE_KEY)))
            .andExpect(jsonPath("$.[*].title").value(hasItem(DEFAULT_TITLE)))
            .andExpect(jsonPath("$.[*].description").value(hasItem(DEFAULT_DESCRIPTION)))
            .andExpect(jsonPath("$.[*].startDate").value(hasItem(DEFAULT_START_DATE.toString())))
            .andExpect(jsonPath("$.[*].endDate").value(hasItem(DEFAULT_END_DATE.toString())));
    }

    @Test
    @Transactional
    @WithMockUser(authorities = "SUPER-ADMIN")
    public void getAllEventsByAssignedFilter() throws Exception {
        // Initialize the database
        eventRepository.saveAndFlush(event);

        // Get all the eventList
        restEventMockMvc.perform(get("/api/events?assignedId.equals=" + event.getAssigned().getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.[*].id").value(hasItem(event.getId().intValue())))
            .andExpect(jsonPath("$.[*].typeKey").value(hasItem(DEFAULT_TYPE_KEY)))
            .andExpect(jsonPath("$.[*].repeatRuleKey").value(hasItem(DEFAULT_REPEAT_RULE_KEY)))
            .andExpect(jsonPath("$.[*].title").value(hasItem(DEFAULT_TITLE)))
            .andExpect(jsonPath("$.[*].description").value(hasItem(DEFAULT_DESCRIPTION)))
            .andExpect(jsonPath("$.[*].startDate").value(hasItem(DEFAULT_START_DATE.toString())))
            .andExpect(jsonPath("$.[*].endDate").value(hasItem(DEFAULT_END_DATE.toString())))
            .andExpect(jsonPath("$.[*].assigned").value(hasItem(event.getAssigned().getId().intValue())));
    }

    @Test
    @Transactional
    public void getEvent() throws Exception {
        // Initialize the database
        eventRepository.saveAndFlush(event);

        // Get the event
        restEventMockMvc.perform(get("/api/events/{id}", event.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(event.getId().intValue()))
            .andExpect(jsonPath("$.typeKey").value(DEFAULT_TYPE_KEY))
            .andExpect(jsonPath("$.repeatRuleKey").value(DEFAULT_REPEAT_RULE_KEY))
            .andExpect(jsonPath("$.title").value(DEFAULT_TITLE))
            .andExpect(jsonPath("$.description").value(DEFAULT_DESCRIPTION))
            .andExpect(jsonPath("$.startDate").value(DEFAULT_START_DATE.toString()))
            .andExpect(jsonPath("$.endDate").value(DEFAULT_END_DATE.toString()));
    }

    @Test
    @Transactional
    public void getNonExistingEvent() throws Exception {
        // Get the event
        restEventMockMvc.perform(get("/api/events/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("error.notfound"))
            .andExpect(jsonPath("$.error_description").value(notNullValue()))
        ;
    }

    @Test
    @Transactional
    public void updateEvent() throws Exception {
        // Initialize the database
        em.persist(event);

        int databaseSizeBeforeUpdate = eventRepository.findAll().size();

        // Update the event
        em.detach(event);
        em.detach(event.getEventDataRef());
        event
            .typeKey(UPDATED_TYPE_KEY)
            .repeatRuleKey(UPDATED_REPEAT_RULE_KEY)
            .title(UPDATED_TITLE)
            .description(UPDATED_DESCRIPTION)
            .startDate(UPDATED_START_DATE)
            .endDate(UPDATED_END_DATE)
            .color(UPDATED_COLOR)
            .getEventDataRef().setData(UPDATED_EVENT_DATA_REF_DATA);

        restEventMockMvc.perform(put("/api/events")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(event)))
            .andExpect(status().isOk());

        // Validate the Event in the database
        List<Event> eventList = eventRepository.findAll();
        assertThat(eventList).hasSize(databaseSizeBeforeUpdate);
        Event testEvent = eventList.get(eventList.size() - 1);
        assertThat(testEvent.getTypeKey()).isEqualTo(UPDATED_TYPE_KEY);
        assertThat(testEvent.getRepeatRuleKey()).isEqualTo(UPDATED_REPEAT_RULE_KEY);
        assertThat(testEvent.getTitle()).isEqualTo(UPDATED_TITLE);
        assertThat(testEvent.getColor()).isEqualTo(UPDATED_COLOR);
        assertThat(testEvent.getDescription()).isEqualTo(UPDATED_DESCRIPTION);
        assertThat(testEvent.getStartDate()).isEqualTo(UPDATED_START_DATE);
        assertThat(testEvent.getEndDate()).isEqualTo(UPDATED_END_DATE);
        //assert that event data ref not updated
        assertThat(testEvent.getEventDataRef()).isNotNull()
            .extracting(XmEntity::getData).isEqualTo(UPDATED_EVENT_DATA_REF_DATA);
    }

    @Test
    @Transactional
    public void updateNonExistingEvent() throws Exception {
        int databaseSizeBeforeUpdate = eventRepository.findAll().size();

        // Create the Event

        // If the entity doesn't have an ID, it will be created instead of just being updated
        restEventMockMvc.perform(put("/api/events")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(event)))
            .andExpect(status().isCreated());

        // Validate the Event in the database
        List<Event> eventList = eventRepository.findAll();
        assertThat(eventList).hasSize(databaseSizeBeforeUpdate + 1);
    }

    @Test
    @Transactional
    public void deleteEvent() throws Exception {
        // Initialize the database
        XmEntity eventDataRef = Objects.requireNonNull(event.getEventDataRef(), "Event data ref can't be NULL");
        em.persist(event);

        int databaseSizeBeforeDelete = eventRepository.findAll().size();

        // Get the event
        restEventMockMvc.perform(delete("/api/events/{id}", event.getId())
            .accept(TestUtil.APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());

        // Validate the database is empty
        List<Event> eventList = eventRepository.findAll();
        assertThat(eventList).hasSize(databaseSizeBeforeDelete - 1);
        assertNull(em.find(XmEntity.class, eventDataRef.getId()));
    }

    @Test
    @Transactional
    public void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Event.class);
        Event event1 = new Event();
        event1.setId(1L);
        Event event2 = new Event();
        event2.setId(event1.getId());
        assertThat(event1).isEqualTo(event2);
        event2.setId(2L);
        assertThat(event1).isNotEqualTo(event2);
        event1.setId(null);
        assertThat(event1).isNotEqualTo(event2);
    }
}
