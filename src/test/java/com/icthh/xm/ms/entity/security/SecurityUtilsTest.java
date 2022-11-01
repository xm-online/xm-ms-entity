package com.icthh.xm.ms.entity.security;

import com.icthh.xm.ms.entity.AbstractSpringBootTest;
import org.junit.Test;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class SecurityUtilsTest extends AbstractSpringBootTest {

    @Test
    @WithMockUser(authorities = "TEST-ROLE")
    public void getCurrentUserRoleFromAuthority() {
        Optional<String> currentUserRole = SecurityUtils.getCurrentUserRole();
        assertThat(currentUserRole).isNotEmpty();
        assertThat(currentUserRole.get()).isEqualTo("TEST-ROLE");
    }

    @Test
    @WithMockUser(roles = "TEST-ROLE")
    public void getCurrentUserRole() {
        Optional<String> currentUserRole = SecurityUtils.getCurrentUserRole();
        assertThat(currentUserRole).isNotEmpty();
        assertThat(currentUserRole.get()).isEqualTo("ROLE_TEST-ROLE");
    }

    @Test
    @WithMockUser("LOGIN")
    public void getCurrentUserLogin() {
        Optional<String> currentUserLogin = SecurityUtils.getCurrentUserLogin();
        assertThat(currentUserLogin).isNotEmpty();
        assertThat(currentUserLogin.get()).isEqualTo("LOGIN");
    }

}
