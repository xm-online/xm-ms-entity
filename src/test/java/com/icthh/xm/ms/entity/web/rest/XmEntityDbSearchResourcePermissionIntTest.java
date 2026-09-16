package com.icthh.xm.ms.entity.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.icthh.xm.commons.permission.service.PermissionService;
import com.icthh.xm.commons.permission.service.RoleService;
import com.icthh.xm.ms.entity.AbstractPostgresIntTest;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
import com.icthh.xm.ms.entity.service.dto.XmEntityDto;
import com.icthh.xm.ms.entity.service.search.db.dto.XmEntityDbSearchRequest;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

/**
 * API-level {@code @PreAuthorize} (typeKey/query/filter in the resource map) and service-level
 * {@code @FindWithPermission} (privilege presence, row-level resourceCondition) are verified independently.
 */
@Transactional
public class XmEntityDbSearchResourcePermissionIntTest extends AbstractPostgresIntTest {

    private static final String TEST_ROLE = "DB_SEARCH_PERM_ROLE";
    private static final String ROLES_CONFIG_KEY = "/config/tenants/" + TENANT + "/roles.yml";
    private static final String PERMISSIONS_CONFIG_KEY = "/config/tenants/" + TENANT + "/permissions.yml";
    private static final String API_PRIVILEGE = "XMENTITY.SEARCH.DB.QUERY";
    private static final String SERVICE_PRIVILEGE = "XMENTITY.SEARCH.DB";
    private static final String ROLES_YML = TEST_ROLE + ":\n  name:\n    en: \"DB search permission test role\"\n";

    @Autowired private XmEntityDbSearchResource resource;
    @Autowired private RoleService roleService;
    @Autowired private PermissionService permissionService;
    @Autowired private XmEntityRepository repository;

    private XmEntity mine;
    private XmEntity other;

    @BeforeEach
    public void setup() {
        pushDbSearchSpec();
        roleService.onRefresh(ROLES_CONFIG_KEY, ROLES_YML);
        mine = repository.save(newEntity("ORDER", "mine", Map.of()).createdBy("user-1"));
        other = repository.save(newEntity("ORDER", "other", Map.of()).createdBy("user-2"));
    }

    @AfterEach
    public void cleanup() {
        roleService.onRefresh(ROLES_CONFIG_KEY, null);
        permissionService.onRefresh(PERMISSIONS_CONFIG_KEY, null);
    }

    private void pushPermissions(String apiCondition, boolean grantApi, String serviceCondition, boolean grantService) {
        StringBuilder yml = new StringBuilder("entity:\n  " + TEST_ROLE + ":\n");
        if (grantApi) {
            yml.append(entry(API_PRIVILEGE, apiCondition));
        }
        if (grantService) {
            yml.append(entry(SERVICE_PRIVILEGE, serviceCondition));
        }
        permissionService.onRefresh(PERMISSIONS_CONFIG_KEY, yml.toString());
    }

    private static String entry(String privilegeKey, String resourceCondition) {
        StringBuilder sb = new StringBuilder("  - privilegeKey: \"" + privilegeKey + "\"\n    disabled: false\n");
        if (resourceCondition != null) {
            sb.append("    resourceCondition: \"").append(resourceCondition).append("\"\n");
        }
        return sb.toString();
    }

    private static XmEntityDbSearchRequest order() {
        XmEntityDbSearchRequest r = new XmEntityDbSearchRequest();
        r.setTypeKey("ORDER");
        return r;
    }

    @Test
    @WithMockUser(authorities = TEST_ROLE)
    public void apiConditionOnTypeKeyDenies() {
        pushPermissions("#typeKey == 'PRODUCT'", true, null, true);
        assertThatThrownBy(() -> resource.searchPost(order(), PageRequest.of(0, 10))).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(authorities = TEST_ROLE)
    public void missingServicePrivilegeDenies() {
        pushPermissions(null, true, null, false);
        assertThatThrownBy(() -> resource.searchPost(order(), PageRequest.of(0, 10))).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(authorities = TEST_ROLE)
    public void rowLevelConditionFiltersResults() {
        pushPermissions(null, true, "#returnObject.createdBy == 'user-1'", true);
        var response = resource.searchPost(order(), PageRequest.of(0, 10));
        assertThat(response.getBody()).extracting(XmEntityDto::getId).containsExactly(mine.getId());
        assertThat(response.getHeaders().getFirst("X-Total-Count")).isEqualTo("1");
    }

    @Test
    @WithMockUser(authorities = TEST_ROLE)
    public void unconditionalGrantReturnsAll() {
        pushPermissions(null, true, null, true);
        assertThat(resource.searchPost(order(), PageRequest.of(0, 10)).getBody())
            .extracting(XmEntityDto::getId).containsExactlyInAnyOrder(mine.getId(), other.getId());
    }
}
