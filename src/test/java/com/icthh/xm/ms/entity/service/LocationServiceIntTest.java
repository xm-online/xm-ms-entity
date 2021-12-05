package com.icthh.xm.ms.entity.service;


import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.ms.entity.AbstractSpringBootTest;
import com.icthh.xm.ms.entity.domain.Location;
import com.icthh.xm.ms.entity.web.rest.LocationResourceIntTest;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_AUTH_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;

public class LocationServiceIntTest extends AbstractSpringBootTest {

    @Autowired
    private LocationService locationService;
    @Autowired
    private EntityManager em;
    @Autowired
    private TenantContextHolder tenantContextHolder;
    @Autowired
    private LepManager lepManager;
    @Mock
    private XmAuthenticationContextHolder authContextHolder;

    private List<Location> expected;

    @Before
    public void setup() {

        lepManager.beginThreadContext(ctx -> {
            ctx.setValue(THREAD_CONTEXT_KEY_TENANT_CONTEXT, tenantContextHolder.getContext());
            ctx.setValue(THREAD_CONTEXT_KEY_AUTH_CONTEXT, authContextHolder.getContext());
        });

        expected = initLocations();
    }

    @Test
    @Transactional
    public void findByIdsTest() {
        List<Location> actual = locationService.findByIds(expected.stream().map(Location::getId).collect(Collectors.toList()));
        Assert.assertEquals(expected, actual);
    }

    @Test
    @Transactional
    public void findByEntityIdsTest() {
        List<Location> actual = locationService.findByEntityIds(expected.stream().map(location -> location.getXmEntity().getId())
            .collect(Collectors.toList()));
        Assert.assertEquals(expected, actual);
    }

    @BeforeTransaction
    public void beforeTransaction() {
        TenantContextUtils.setTenant(tenantContextHolder, "RESINTTEST");
    }

    @After
    public void tearDown() {
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
    }

    public List<Location> initLocations() {
        List<Location> locations = new LinkedList<>();
        for (int i = 0; i < 10; i++) {
            Location location = LocationResourceIntTest.createEntity(em);
            em.persist(location);
            em.flush();
            locations.add(location);
        }
        return locations;
    }
}
