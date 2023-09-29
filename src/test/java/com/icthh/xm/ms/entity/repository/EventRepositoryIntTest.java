package com.icthh.xm.ms.entity.repository;

import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.ms.entity.AbstractSpringBootTest;
import com.icthh.xm.ms.entity.domain.Calendar;
import com.icthh.xm.ms.entity.domain.Event;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.service.SeparateTransactionExecutor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.stat.Statistics;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepScriptConstants.BINDING_KEY_AUTH_CONTEXT;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.junit.Assert.assertEquals;

@Transactional
@Slf4j
public class EventRepositoryIntTest extends AbstractSpringBootTest {

    @Autowired
    EventRepository eventRepository;

    @Autowired
    XmEntityRepository xmEntityRepository;

    @Autowired
    CalendarRepository calendarRepository;

    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Autowired
    private LepManager lepManager;

    @Autowired
    private XmAuthenticationContextHolder xmAuthenticationContextHolder;

    @PersistenceContext
    public EntityManager entityManager;

    private Statistics statistics;

    private Session session;

    @Autowired
    private SeparateTransactionExecutor separateTransactionExecutor;

    private static final String ENTITY_TYPE_KEY = "TYPE1.SUBTYPE1";
    private static final String CALENDAR_TYPE_KEY = "DEFAULT";
    private static final String EVENT_TYPE_KEY = "EVENT1";

    @Before
    public void setUp() {
        session = entityManager.unwrap(Session.class);
        statistics = session.getSessionFactory().getStatistics();
        statistics.clear();
    }

    @BeforeTransaction
    public void beforeTransaction() {
        TenantContextUtils.setTenant(tenantContextHolder, "TEST");
        lepManager.beginThreadContext(scopedContext -> {
            scopedContext.setValue(THREAD_CONTEXT_KEY_TENANT_CONTEXT, tenantContextHolder.getContext());
            scopedContext.setValue(BINDING_KEY_AUTH_CONTEXT, xmAuthenticationContextHolder.getContext());
        });
    }

    @After
    public void tearDown() {
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
        lepManager.endThreadContext();
    }

    @Test
    public void findAll() {
        separateTransactionExecutor.doInSeparateTransaction(() -> {
            List<XmEntity> xmEntities = xmEntityRepository.saveAll(List.of(createXmEntity(), createXmEntity()));
            List<Event> events = List.of(
                createEvent("Event 1", createCalendar("Calendar 1", xmEntities.get(0)), xmEntities.get(0)),
                createEvent("Event 2", createCalendar("Calendar 2", xmEntities.get(1)), xmEntities.get(1))
            );
            eventRepository.saveAll(events);
            return null;
        });
        statistics.clear();

        eventRepository.findAll(Specification.where((root, query, cb) -> {
            return cb.equal(root.get("typeKey"), EVENT_TYPE_KEY);
        }), PageRequest.of(0, 10));

        assertEquals(1, statistics.getPrepareStatementCount());
    }

    private XmEntity createXmEntity() {
        XmEntity xmEntity = new XmEntity()
            .typeKey(ENTITY_TYPE_KEY)
            .key(randomUUID())
            .name("name")
            .startDate(now())
            .updateDate(now());
        return xmEntity;
    }

    private Calendar createCalendar(String name, XmEntity entity) {
        Calendar calendar = new Calendar();
        calendar.setTypeKey(CALENDAR_TYPE_KEY);
        calendar.setName(name);
        calendar.setXmEntity(entity);
        return calendarRepository.save(calendar);
    }

    private Event createEvent(String title, Calendar calendar, XmEntity assigned) {
        Event event = new Event();
        event.typeKey(EVENT_TYPE_KEY);
        event.setTitle(title);
        event.setCalendar(calendar);
        event.assigned(assigned);

        return event;
    }
}
