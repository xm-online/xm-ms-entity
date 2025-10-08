package com.icthh.xm.ms.entity.service;


import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.ms.entity.AbstractJupiterSpringBootTest;
import com.icthh.xm.ms.entity.domain.Link;
import com.icthh.xm.ms.entity.domain.Link_;
import com.icthh.xm.ms.entity.security.access.XmEntityDynamicPermissionCheckService;
import com.icthh.xm.ms.entity.web.rest.LinkResourceIntTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedList;
import java.util.List;

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_AUTH_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import static com.icthh.xm.ms.entity.security.access.FeatureContext.LINK_DELETE;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LinkServiceIntTest extends AbstractJupiterSpringBootTest {

    @Autowired
    private LinkService linkService;
    @Autowired
    private EntityManager em;
    @Autowired
    private TenantContextHolder tenantContextHolder;
    @Autowired
    private LepManager lepManager;
    @SpyBean
    private XmEntityDynamicPermissionCheckService dynamicPermissionCheckService;
    @Autowired
    private XmAuthenticationContextHolder authContextHolder;

    private List<Link> expected;

    @BeforeEach
    public void setup() {

        lepManager.beginThreadContext(ctx -> {
            ctx.setValue(THREAD_CONTEXT_KEY_TENANT_CONTEXT, tenantContextHolder.getContext());
            ctx.setValue(THREAD_CONTEXT_KEY_AUTH_CONTEXT, authContextHolder.getContext());
        });

        expected = initLinks();
    }

    @AfterEach
    public void tearDown() {
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
    }

    @Test
    @Transactional
    public void findAllTest() {
        Page<Link> actual = linkService.findAll(Specification.where((root, query, criteriaBuilder)
            -> criteriaBuilder.isNotNull(root.get(Link_.id))), PageRequest.of(0, expected.size()));
        Assertions.assertNotNull(actual);
        Assertions.assertEquals(expected.size(), actual.getContent().size());
        Assertions.assertTrue(actual.getContent().containsAll(expected));
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

    @Test
    @Transactional
    @WithMockUser(authorities = "ROLE_ANONYMOUS")
    public void testDeleteWherePermissionDenied() {
        assertThrows(AccessDeniedException.class, () -> {
            when(dynamicPermissionCheckService.isDynamicLinkDeletePermissionEnabled()).thenReturn(true);
            linkService.delete(expected.get(0).getId());
        });
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
