package com.icthh.xm.ms.entity.service;


import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.ms.entity.AbstractJupiterSpringBootTest;
import com.icthh.xm.ms.entity.domain.Link;
import com.icthh.xm.ms.entity.domain.Link_;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
import com.icthh.xm.ms.entity.security.access.XmEntityDynamicPermissionCheckService;
import com.icthh.xm.ms.entity.web.rest.LinkResourceIntTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedList;
import java.util.List;

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_AUTH_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import static com.icthh.xm.ms.entity.security.access.FeatureContext.LINK_DELETE;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    @MockitoSpyBean
    private XmEntityDynamicPermissionCheckService dynamicPermissionCheckService;
    @Autowired
    private XmAuthenticationContextHolder authContextHolder;
    @Autowired
    private XmEntityRepository xmEntityRepository;

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
        assertEquals(expected.size(), actual.getContent().size());
        Assertions.assertTrue(actual.getContent().containsAll(expected));
    }

    @Test
    @Transactional
    public void testSimpleDelete() {
        when(dynamicPermissionCheckService.isDynamicLinkDeletePermissionEnabled()).thenReturn(false);
        linkService.delete(expected.getFirst().getId());
        verify(dynamicPermissionCheckService).isDynamicLinkDeletePermissionEnabled();
        verify(dynamicPermissionCheckService, never()).checkContextPermission(any(), any(), any());
    }

    @Test
    @Transactional
    @WithMockUser(authorities = "SUPER-ADMIN")
    public void testDeleteWithPermissionAssertion() {
        when(dynamicPermissionCheckService.isDynamicLinkDeletePermissionEnabled()).thenReturn(true);
        linkService.delete(expected.getFirst().getId());
        verify(dynamicPermissionCheckService, times(2)).isDynamicLinkDeletePermissionEnabled();
        verify(dynamicPermissionCheckService).checkContextPermission(LINK_DELETE, "LINK.DELETE", expected.getFirst().getTypeKey());
    }

    @Test
    @Transactional
    @WithMockUser(authorities = "ROLE_ANONYMOUS")
    public void testDeleteWherePermissionDenied() {
        assertThrows(AccessDeniedException.class, () -> {
            when(dynamicPermissionCheckService.isDynamicLinkDeletePermissionEnabled()).thenReturn(true);
            linkService.delete(expected.getFirst().getId());
        });
    }

    @Test
    @Transactional
    void deleteInBatchShouldDeleteLinks() {
        List<Link> linksToDelete = expected.subList(0, 2);

        linkService.deleteInBatch(linksToDelete);

        List<Link> actual = linkService.findAll(
            Specification.where((root, query, cb) ->
                cb.isNotNull(root.get(Link_.id))),
            PageRequest.of(0, expected.size())
        ).getContent();

        assertEquals(expected.size() - linksToDelete.size(), actual.size());
        assertFalse(actual.containsAll(linksToDelete));
    }

    @Test
    @Transactional
    void deleteInBatchShouldDetachDeletedLinks() {
        Link linkToDelete = expected.getFirst();
        XmEntity targetToDelete = linkToDelete.getTarget();

        assertTrue(em.contains(linkToDelete));
        assertTrue(em.contains(targetToDelete));

        linkService.deleteInBatch(List.of(linkToDelete));

        assertFalse(em.contains(linkToDelete));

        xmEntityRepository.deleteAll(List.of(targetToDelete));

        assertDoesNotThrow(() -> em.flush());
    }

    @Test
    @Transactional
    void deleteInBatchShouldNotFailOnNextQueryAfterTargetDeletion() {
        Link linkToDelete = expected.getFirst();
        XmEntity targetToDelete = linkToDelete.getTarget();

        linkService.deleteInBatch(List.of(linkToDelete));

        xmEntityRepository.deleteAll(List.of(targetToDelete));

        assertDoesNotThrow(() ->
            linkService.findAll(
                Specification.where((root, query, cb) ->
                    cb.isNotNull(root.get(Link_.id))),
                PageRequest.of(0, 10)
            )
        );
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
