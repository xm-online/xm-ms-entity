package com.icthh.xm.ms.entity.config;

import org.springframework.cloud.client.loadbalancer.RestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.Arrays;

import static org.mockito.Mockito.mock;

/**
 * Overrides UAA specific beans, so they do not interfere the testing
 * This configuration must be included in @SpringBootTest in order to take effect.
 */
@Configuration
public class SecurityBeanOverrideConfiguration {

    @Bean
    @Primary
    public TokenStore tokenStore() {
        return mock(TokenStore.class);
    }

    @Bean
    @Primary
    public JwtAccessTokenConverter jwtAccessTokenConverter() {
        return mock(JwtAccessTokenConverter.class);
    }

    @Bean
    @Primary
    public RestTemplate loadBalancedRestTemplate(RestTemplateCustomizer customizer) {
        return mock(RestTemplate.class);
    }

    @PostConstruct
    public void postConstruct(){
        // Need system error to see when contect is recreated during dunning test from gradle
        System.err.println(" ===================== SecurityBeanOverrideConfiguration inited !!! =======================");
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return new InMemoryUserDetailsManager(Arrays.asList(
            newUserDetails("admin"),
            newUserDetails("user"),
            newUserDetails("agent"),
            newUserDetails("anonymous")
        ));
    }

    private UserDetails newUserDetails(String userName) {
        GrantedAuthority anonymous = new SimpleGrantedAuthority(userName.toUpperCase());
        return new User(userName, "password", Arrays.asList(anonymous));
    }

}
