package com.icthh.xm.ms.entity.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.icthh.xm.commons.lep.api.LepEngineSession;
import com.icthh.xm.commons.lep.api.LepManagementService;
import com.icthh.xm.commons.permission.service.PermissionService;
import com.icthh.xm.commons.permission.service.RoleService;
import com.icthh.xm.commons.security.XmAuthenticationConstants;
import com.icthh.xm.commons.security.internal.XmAuthenticationDetails;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.ms.entity.AbstractJupiterSpringBootTest;
import com.icthh.xm.ms.entity.domain.Profile;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.repository.ProfileRepository;
import com.icthh.xm.ms.entity.service.dto.XmEntityDto;
import com.icthh.xm.ms.entity.service.mapper.XmEntityMapper;
import jakarta.persistence.EntityManager;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verifies that {@link ProfileResource#updateProfile} checks {@code ENTITY.PROFILE.SELF.UPDATE} before the profile
 * data is saved.
 */
public class ProfileResourcePermissionIntTest extends AbstractJupiterSpringBootTest {

    private static final String TENANT = "RESINTTEST";
    private static final String TEST_ROLE = "PROFILE_PERM_TEST_ROLE";
    private static final String USER_KEY = "PROFILE_PERM_TEST_USER";
    private static final String ROLES_CONFIG_KEY = "/config/tenants/" + TENANT + "/roles.yml";
    private static final String PERMISSIONS_CONFIG_KEY = "/config/tenants/" + TENANT + "/permissions.yml";
    private static final String PRIVILEGE = "ENTITY.PROFILE.SELF.UPDATE";
    // required by the data spec of the test entity type
    private static final String FIELD = "AAAAAAAAAA";

    private static final String ROLES_YML = TEST_ROLE + ":\n"
        + "  name:\n"
        + "    en: \"Profile permission test role\"\n"
        + "  description: \"role used only by ProfileResourcePermissionIntTest\"\n";

    @Autowired
    private ProfileResource profileResource;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private XmEntityMapper xmEntityMapper;

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

        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(USER_KEY, "",
            List.of(new SimpleGrantedAuthority(TEST_ROLE)));
        token.setDetails(userDetails());
        SecurityContextHolder.getContext().setAuthentication(token);
    }

    @AfterEach
    public void tearDown() {
        SecurityContextHolder.clearContext();
        roleService.onRefresh(ROLES_CONFIG_KEY, null);
        permissionService.onRefresh(PERMISSIONS_CONFIG_KEY, null);
        lepEngineSession.close();
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
    }

    @Test
    @Transactional
    public void shouldNotUpdateProfileWithoutPrivilege() {
        createProfile();
        pushPermission(null, null);

        assertThatThrownBy(() -> profileResource.updateProfile(profileUpdate("changed")))
            .isInstanceOf(AccessDeniedException.class);

        assertThat(profileData()).containsEntry(FIELD, "initial");
    }

    @Test
    @Transactional
    public void shouldUpdateProfileWithPrivilege() {
        createProfile();
        pushPermission(PRIVILEGE, null);

        profileResource.updateProfile(profileUpdate("changed"));

        assertThat(profileData()).containsEntry(FIELD, "changed");
    }

    @Test
    @Transactional
    public void shouldCheckResourceConditionOnRequestedProfile() {
        createProfile();
        pushPermission(PRIVILEGE, "#xmEntity.data['" + FIELD + "'] != 'forbidden'");

        assertThatThrownBy(() -> profileResource.updateProfile(profileUpdate("forbidden")))
            .isInstanceOf(AccessDeniedException.class);
        assertThat(profileData()).containsEntry(FIELD, "initial");

        profileResource.updateProfile(profileUpdate("allowed"));
        assertThat(profileData()).containsEntry(FIELD, "allowed");
    }

    private void createProfile() {
        XmEntity entity = XmEntityResourceIntTest.createEntity().key(USER_KEY);
        entity.setData(new HashMap<>(Map.of(FIELD, "initial")));
        em.persist(entity);
        Profile profile = new Profile().userKey(USER_KEY);
        profile.setXmentity(entity);
        profileRepository.save(profile);
        em.flush();
    }

    private XmEntityDto profileUpdate(String value) {
        XmEntityDto dto = xmEntityMapper.toDto(profileRepository.findOneByUserKey(USER_KEY).getXmentity());
        dto.setData(new HashMap<>(Map.of(FIELD, value)));
        return dto;
    }

    private Map<String, Object> profileData() {
        return profileRepository.findOneByUserKey(USER_KEY).getXmentity().getData();
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

    private static XmAuthenticationDetails userDetails() {
        XmAuthenticationDetails details = mock(XmAuthenticationDetails.class);
        when(details.getDecodedDetails()).thenReturn(
            new HashMap<>(Map.of(XmAuthenticationConstants.AUTH_DETAILS_USER_KEY, USER_KEY)));
        return details;
    }
}
