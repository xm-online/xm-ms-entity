package com.icthh.xm.ms.entity.util;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.function.Consumer;

public class AuthTokenUtils {

    public static String TEST_PRINCIPAL = "admin";
    public static String TEST_CRED = "admin";
    public static String TEST_SUPER_ADMIN_ROLE = "SUPER-ADMIN";

    public static UsernamePasswordAuthenticationToken newToken() {
        return newToken(e -> {});
    }

    public static UsernamePasswordAuthenticationToken newToken(Consumer<UsernamePasswordAuthenticationToken> auth) {
        UsernamePasswordAuthenticationToken a = new UsernamePasswordAuthenticationToken(
            TEST_PRINCIPAL,
            TEST_CRED,
            List.of(new SimpleGrantedAuthority(TEST_SUPER_ADMIN_ROLE))
        );
        auth.accept(a);
        return a;
    }
}
