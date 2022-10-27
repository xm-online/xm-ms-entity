package com.icthh.xm.ms.entity;

import org.junit.experimental.categories.Category;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;

import java.util.List;

import static org.mockito.Mockito.when;

/**
 * Abstract test for extension for any Unit test.
 * Marks test with junit @Category
 */
@Category(AbstractUnitTest.class)
public abstract class AbstractUnitTest {

    public static void withMockUser(OAuth2Authentication auth, String roleKey, Runnable function) {
        when(auth.getAuthorities()).thenReturn(List.of(new SimpleGrantedAuthority(roleKey)));
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        try {
            SecurityContextHolder.getContext().setAuthentication(auth);
            function.run();
        } finally {
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
    }

}
