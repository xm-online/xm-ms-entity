package com.icthh.xm.ms.entity.service;


import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.ms.entity.AbstractSpringBootTest;
import com.icthh.xm.ms.entity.domain.Link;
import com.icthh.xm.ms.entity.domain.Link_;
import com.icthh.xm.ms.entity.security.access.DynamicPermissionCheckService;
import com.icthh.xm.ms.entity.web.rest.LinkResourceIntTest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.LinkedList;
import java.util.List;

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_AUTH_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import static com.icthh.xm.ms.entity.security.access.DynamicPermissionCheckService.FeatureContext.LINK_DELETE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class LinkServiceIntTest extends AbstractSpringBootTest {

    @Autowired
    private LinkService linkService;
    @Autowired
    private EntityManager em;
    @Autowired
    private TenantContextHolder tenantContextHolder;
    @Autowired
    private LepManager lepManager;
    @SpyBean
    private DynamicPermissionCheckService dynamicPermissionCheckService;
    @Autowired
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

    @Test
    @Transactional
    public void testSimpleDelete() {
        when(dynamicPermissionCheckService.isDynamicLinkDeletePermissionEnabled()).thenReturn(false);
        linkService.delete(expected.get(0).getId());
        verify(dynamicPermissionCheckService).isDynamicLinkDeletePermissionEnabled();
        verify(dynamicPermissionCheckService, never()).checkContextPermission(any(), any(), any());
    }

    @Test
    @Transactional
    @WithMockUser(authorities = "SUPER-ADMIN")
    public void testDeleteWithPermissionAssertion() {
        when(dynamicPermissionCheckService.isDynamicLinkDeletePermissionEnabled()).thenReturn(true);
        linkService.delete(expected.get(0).getId());
        verify(dynamicPermissionCheckService, times(2)).isDynamicLinkDeletePermissionEnabled();
        verify(dynamicPermissionCheckService).checkContextPermission(LINK_DELETE, "LINK.DELETE", expected.get(0).getTypeKey());
    }

    @Test(expected = AccessDeniedException.class)
    @Transactional
    @WithMockUser(authorities = "ROLE_ANONYMOUS")
    public void testDeleteWherePermissionDenied() {
        when(dynamicPermissionCheckService.isDynamicLinkDeletePermissionEnabled()).thenReturn(true);
        linkService.delete(expected.get(0).getId());
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
