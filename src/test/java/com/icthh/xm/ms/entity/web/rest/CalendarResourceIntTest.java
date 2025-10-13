package com.icthh.xm.ms.entity.web.rest;

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_AUTH_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.icthh.xm.commons.i18n.error.web.ExceptionTranslator;
import com.icthh.xm.commons.permission.repository.PermittedRepository;
import com.icthh.xm.commons.security.XmAuthenticationContext;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.ms.entity.AbstractJupiterSpringBootTest;
import com.icthh.xm.ms.entity.domain.Calendar;
import com.icthh.xm.ms.entity.domain.Event;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.spec.CalendarSpec;
import com.icthh.xm.ms.entity.repository.CalendarRepository;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
import com.icthh.xm.ms.entity.service.CalendarService;
import com.icthh.xm.ms.entity.service.XmEntitySpecService;
import com.icthh.xm.ms.entity.service.impl.StartUpdateDateGenerationStrategy;
import com.icthh.xm.ms.entity.service.query.EventQueryService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.EntityManager;
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
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Validator;

/**
 * Test class for the CalendarResource REST controller.
 *
 * @see CalendarResource
 */
@WithMockUser(authorities = {"SUPER-ADMIN"})
public class CalendarResourceIntTest extends AbstractJupiterSpringBootTest {

    public static final String DEFAULT_TYPE_KEY = "AAAAAAAAAA";
    private static final String UPDATED_TYPE_KEY = "BBBBBBBBBB";

    private static final String DEFAULT_NAME = "AAAAAAAAAA";
    private static final String UPDATED_NAME = "BBBBBBBBBB";

    private static final String DEFAULT_DESCRIPTION = "AAAAAAAAAA";
    private static final String UPDATED_DESCRIPTION = "BBBBBBBBBB";

    private static final String DEFAULT_TIMEZONE_ID = "Europe/Kyiv";
    private static final String UPDATED_TIMEZONE_ID = "Europe/Kyiv1";

    private static final Instant DEFAULT_START_DATE = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_START_DATE = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_END_DATE = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_END_DATE = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    @Autowired
    private CalendarResource calendarResource;

    @Autowired
    private CalendarRepository calendarRepository;

    @Autowired
    private PermittedRepository permittedRepository;

    @Spy
    private StartUpdateDateGenerationStrategy startUpdateDateGenerationStrategy;

    @Autowired
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Autowired
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @Autowired
    private EntityManager em;

    @Autowired
    private XmEntityRepository xmEntityRepository;

    @Autowired
    private EventQueryService eventQueryService;

    private CalendarService calendarService;

    private MockMvc restCalendarMockMvc;

    private Calendar calendar;

    @Autowired
    private Validator validator;

    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Autowired
    private LepManager lepManager;

    @Mock
    private XmAuthenticationContext context;

    @Mock
    private XmEntitySpecService xmEntitySpecService;

    @BeforeTransaction
    public void beforeTransaction() {
        TenantContextUtils.setTenant(tenantContextHolder, "RESINTTEST");
    }

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        when(startUpdateDateGenerationStrategy.generateStartDate()).thenReturn(DEFAULT_START_DATE);

        calendarService = new CalendarService(
            calendarRepository,
            permittedRepository,
            startUpdateDateGenerationStrategy,
            xmEntityRepository,
            eventQueryService,
            xmEntitySpecService);

        CalendarResource calendarResourceMock = new CalendarResource(calendarService, calendarResource);
        this.restCalendarMockMvc = MockMvcBuilders.standaloneSetup(calendarResourceMock)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setValidator(validator)
            .setMessageConverters(jacksonMessageConverter).build();

        calendar = createEntity(em);

        lepManager.beginThreadContext(ctx -> {
            ctx.setValue(THREAD_CONTEXT_KEY_TENANT_CONTEXT, tenantContextHolder.getContext());
            ctx.setValue(THREAD_CONTEXT_KEY_AUTH_CONTEXT, context);
        });

    }

    @AfterEach
    public void tearDown() {
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
    }

    /**
     * Create an entity for this test.
     * <p>
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Calendar createEntity(EntityManager em) {
        // Create required entity
        XmEntity xmEntity = XmEntityResourceIntTest.createEntity();
        em.persist(xmEntity);
        em.flush();

        return new Calendar()
            .typeKey(DEFAULT_TYPE_KEY)
            .name(DEFAULT_NAME)
            .description(DEFAULT_DESCRIPTION)
            .startDate(DEFAULT_START_DATE)
            .endDate(DEFAULT_END_DATE)
            .xmEntity(xmEntity)
            .timeZoneId(DEFAULT_TIMEZONE_ID);
    }

    @BeforeEach
    public void initTest() {
        //  calendarSearchRepository.deleteAll();
    }

    @Test
    @Transactional
    public void createCalendar() throws Exception {
        int databaseSizeBeforeCreate = calendarRepository.findAll().size();

        // Create the Calendar
        restCalendarMockMvc.perform(post("/api/calendars")
                                        .contentType(TestUtil.APPLICATION_JSON_UTF8)
                                        .content(TestUtil.convertObjectToJsonBytes(calendar)))
            .andExpect(status().isCreated());

        // Validate the Calendar in the database
        List<Calendar> calendarList = calendarRepository.findAll();
        assertThat(calendarList).hasSize(databaseSizeBeforeCreate + 1);
        Calendar testCalendar = calendarList.get(calendarList.size() - 1);
        assertThat(testCalendar.getTypeKey()).isEqualTo(DEFAULT_TYPE_KEY);
        assertThat(testCalendar.getName()).isEqualTo(DEFAULT_NAME);
        assertThat(testCalendar.getDescription()).isEqualTo(DEFAULT_DESCRIPTION);
        assertThat(testCalendar.getStartDate()).isEqualTo(DEFAULT_START_DATE);
        assertThat(testCalendar.getEndDate()).isEqualTo(DEFAULT_END_DATE);
        assertThat(testCalendar.getTimeZoneId()).isEqualTo(DEFAULT_TIMEZONE_ID);
    }

    @Test
    @Transactional
    public void createReadOnlyCalendar() throws Exception {
        CalendarSpec calendarSpec = new CalendarSpec();
        calendarSpec.setReadonly(true);
        when(xmEntitySpecService.findCalendar(eq(XmEntityResourceIntTest.DEFAULT_TYPE_KEY), eq(DEFAULT_TYPE_KEY)))
            .thenReturn(Optional.of(calendarSpec));
        createCalendar();
    }

    @Test
    @Transactional
    public void updateReadOnlyCalendar() throws Exception {
        CalendarSpec calendarSpec = new CalendarSpec();
        calendarSpec.setReadonly(true);
        when(xmEntitySpecService.findCalendar(eq(XmEntityResourceIntTest.DEFAULT_TYPE_KEY), eq(DEFAULT_TYPE_KEY)))
            .thenReturn(Optional.of(calendarSpec));
        when(xmEntitySpecService.findCalendar(eq(XmEntityResourceIntTest.DEFAULT_TYPE_KEY), eq(UPDATED_TYPE_KEY)))
            .thenReturn(Optional.of(calendarSpec));
        calendarService.save(calendar);

        Calendar updatedCalendar = calendarRepository.findById(calendar.getId()).get();
        updatedCalendar
            .typeKey(UPDATED_TYPE_KEY)
            .name(UPDATED_NAME)
            .description(UPDATED_DESCRIPTION)
            .startDate(UPDATED_START_DATE)
            .endDate(UPDATED_END_DATE)
            .setTimeZoneId(UPDATED_TIMEZONE_ID);

        restCalendarMockMvc.perform(put("/api/calendars")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(updatedCalendar)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("error.read.only.calendar"))
            .andExpect(jsonPath("$.error_description").value(notNullValue()));
    }

    @Test
    @Transactional
    public void createCalendarWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = calendarRepository.findAll().size();

        // Create the Calendar with an existing ID
        calendar.setId(1L);

        // An entity with an existing ID cannot be created, so this API call must fail
        restCalendarMockMvc.perform(post("/api/calendars")
                                        .contentType(TestUtil.APPLICATION_JSON_UTF8)
                                        .content(TestUtil.convertObjectToJsonBytes(calendar)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("error.business.idexists"))
            .andExpect(jsonPath("$.error_description").value(notNullValue()))
        ;

        // Validate the Alice in the database
        List<Calendar> calendarList = calendarRepository.findAll();
        assertThat(calendarList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    public void checkTypeKeyIsRequired() throws Exception {
        int databaseSizeBeforeTest = calendarRepository.findAll().size();
        // set the field null
        calendar.setTypeKey(null);

        // Create the Calendar, which fails.

        restCalendarMockMvc.perform(post("/api/calendars")
                                        .contentType(TestUtil.APPLICATION_JSON_UTF8)
                                        .content(TestUtil.convertObjectToJsonBytes(calendar)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("error.validation"))
            .andExpect(jsonPath("$.error_description").value(notNullValue()))
            .andExpect(jsonPath("$.fieldErrors[0].objectName").value("calendar"))
            .andExpect(jsonPath("$.fieldErrors[0].field").value("typeKey"))
            .andExpect(jsonPath("$.fieldErrors[0].message").value("NotNull"))
        ;

        List<Calendar> calendarList = calendarRepository.findAll();
        assertThat(calendarList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkNameIsRequired() throws Exception {
        int databaseSizeBeforeTest = calendarRepository.findAll().size();
        // set the field null
        calendar.setName(null);

        // Create the Calendar, which fails.

        restCalendarMockMvc.perform(post("/api/calendars")
                                        .contentType(TestUtil.APPLICATION_JSON_UTF8)
                                        .content(TestUtil.convertObjectToJsonBytes(calendar)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("error.validation"))
            .andExpect(jsonPath("$.error_description").value(notNullValue()))
            .andExpect(jsonPath("$.fieldErrors[0].objectName").value("calendar"))
            .andExpect(jsonPath("$.fieldErrors[0].field").value("name"))
            .andExpect(jsonPath("$.fieldErrors[0].message").value("NotNull"))
        ;

        List<Calendar> calendarList = calendarRepository.findAll();
        assertThat(calendarList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    @Disabled("see CalendarResourceExtendedIntTest.checkStartDateIsNotRequired instead")
    public void checkStartDateIsRequired() throws Exception {
        int databaseSizeBeforeTest = calendarRepository.findAll().size();
        // set the field null
        calendar.setStartDate(null);

        // Create the Calendar, which fails.

        restCalendarMockMvc.perform(post("/api/calendars")
                                        .contentType(TestUtil.APPLICATION_JSON_UTF8)
                                        .content(TestUtil.convertObjectToJsonBytes(calendar)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("error.validation"))
            .andExpect(jsonPath("$.error_description").value(notNullValue()))
            .andExpect(jsonPath("$.fieldErrors[0].objectName").value("calendar"))
            .andExpect(jsonPath("$.fieldErrors[0].field").value("startDate"))
            .andExpect(jsonPath("$.fieldErrors[0].message").value("NotNull"))
        ;

        List<Calendar> calendarList = calendarRepository.findAll();
        assertThat(calendarList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    @WithMockUser(authorities = "SUPER-ADMIN")
    public void getAllCalendars() throws Exception {
        // Initialize the database
        calendarRepository.saveAndFlush(calendar);

        // Get all the calendarList
        restCalendarMockMvc.perform(get("/api/calendars?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.[*].id").value(hasItem(calendar.getId().intValue())))
            .andExpect(jsonPath("$.[*].typeKey").value(hasItem(DEFAULT_TYPE_KEY)))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME)))
            .andExpect(jsonPath("$.[*].description").value(hasItem(DEFAULT_DESCRIPTION)))
            .andExpect(jsonPath("$.[*].startDate").value(hasItem(DEFAULT_START_DATE.toString())))
            .andExpect(jsonPath("$.[*].endDate").value(hasItem(DEFAULT_END_DATE.toString())))
            .andExpect(jsonPath("$.[*].timeZoneId").value(hasItem(DEFAULT_TIMEZONE_ID)));
    }

    @Test
    @Transactional
    public void getCalendar() throws Exception {
        // Initialize the database
        calendarRepository.saveAndFlush(calendar);

        // Get the calendar
        restCalendarMockMvc.perform(get("/api/calendars/{id}", calendar.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(calendar.getId().intValue()))
            .andExpect(jsonPath("$.typeKey").value(DEFAULT_TYPE_KEY))
            .andExpect(jsonPath("$.name").value(DEFAULT_NAME))
            .andExpect(jsonPath("$.description").value(DEFAULT_DESCRIPTION))
            .andExpect(jsonPath("$.startDate").value(DEFAULT_START_DATE.toString()))
            .andExpect(jsonPath("$.endDate").value(DEFAULT_END_DATE.toString()))
            .andExpect(jsonPath("$.timeZoneId").value(DEFAULT_TIMEZONE_ID));
    }

    @Test
    @Transactional
    public void getNonExistingCalendar() throws Exception {
        // Get the calendar
        restCalendarMockMvc.perform(get("/api/calendars/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("error.notfound"))
            .andExpect(jsonPath("$.error_description").value(notNullValue()))
        ;
    }

    @Test
    @Transactional
    public void updateCalendar() throws Exception {
        // Initialize the database
        calendarService.save(calendar);

        int databaseSizeBeforeUpdate = calendarRepository.findAll().size();

        // Update the calendar
        Calendar updatedCalendar = calendarRepository.findById(calendar.getId())
            .orElseThrow(NullPointerException::new);
        updatedCalendar
            .typeKey(UPDATED_TYPE_KEY)
            .name(UPDATED_NAME)
            .description(UPDATED_DESCRIPTION)
            .startDate(UPDATED_START_DATE)
            .endDate(UPDATED_END_DATE)
            .setTimeZoneId(UPDATED_TIMEZONE_ID);

        restCalendarMockMvc.perform(put("/api/calendars")
                                        .contentType(TestUtil.APPLICATION_JSON_UTF8)
                                        .content(TestUtil.convertObjectToJsonBytes(updatedCalendar)))
            .andExpect(status().isOk());

        // Validate the Calendar in the database
        List<Calendar> calendarList = calendarRepository.findAll();
        assertThat(calendarList).hasSize(databaseSizeBeforeUpdate);
        Calendar testCalendar = calendarList.get(calendarList.size() - 1);
        assertThat(testCalendar.getTypeKey()).isEqualTo(UPDATED_TYPE_KEY);
        assertThat(testCalendar.getName()).isEqualTo(UPDATED_NAME);
        assertThat(testCalendar.getDescription()).isEqualTo(UPDATED_DESCRIPTION);
        assertThat(testCalendar.getStartDate()).isEqualTo(UPDATED_START_DATE);
        assertThat(testCalendar.getEndDate()).isEqualTo(UPDATED_END_DATE);
        assertThat(testCalendar.getTimeZoneId()).isEqualTo(UPDATED_TIMEZONE_ID);
    }

    @Test
    @Transactional
    public void updateNonExistingCalendar() throws Exception {
        int databaseSizeBeforeUpdate = calendarRepository.findAll().size();

        // Create the Calendar

        // If the entity doesn't have an ID, it will be created instead of just being updated
        restCalendarMockMvc.perform(put("/api/calendars")
                                        .contentType(TestUtil.APPLICATION_JSON_UTF8)
                                        .content(TestUtil.convertObjectToJsonBytes(calendar)))
            .andExpect(status().isCreated());

        // Validate the Calendar in the database
        List<Calendar> calendarList = calendarRepository.findAll();
        assertThat(calendarList).hasSize(databaseSizeBeforeUpdate + 1);
    }

    @Test
    @Transactional
    public void deleteCalendar() throws Exception {
        // Initialize the database
        calendarService.save(calendar);

        int databaseSizeBeforeDelete = calendarRepository.findAll().size();

        // Get the calendar
        restCalendarMockMvc.perform(delete("/api/calendars/{id}", calendar.getId())
                                        .accept(TestUtil.APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());

        // Validate the database is empty
        List<Calendar> calendarList = calendarRepository.findAll();
        assertThat(calendarList).hasSize(databaseSizeBeforeDelete - 1);
    }

    @Test
    @Transactional
    public void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Calendar.class);
        Calendar calendar1 = new Calendar();
        calendar1.setId(1L);
        Calendar calendar2 = new Calendar();
        calendar2.setId(calendar1.getId());
        assertThat(calendar1).isEqualTo(calendar2);
        calendar2.setId(2L);
        assertThat(calendar1).isNotEqualTo(calendar2);
        calendar1.setId(null);
        assertThat(calendar1).isNotEqualTo(calendar2);
    }

    @Test
    @Transactional
    public void getEventsByIdFiltering() throws Exception {
        // Initialize the database
        calendarRepository.saveAndFlush(calendar);
        Event event1 = EventResourceIntTest.createEntity(em);
        event1.setCalendar(calendar);
        em.persist(event1);

        Event event2 = EventResourceIntTest.createEntity(em);
        event2.setCalendar(calendar);
        em.persist(event1);

        Long id = event1.getId();

        //assert that single event returned
        defaultEventShouldBeFound(event1, "id.equals=" + id)
            .andExpect(jsonPath("$.length()").value(1));
        defaultEventShouldNotBeFound(event1, "id.notEquals=" + id);

        defaultEventShouldBeFound(event1, "id.greaterThanOrEqual=" + id);
        defaultEventShouldNotBeFound(event1, "id.greaterThan=" + id);

        defaultEventShouldBeFound(event1, "id.lessThanOrEqual=" + id);
        defaultEventShouldNotBeFound(event1, "id.lessThan=" + id);
    }


    @Test
    @Transactional
    public void getAllEventsByTypeKeyIsEqualToSomething() throws Exception {
        // Initialize the database
        calendarRepository.saveAndFlush(calendar);
        Event event = EventResourceIntTest.createEntity(em);
        event.setCalendar(calendar);
        em.persist(event);

        String defaultTypeKey = event.getTypeKey();
        String updatedTypeKey = defaultTypeKey + UPDATED_TYPE_KEY;

        // Get all the eventList where typeKey equals to DEFAULT_TYPE_KEY
        defaultEventShouldBeFound(event, "typeKey.equals=" + defaultTypeKey);

        // Get all the eventList where typeKey equals to UPDATED_TYPE_KEY
        defaultEventShouldNotBeFound(event, "typeKey.equals=" + updatedTypeKey);
    }

    @Test
    @Transactional
    public void getAllEventsByTypeKeyIsNotEqualToSomething() throws Exception {
        // Initialize the database
        calendarRepository.saveAndFlush(calendar);
        Event event = EventResourceIntTest.createEntity(em);
        event.setCalendar(calendar);
        em.persist(event);

        String defaultTypeKey = event.getTypeKey();
        String updatedTypeKey = defaultTypeKey + UPDATED_TYPE_KEY;

        // Get all the eventList where typeKey not equals to DEFAULT_TYPE_KEY
        defaultEventShouldNotBeFound(event, "typeKey.notEquals=" + defaultTypeKey);

        // Get all the eventList where typeKey not equals to UPDATED_TYPE_KEY
        defaultEventShouldBeFound(event, "typeKey.notEquals=" + updatedTypeKey);
    }

    @Test
    @Transactional
    public void getAllEventsByTypeKeyIsInShouldWork() throws Exception {
        // Initialize the database
        calendarRepository.saveAndFlush(calendar);
        Event event = EventResourceIntTest.createEntity(em);
        event.setCalendar(calendar);
        em.persist(event);

        String defaultTypeKey = event.getTypeKey();
        String updatedTypeKey = defaultTypeKey + UPDATED_TYPE_KEY;

        // Get all the eventList where typeKey in DEFAULT_TYPE_KEY or UPDATED_TYPE_KEY
        defaultEventShouldBeFound(event, "typeKey.in=" + defaultTypeKey + "," + updatedTypeKey);

        // Get all the eventList where typeKey equals to UPDATED_TYPE_KEY
        defaultEventShouldNotBeFound(event, "typeKey.in=" + updatedTypeKey);
    }

    @Test
    @Transactional
    public void getAllEventsByTypeKeyIsNullOrNotNull() throws Exception {
        // Initialize the database
        calendarRepository.saveAndFlush(calendar);
        Event event = EventResourceIntTest.createEntity(em);
        event.setCalendar(calendar);
        em.persist(event);

        // Get all the eventList where typeKey is not null
        defaultEventShouldBeFound(event, "typeKey.specified=true");

        // Get all the eventList where typeKey is null
        defaultEventShouldNotBeFound(event, "typeKey.specified=false");
    }
    @Test
    @Transactional
    public void getAllEventsByTypeKeyContainsSomething() throws Exception {
        // Initialize the database
        calendarRepository.saveAndFlush(calendar);
        Event event = EventResourceIntTest.createEntity(em);
        event.setCalendar(calendar);
        em.persist(event);

        String defaultTypeKey = event.getTypeKey();
        String updatedTypeKey = defaultTypeKey + UPDATED_TYPE_KEY;

        // Get all the eventList where typeKey contains DEFAULT_TYPE_KEY
        defaultEventShouldBeFound(event, "typeKey.contains=" + defaultTypeKey);

        // Get all the eventList where typeKey contains UPDATED_TYPE_KEY
        defaultEventShouldNotBeFound(event, "typeKey.contains=" + updatedTypeKey);
    }

    @Test
    @Transactional
    public void getAllEventsByTypeKeyNotContainsSomething() throws Exception {
        // Initialize the database
        calendarRepository.saveAndFlush(calendar);
        Event event = EventResourceIntTest.createEntity(em);
        event.setCalendar(calendar);
        em.persist(event);

        String defaultTypeKey = event.getTypeKey();
        String updatedTypeKey = defaultTypeKey + UPDATED_TYPE_KEY;

        // Get all the eventList where typeKey does not contain DEFAULT_TYPE_KEY
        defaultEventShouldNotBeFound(event, "typeKey.doesNotContain=" + defaultTypeKey);

        // Get all the eventList where typeKey does not contain UPDATED_TYPE_KEY
        defaultEventShouldBeFound(event, "typeKey.doesNotContain=" + updatedTypeKey);
    }

    @Test
    @Transactional
    public void getAllEventsByStartDateIsEqualToSomething() throws Exception {
        // Initialize the database
        calendarRepository.saveAndFlush(calendar);
        Event event = EventResourceIntTest.createEntity(em);
        event.setCalendar(calendar);
        em.persist(event);

        Instant defaultStartDate = event.getStartDate();
        Instant updatedStartDate = defaultStartDate.plus(1, ChronoUnit.DAYS);

        // Get all the eventList where startDate equals to DEFAULT_START_DATE
        defaultEventShouldBeFound(event, "startDate.equals=" + defaultStartDate);

        // Get all the eventList where startDate equals to UPDATED_START_DATE
        defaultEventShouldNotBeFound(event, "startDate.equals=" + updatedStartDate);
    }

    @Test
    @Transactional
    public void getAllEventsByStartDateIsNotEqualToSomething() throws Exception {
        // Initialize the database
        calendarRepository.saveAndFlush(calendar);
        Event event = EventResourceIntTest.createEntity(em);
        event.setCalendar(calendar);
        em.persist(event);

        Instant defaultStartDate = event.getStartDate();
        Instant updatedStartDate = defaultStartDate.plus(1, ChronoUnit.DAYS);

        // Get all the eventList where startDate not equals to DEFAULT_START_DATE
        defaultEventShouldNotBeFound(event, "startDate.notEquals=" + defaultStartDate);

        // Get all the eventList where startDate not equals to UPDATED_START_DATE
        defaultEventShouldBeFound(event, "startDate.notEquals=" + updatedStartDate);
    }

    @Test
    @Transactional
    public void getAllEventsByStartDateIsInShouldWork() throws Exception {
        // Initialize the database
        calendarRepository.saveAndFlush(calendar);
        Event event = EventResourceIntTest.createEntity(em);
        event.setCalendar(calendar);
        em.persist(event);

        Instant defaultStartDate = event.getStartDate();
        Instant updatedStartDate = defaultStartDate.plus(1, ChronoUnit.DAYS);

        // Get all the eventList where startDate in DEFAULT_START_DATE or UPDATED_START_DATE
        defaultEventShouldBeFound(event, "startDate.in=" + defaultStartDate + "," + updatedStartDate);

        // Get all the eventList where startDate equals to UPDATED_START_DATE
        defaultEventShouldNotBeFound(event, "startDate.in=" + updatedStartDate);
    }

    @Test
    @Transactional
    public void getAllEventsByStartDateIsNullOrNotNull() throws Exception {
        // Initialize the database
        calendarRepository.saveAndFlush(calendar);
        Event event = EventResourceIntTest.createEntity(em);
        event.setCalendar(calendar);
        em.persist(event);

        // Get all the eventList where startDate is not null
        defaultEventShouldBeFound(event, "startDate.specified=true");

        // Get all the eventList where startDate is null
        defaultEventShouldNotBeFound(event, "startDate.specified=false");
    }

    @Test
    @Transactional
    public void getAllEventsByEndDateIsEqualToSomething() throws Exception {
        // Initialize the database
        calendarRepository.saveAndFlush(calendar);
        Event event = EventResourceIntTest.createEntity(em);
        event.setCalendar(calendar);
        em.persist(event);

        Instant defaultEndDate = event.getEndDate();
        Instant updatedEndDate = defaultEndDate.plus(1, ChronoUnit.DAYS);

        // Get all the eventList where endDate equals to DEFAULT_END_DATE
        defaultEventShouldBeFound(event, "endDate.equals=" + defaultEndDate);

        // Get all the eventList where endDate equals to UPDATED_END_DATE
        defaultEventShouldNotBeFound(event, "endDate.equals=" + updatedEndDate);
    }

    @Test
    @Transactional
    public void getAllEventsByEndDateIsNotEqualToSomething() throws Exception {
        // Initialize the database
        calendarRepository.saveAndFlush(calendar);
        Event event = EventResourceIntTest.createEntity(em);
        event.setCalendar(calendar);
        em.persist(event);

        Instant defaultEndDate = event.getEndDate();
        Instant updatedEndDate = defaultEndDate.plus(1, ChronoUnit.DAYS);

        // Get all the eventList where endDate not equals to DEFAULT_END_DATE
        defaultEventShouldNotBeFound(event, "endDate.notEquals=" + defaultEndDate);

        // Get all the eventList where endDate not equals to UPDATED_END_DATE
        defaultEventShouldBeFound(event, "endDate.notEquals=" + updatedEndDate);
    }

    @Test
    @Transactional
    public void getAllEventsByEndDateIsInShouldWork() throws Exception {
        // Initialize the database
        calendarRepository.saveAndFlush(calendar);
        Event event = EventResourceIntTest.createEntity(em);
        event.setCalendar(calendar);
        em.persist(event);

        Instant defaultEndDate = event.getEndDate();
        Instant updatedEndDate = defaultEndDate.plus(1, ChronoUnit.DAYS);


        // Get all the eventList where endDate in DEFAULT_END_DATE or UPDATED_END_DATE
        defaultEventShouldBeFound(event, "endDate.in=" + defaultEndDate + "," + updatedEndDate);

        // Get all the eventList where endDate equals to UPDATED_END_DATE
        defaultEventShouldNotBeFound(event, "endDate.in=" + updatedEndDate);
    }

    @Test
    @Transactional
    public void getAllEventsByEndDateIsNullOrNotNull() throws Exception {
        // Initialize the database
        calendarRepository.saveAndFlush(calendar);
        Event event = EventResourceIntTest.createEntity(em);
        event.setCalendar(calendar);
        em.persist(event);

        // Get all the eventList where endDate is not null
        defaultEventShouldBeFound(event, "endDate.specified=true");

        // Get all the eventList where endDate is null
        defaultEventShouldNotBeFound(event, "endDate.specified=false");
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private ResultActions defaultEventShouldBeFound(Event event, String filter) throws Exception {
        return performGetEventRequest(event, filter)
            .andExpect(jsonPath("$.[*].id").value(hasItem(event.getId().intValue())))
            .andExpect(jsonPath("$.[*].typeKey").value(hasItem(event.getTypeKey())))
            .andExpect(jsonPath("$.[*].startDate").value(hasItem(event.getStartDate().toString())))
            .andExpect(jsonPath("$.[*].endDate").value(hasItem(event.getEndDate().toString())))
            .andExpect(jsonPath("$.[*].calendar").value(hasItem(event.getCalendar().getId().intValue())));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultEventShouldNotBeFound(Event event, String filter) throws Exception {
        performGetEventRequest(event, filter)
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());
    }

    private ResultActions performGetEventRequest(Event event, String filter) throws Exception {
        Long calendarId = event.getCalendar().getId();
        return restCalendarMockMvc.perform(get("/api/calendars/{id}/events?sort=id,desc&" + filter, calendarId))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }
}
