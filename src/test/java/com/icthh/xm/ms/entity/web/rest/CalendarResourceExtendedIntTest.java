package com.icthh.xm.ms.entity.web.rest;

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_AUTH_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import com.icthh.xm.ms.entity.repository.CalendarRepository;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
import com.icthh.xm.ms.entity.service.CalendarService;
import com.icthh.xm.ms.entity.service.XmEntitySpecService;
import com.icthh.xm.ms.entity.service.impl.StartUpdateDateGenerationStrategy;
import com.icthh.xm.ms.entity.service.query.EventQueryService;
import java.time.Instant;
import java.util.List;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Validator;

/**
 * Extended Test class for the CalendarResource REST controller.
 *
 * @see CalendarResource
 */
@WithMockUser(authorities = {"SUPER-ADMIN"})
public class CalendarResourceExtendedIntTest extends AbstractJupiterSpringBootTest {

    private static final Instant MOCKED_START_DATE = Instant.ofEpochMilli(42L);

    @Autowired
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Autowired
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @Autowired
    private EntityManager em;

    @Autowired
    private Validator validator;

    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Autowired
    private CalendarRepository calendarRepository;

    @Autowired
    private CalendarResource calendarResource;

    @Autowired
    private PermittedRepository permittedRepository;

    @Autowired
    private XmEntityRepository xmEntityRepository;

    @Autowired
    private EventQueryService eventQueryService;

    @Spy
    private StartUpdateDateGenerationStrategy startUpdateDateGenerationStrategy;

    private CalendarService calendarService;

    private MockMvc restCalendarMockMvc;

    private Calendar calendar;

    @Autowired
    private LepManager lepManager;

    @Mock
    private XmAuthenticationContext context;

    @Autowired
    private XmEntitySpecService xmEntitySpecService;

    @BeforeTransaction
    public void beforeTransaction() {
        TenantContextUtils.setTenant(tenantContextHolder, "RESINTTEST");
    }

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);

        when(startUpdateDateGenerationStrategy.generateStartDate()).thenReturn(MOCKED_START_DATE);

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

        calendar = CalendarResourceIntTest.createEntity(em);

        lepManager.beginThreadContext(ctx -> {
            ctx.setValue(THREAD_CONTEXT_KEY_TENANT_CONTEXT, tenantContextHolder.getContext());
            ctx.setValue(THREAD_CONTEXT_KEY_AUTH_CONTEXT, context);
        });

    }

    @AfterEach
    public void tearDown() {
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
    }

    @Test
    @Transactional
    public void checkStartDateIsNotRequired() throws Exception {
        int databaseSizeBeforeTest = calendarRepository.findAll().size();
        // set the field null
        calendar.setStartDate(null);

        // Create the Calendar.

        restCalendarMockMvc.perform(post("/api/calendars")
                                    .contentType(TestUtil.APPLICATION_JSON_UTF8)
                                    .content(TestUtil.convertObjectToJsonBytes(calendar)))
                       .andExpect(status().isCreated())
                       .andExpect(jsonPath("$.startDate").value(MOCKED_START_DATE.toString()))
        ;

        List<Calendar> calendarList = calendarRepository.findAll();
        assertThat(calendarList).hasSize(databaseSizeBeforeTest + 1);

        Calendar testCalendar = calendarList.get(calendarList.size() - 1);
        assertThat(testCalendar.getStartDate()).isEqualTo(MOCKED_START_DATE);
    }

    @Test
    @Transactional
    public void createCalendarWithEntryDate() throws Exception {

        int databaseSizeBeforeCreate = calendarRepository.findAll().size();

        // Create the Calendar
        restCalendarMockMvc.perform(post("/api/calendars")
                                    .contentType(TestUtil.APPLICATION_JSON_UTF8)
                                    .content(TestUtil.convertObjectToJsonBytes(calendar)))
                       .andExpect(status().isCreated())
                       .andExpect(jsonPath("$.startDate").value(MOCKED_START_DATE.toString()));

        // Validate the Calendar in the database
        List<Calendar> voteList = calendarRepository.findAll();
        assertThat(voteList).hasSize(databaseSizeBeforeCreate + 1);

        Calendar testCalendar = voteList.get(voteList.size() - 1);
        assertThat(testCalendar.getStartDate()).isEqualTo(MOCKED_START_DATE);
    }

    @Test
    @Transactional
    public void checkStartDateIsRequiredInDb() {

        Calendar cal = calendarService.save(calendar);

        // set the field null
        when(startUpdateDateGenerationStrategy.generateStartDate()).thenReturn(null);

        cal.setStartDate(null);

        calendarService.save(cal);


        try {
            calendarRepository.flush();
            fail("DataIntegrityViolationException exception was expected!");
        } catch (DataIntegrityViolationException e) {
            assertThat(e.getMostSpecificCause().getMessage())
                .containsIgnoringCase("NULL not allowed for column \"START_DATE\"");
        }

    }

}
