package com.icthh.xm.ms.entity.service;

import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.ms.entity.AbstractSpringBootTest;
import com.icthh.xm.ms.entity.domain.Event;
import com.icthh.xm.ms.entity.service.query.filter.EventFilter;
import io.github.jhipster.service.filter.InstantFilter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.Assert;

import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.LinkedList;
import java.util.List;

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_AUTH_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;

public class EventQueryServiceIntTest extends AbstractSpringBootTest {

    @Autowired
    private EventService eventService;
    @Autowired
    private EntityManager em;
    @Autowired
    private TenantContextHolder tenantContextHolder;
    @Autowired
    private LepManager lepManager;
    @Autowired
    private XmAuthenticationContextHolder authContextHolder;

    private List<Event> expected;

    @Before
    public void setup() {

        lepManager.beginThreadContext(ctx -> {
            ctx.setValue(THREAD_CONTEXT_KEY_TENANT_CONTEXT, tenantContextHolder.getContext());
            ctx.setValue(THREAD_CONTEXT_KEY_AUTH_CONTEXT, authContextHolder.getContext());
        });

        expected = initEvents();
    }

    @Test
    @Transactional
    public void findAllTest() {
        EventFilter eventFilter = new EventFilter();
        InstantFilter instantFilter = new InstantFilter();

        instantFilter.setGreaterThanOrEqual(LocalDate.parse("2019-01-26").atStartOfDay().toInstant(ZoneOffset.UTC));
        instantFilter.setLessThanOrEqual(LocalDate.parse("2019-01-26").atTime(LocalTime.MAX).toInstant(ZoneOffset.UTC));
        eventFilter.setStartDate(instantFilter);
        List<Event> actual = eventService.findAllByFilter(eventFilter);

        Assert.assertNotNull(actual);
        Assert.assertEquals(expected.size() - 1, actual.size());
    }

    @BeforeTransaction
    public void beforeTransaction() {
        TenantContextUtils.setTenant(tenantContextHolder, "TEST");
    }

    @After
    public void tearDown() {
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
    }

    public List<Event> initEvents() {
        List<Event> events = new LinkedList<>();
        for (int i = 0; i < 10; i++) {
            Event event = createEntity();
            em.persist(event);
            em.flush();
            events.add(event);
        }
        Event event = new Event()
            .typeKey("Event2")
            .title("Tomorrow event")
            .startDate(LocalDate.parse("2019-01-27").atTime(LocalTime.parse("07:00")).toInstant(ZoneOffset.UTC))
            .endDate(LocalDate.parse("2019-01-27").atTime(LocalTime.parse("20:00")).toInstant(ZoneOffset.UTC));
        events.add(event);
        return events;
    }

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Event createEntity() {
        return new Event()
            .typeKey("Event")
            .title("Today event")
            .startDate(LocalDate.parse("2019-01-26").atTime(LocalTime.parse("07:00")).toInstant(ZoneOffset.UTC))
            .endDate(LocalDate.parse("2019-01-26").atTime(LocalTime.parse("20:00")).toInstant(ZoneOffset.UTC));
    }
}
