package com.icthh.xm.ms.entity.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.icthh.xm.commons.lep.api.LepEngineSession;
import com.icthh.xm.commons.lep.api.LepManagementService;
import com.icthh.xm.commons.permission.service.PermissionService;
import com.icthh.xm.commons.permission.service.RoleService;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.ms.entity.AbstractJupiterSpringBootTest;
import com.icthh.xm.ms.entity.domain.Event;
import com.icthh.xm.ms.entity.service.dto.EventDto;
import com.icthh.xm.ms.entity.service.query.filter.EventFilter;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;
import tech.jhipster.service.filter.StringFilter;

/**
 * Verifies that {@link EventResource#getAllEvents} enforces the {@code EVENT.GET_LIST} privilege
 * and its row-level {@code resourceCondition}, also when request filters are passed.
 */
public class EventResourcePermissionIntTest extends AbstractJupiterSpringBootTest {

    private static final String TENANT = "RESINTTEST";
    private static final String TEST_ROLE = "EVENT_PERM_TEST_ROLE";
    private static final String ROLES_CONFIG_KEY = "/config/tenants/" + TENANT + "/roles.yml";
    private static final String PERMISSIONS_CONFIG_KEY = "/config/tenants/" + TENANT + "/permissions.yml";

    private static final String PRIVILEGE = "EVENT.GET_LIST";
    private static final String VISIBLE_TYPE_KEY = "TEST_EVENT_TYPEKEY_1";
    private static final String HIDDEN_TYPE_KEY = "TEST_EVENT_TYPEKEY_2";

    private static final String ROLES_YML = TEST_ROLE + ":\n"
        + "  name:\n"
        + "    en: \"Event permission test role\"\n"
        + "  description: \"role used only by EventResourcePermissionIntTest\"\n";

    @Autowired
    private EventResource eventResource;

    @Autowired
    private RoleService roleService;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private EntityManager em;

    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Autowired
    private LepManagementService lepManagementService;

    private LepEngineSession lepEngineSession;

    @BeforeTransaction
    public void beforeTransaction() {
        TenantContextUtils.setTenant(tenantContextHolder, TENANT);
    }

    @BeforeEach
    public void setup() {
        lepEngineSession = lepManagementService.beginThreadContext();
        roleService.onRefresh(ROLES_CONFIG_KEY, ROLES_YML);
    }

    @AfterEach
    public void tearDown() {
        roleService.onRefresh(ROLES_CONFIG_KEY, null);
        permissionService.onRefresh(PERMISSIONS_CONFIG_KEY, null);
        lepEngineSession.close();
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
    }

    private void pushPermission(String privilegeKey, String resourceCondition) {
        StringBuilder yml = new StringBuilder("entity:\n  " + TEST_ROLE + ":\n");
        if (privilegeKey != null) {
            yml.append("  - privilegeKey: \"").append(privilegeKey).append("\"\n");
            yml.append("    disabled: false\n");
            if (resourceCondition != null) {
                yml.append("    resourceCondition: \"").append(resourceCondition).append("\"\n");
            }
        }
        permissionService.onRefresh(PERMISSIONS_CONFIG_KEY, yml.toString());
    }

    private Event persistEvent(String typeKey) {
        Event event = EventResourceIntTest.createEntity(em);
        event.setTypeKey(typeKey);
        em.persist(event);
        em.flush();
        return event;
    }

    private static EventFilter typeKeyFilter(String typeKey) {
        StringFilter typeKeyFilter = new StringFilter();
        typeKeyFilter.setEquals(typeKey);
        EventFilter filter = new EventFilter();
        filter.setTypeKey(typeKeyFilter);
        return filter;
    }

    @Test
    @Transactional
    @WithMockUser(authorities = TEST_ROLE)
    public void getAllEvents_deniesAccess_withoutPrivilege() {
        persistEvent(VISIBLE_TYPE_KEY);
        pushPermission(null, null);

        assertThatThrownBy(() -> eventResource.getAllEvents(new EventFilter()))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> eventResource.getAllEvents(typeKeyFilter(VISIBLE_TYPE_KEY)))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @Transactional
    @WithMockUser(authorities = TEST_ROLE)
    public void getAllEvents_appliesRequestFilter_withPrivilege() {
        Event visible = persistEvent(VISIBLE_TYPE_KEY);
        Event other = persistEvent(HIDDEN_TYPE_KEY);
        pushPermission(PRIVILEGE, null);

        List<EventDto> all = eventResource.getAllEvents(new EventFilter());
        assertThat(all).extracting(EventDto::getId).contains(visible.getId(), other.getId());

        List<EventDto> filtered = eventResource.getAllEvents(typeKeyFilter(VISIBLE_TYPE_KEY));
        assertThat(filtered).extracting(EventDto::getId).contains(visible.getId()).doesNotContain(other.getId());
    }

    @Test
    @Transactional
    @WithMockUser(authorities = TEST_ROLE)
    public void getAllEvents_appliesResourceCondition() {
        Event visible = persistEvent(VISIBLE_TYPE_KEY);
        Event hidden = persistEvent(HIDDEN_TYPE_KEY);
        pushPermission(PRIVILEGE, "#returnObject.typeKey == '" + VISIBLE_TYPE_KEY + "'");

        List<EventDto> all = eventResource.getAllEvents(new EventFilter());
        assertThat(all).extracting(EventDto::getId).contains(visible.getId()).doesNotContain(hidden.getId());

        List<EventDto> filtered = eventResource.getAllEvents(typeKeyFilter(HIDDEN_TYPE_KEY));
        assertThat(filtered).isEmpty();
    }
}
