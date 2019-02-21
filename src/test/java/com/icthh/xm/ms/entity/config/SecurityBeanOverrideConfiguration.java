package com.icthh.xm.ms.entity.config;

import static org.mockito.Mockito.mock;

import org.springframework.cloud.client.loadbalancer.RestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;

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

}
