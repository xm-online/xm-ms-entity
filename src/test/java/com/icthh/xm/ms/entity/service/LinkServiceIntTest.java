package com.icthh.xm.ms.entity.service;


import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.ms.entity.AbstractSpringBootTest;
import com.icthh.xm.ms.entity.domain.Link;
import com.icthh.xm.ms.entity.domain.Link_;
import com.icthh.xm.ms.entity.web.rest.LinkResourceIntTest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.LinkedList;
import java.util.List;

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_AUTH_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;

public class LinkServiceIntTest extends AbstractSpringBootTest {

    @Autowired
    private LinkService linkService;
    @Autowired
    private EntityManager em;
    @Autowired
    private TenantContextHolder tenantContextHolder;
    @Autowired
    private LepManager lepManager;
    @Mock
    private XmAuthenticationContextHolder authContextHolder;

    private List<Link> expected;

    @Before
    public void setup() {

        lepManager.beginThreadContext(ctx -> {
            ctx.setValue(THREAD_CONTEXT_KEY_TENANT_CONTEXT, tenantContextHolder.getContext());
            ctx.setValue(THREAD_CONTEXT_KEY_AUTH_CONTEXT, authContextHolder.getContext());
        });

        expected = initLinks();
    }


    @Test
    @Transactional
    public void findAllTest() {
        Page<Link> actual = linkService.findAll(Specification.where((root, query, criteriaBuilder)
            -> criteriaBuilder.isNotNull(root.get(Link_.id))), PageRequest.of(0, expected.size()));
        Assert.assertNotNull(actual);
        Assert.assertEquals(expected, actual.getContent());
    }

    @BeforeTransaction
    public void beforeTransaction() {
        TenantContextUtils.setTenant(tenantContextHolder, "RESINTTEST");
    }

    public List<Link> initLinks() {
        List<Link> locations = new LinkedList<>();
        for (int i = 0; i < 10; i++) {
            Link link = LinkResourceIntTest.createEntity(em);
            em.persist(link);
            em.flush();
            locations.add(link);
        }
        return locations;
    }
}
