package com.icthh.xm.ms.entity.service;

import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.ms.entity.AbstractSpringBootTest;
import com.icthh.xm.ms.entity.domain.Event;
import com.icthh.xm.ms.entity.service.query.filter.EventFilter;
import io.github.jhipster.service.filter.InstantFilter;

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
    @Mock
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

        instantFilter.setGreaterThanOrEqual(LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC));
        instantFilter.setLessThanOrEqual(LocalDate.now().atTime(LocalTime.MAX).toInstant(ZoneOffset.UTC));
        eventFilter.setStartDate(instantFilter);
        List<Event> actual = eventService.findAllByFilter(eventFilter);

        Assert.assertNotNull(actual);
        Assert.assertEquals(expected, actual);
    }

    @BeforeTransaction
    public void beforeTransaction() {
        TenantContextUtils.setTenant(tenantContextHolder, "RESINTTEST");
    }

    public List<Event> initEvents() {
        List<Event> events = new LinkedList<>();
        for (int i = 0; i < 10; i++) {
            Event event = createEntity();
            em.persist(event);
            em.flush();
            events.add(event);
        }
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
            .startDate(LocalDate.now().atTime(LocalTime.parse("07:00")).toInstant(ZoneOffset.UTC))
            .endDate(LocalDate.now().atTime(LocalTime.parse("20:00")).toInstant(ZoneOffset.UTC));
    }
}
