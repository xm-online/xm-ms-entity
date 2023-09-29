package com.icthh.xm.ms.entity.repository;

import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.ms.entity.AbstractSpringBootTest;
import com.icthh.xm.ms.entity.domain.Location;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.service.SeparateTransactionExecutor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.stat.Statistics;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.stream.Collectors;

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepScriptConstants.BINDING_KEY_AUTH_CONTEXT;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.junit.Assert.assertEquals;

@Transactional
@Slf4j
public class LocationRepositoryIntTest extends AbstractSpringBootTest {

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    XmEntityRepository xmEntityRepository;

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

    private static final String LOCATION_TYPE_KEY = "LOCATION1";

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
    public void findAllByXmEntityIdIn() {
        List<Long> xmEntityIds = separateTransactionExecutor.doInSeparateTransaction(() -> {
            List<XmEntity> savedXmEntities = xmEntityRepository.saveAll(List.of(createXmEntity(), createXmEntity()));
            List<Location> locations = List.of(
                createLocation("Location 1", savedXmEntities.get(0)),
                createLocation("Location 2", savedXmEntities.get(1))
            );
            locationRepository.saveAll(locations);
            return savedXmEntities.stream().map(XmEntity::getId).collect(Collectors.toList());
        });
        statistics.clear();

        locationRepository.findAllByXmEntityIdIn(xmEntityIds);

        assertEquals(1, statistics.getPrepareStatementCount());
    }

    @Test
    public void findAllByIdIn() {
        List<Long> locationIds = separateTransactionExecutor.doInSeparateTransaction(() -> {
            List<Location> locations = List.of(
                createLocation("Location 1", createAndSaveXmEntity()),
                createLocation("Location 2", createAndSaveXmEntity())
            );
            return locationRepository.saveAll(locations).stream().map(Location::getId).collect(Collectors.toList());
        });
        statistics.clear();

        locationRepository.findAllByIdIn(locationIds);

        assertEquals(1, statistics.getPrepareStatementCount());
    }

    private XmEntity createAndSaveXmEntity() {
        return xmEntityRepository.save(createXmEntity());
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

    private Location createLocation(String name, XmEntity xmEntity) {
        Location location = new Location();
        location.setTypeKey(LOCATION_TYPE_KEY);
        location.setName(name);
        location.setXmEntity(xmEntity);
        return location;
    }
}
