package com.icthh.xm.ms.entity.web.rest;

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_AUTH_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

import com.icthh.xm.commons.permission.service.PermissionService;
import com.icthh.xm.commons.permission.service.RoleService;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.ms.entity.AbstractJupiterSpringBootTest;
import com.icthh.xm.ms.entity.domain.Link;
import com.icthh.xm.ms.entity.web.rest.facade.LinkFacade;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verifies that the two independent SpEL permission checks around
 * {@link LinkResource#getTargetsByXmEntity} each enforce their own concern:
 * <ul>
 *     <li>the {@code @PreAuthorize} on the REST endpoint - gated on {@code typeKey} passed from the path,</li>
 *     <li>the {@code @FindWithPermission} on {@link com.icthh.xm.ms.entity.service.LinkService#findTargetsByXmEntity}
 *     - gated on role privilege presence and row-level {@code resourceCondition} filtering.</li>
 * </ul>
 */
@Slf4j
public class LinkResourcePermissionIntTest extends AbstractJupiterSpringBootTest {

    private static final String TENANT = "RESINTTEST";
    private static final String TEST_ROLE = "LINK_PERM_TEST_ROLE";
    private static final String ROLES_CONFIG_KEY = "/config/tenants/" + TENANT + "/roles.yml";
    private static final String PERMISSIONS_CONFIG_KEY = "/config/tenants/" + TENANT + "/permissions.yml";

    private static final String API_PRIVILEGE = "LINK.TARGETS.GET_LIST.BY_XM_ENTITY.BY_TYPE_KEY";
    private static final String SERVICE_PRIVILEGE = "LINK.TARGETS.GET_LIST.BY_XM_ENTITY";

    private static final String ROLES_YML = TEST_ROLE + ":\n"
        + "  name:\n"
        + "    en: \"Link permission test role\"\n"
        + "  description: \"role used only by LinkResourcePermissionIntTest\"\n";

    @Autowired
    private LinkResource linkResource;

    @MockitoSpyBean
    private LinkFacade linkFacade;

    @Autowired
    private RoleService roleService;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private EntityManager em;

    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Autowired
    private LepManager lepManager;

    @Autowired
    private XmAuthenticationContextHolder authContextHolder;

    @BeforeTransaction
    public void beforeTransaction() {
        TenantContextUtils.setTenant(tenantContextHolder, TENANT);
    }

    @BeforeEach
    public void setup() {
        lepManager.beginThreadContext(ctx -> {
            ctx.setValue(THREAD_CONTEXT_KEY_TENANT_CONTEXT, tenantContextHolder.getContext());
            ctx.setValue(THREAD_CONTEXT_KEY_AUTH_CONTEXT, authContextHolder.getContext());
        });
        roleService.onRefresh(ROLES_CONFIG_KEY, ROLES_YML);
    }

    @AfterEach
    public void tearDown() {
        roleService.onRefresh(ROLES_CONFIG_KEY, null);
        permissionService.onRefresh(PERMISSIONS_CONFIG_KEY, null);
        lepManager.endThreadContext();
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
    }

    private void pushPermissions(String apiPermissionYml, String servicePermissionYml) {
        StringBuilder yml = new StringBuilder("entity:\n  " + TEST_ROLE + ":\n");
        if (apiPermissionYml != null) {
            yml.append(apiPermissionYml);
        }
        if (servicePermissionYml != null) {
            yml.append(servicePermissionYml);
        }
        permissionService.onRefresh(PERMISSIONS_CONFIG_KEY, yml.toString());
    }

    private static String permissionEntry(String privilegeKey, String resourceCondition) {
        StringBuilder sb = new StringBuilder();
        sb.append("  - privilegeKey: \"").append(privilegeKey).append("\"\n");
        sb.append("    disabled: false\n");
        if (resourceCondition != null) {
            sb.append("    resourceCondition: \"").append(resourceCondition).append("\"\n");
        }
        return sb.toString();
    }

    @Test
    @Transactional
    @WithMockUser(authorities = TEST_ROLE)
    public void apiPreAuthorize_deniesCall_whenTypeKeyConditionDoesNotMatch() {
        Link link = LinkResourceIntTest.createEntity(em);
        em.persist(link);
        em.flush();

        // API layer only allows typeKey == "ALLOWED_TYPE"; service-level privilege is fully open.
        pushPermissions(
            permissionEntry(API_PRIVILEGE, "#typeKey == 'ALLOWED_TYPE'"),
            permissionEntry(SERVICE_PRIVILEGE, null));

        assertThatThrownBy(() ->
            linkResource.getTargetsByXmEntity(link.getSource().getId(), link.getTypeKey(), PageRequest.of(0, 10)))
            .isInstanceOf(AccessDeniedException.class);

        // The controller-level SpEL must reject before the request ever reaches the service/facade layer.
        verifyNoInteractions(linkFacade);
    }

    @Test
    @Transactional
    @WithMockUser(authorities = TEST_ROLE)
    public void apiPreAuthorize_allowsCall_whenTypeKeyConditionMatches_butServiceDeniesMissingPrivilege() {
        Link link = LinkResourceIntTest.createEntity(em);
        em.persist(link);
        em.flush();

        // API layer allows exactly this typeKey; the service-level privilege is intentionally NOT granted at all.
        pushPermissions(
            permissionEntry(API_PRIVILEGE, "#typeKey == '" + link.getTypeKey() + "'"),
            null);

        assertThatThrownBy(() ->
            linkResource.getTargetsByXmEntity(link.getSource().getId(), link.getTypeKey(), PageRequest.of(0, 10)))
            .isInstanceOf(AccessDeniedException.class);

        // The request DID pass the controller gate and reached the service, which denied on its own.
        org.mockito.Mockito.verify(linkFacade)
            .findTargetsByXmEntity(link.getSource().getId(), link.getTypeKey(), PageRequest.of(0, 10), null);
    }

    @Test
    @Transactional
    @WithMockUser(authorities = TEST_ROLE)
    public void bothLayers_allowUnconditionally_returnFullList() {
        Link link = LinkResourceIntTest.createEntity(em);
        em.persist(link);
        em.flush();

        pushPermissions(
            permissionEntry(API_PRIVILEGE, "#typeKey == '" + link.getTypeKey() + "'"),
            permissionEntry(SERVICE_PRIVILEGE, null));

        var response = linkResource.getTargetsByXmEntity(link.getSource().getId(), link.getTypeKey(), PageRequest.of(0, 10));

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).extracting("id").containsExactly(link.getId());
    }

    @Test
    @Transactional
    @WithMockUser(authorities = TEST_ROLE)
    public void servicePrivilege_resourceConditionFiltersRows_independentlyOfApiLayer() {
        Link visibleLink = LinkResourceIntTest.createEntity(em);
        visibleLink.setName("VISIBLE_NAME");
        em.persist(visibleLink);
        em.flush();

        Link hiddenLink = LinkResourceIntTest.createEntity(em);
        hiddenLink.setSource(visibleLink.getSource());
        hiddenLink.setTypeKey(visibleLink.getTypeKey());
        hiddenLink.setName("HIDDEN_NAME");
        em.persist(hiddenLink);
        em.flush();

        // API layer is fully open; the service layer only exposes rows named 'VISIBLE_NAME'.
        pushPermissions(
            permissionEntry(API_PRIVILEGE, null),
            permissionEntry(SERVICE_PRIVILEGE, "#returnObject.name == 'VISIBLE_NAME'"));

        var response = linkResource.getTargetsByXmEntity(
            visibleLink.getSource().getId(), visibleLink.getTypeKey(), PageRequest.of(0, 10));

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).extracting("id").containsExactly(visibleLink.getId());
    }
}
