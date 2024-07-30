package com.icthh.xm.ms.entity.config;

import static com.icthh.xm.commons.config.client.config.XmRestTemplateConfiguration.XM_CONFIG_REST_TEMPLATE;
import static org.mockito.Mockito.mock;

import com.icthh.xm.commons.security.jwt.TokenProvider;
import jakarta.annotation.PostConstruct;
import org.springframework.cloud.client.loadbalancer.RestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

/**
 * Overrides UAA specific beans, so they do not interfere the testing
 * This configuration must be included in @SpringBootTest in order to take effect.
 */
@Configuration
public class SecurityBeanOverrideConfiguration {

//    @Bean
//    @Primary
//    public TokenStore tokenStore() {
//        return mock(TokenStore.class);
//    }
//
//    @Bean
//    @Primary
//    public JwtAccessTokenConverter jwtAccessTokenConverter() {
//        return mock(JwtAccessTokenConverter.class);
//    }

    @Bean
    @Primary
    public TokenProvider tokenProvider() {
        return mock(TokenProvider.class);
    }

    @Bean
    @Primary
    public RestTemplate loadBalancedRestTemplate(RestTemplateCustomizer customizer) {
        return mock(RestTemplate.class);
    }

    @Bean(XM_CONFIG_REST_TEMPLATE)
    public RestTemplate restTemplate() {
        return mock(RestTemplate.class);
    }

    @PostConstruct
    public void postConstruct(){
        // Need system error to see when contect is recreated during dunning test from gradle
        System.err.println(" ===================== SecurityBeanOverrideConfiguration inited !!! =======================");
    }

}
