package com.icthh.xm.ms.entity.config;

import org.springframework.cloud.client.loadbalancer.LoadBalancerAutoConfiguration;
import org.springframework.cloud.client.loadbalancer.RestTemplateCustomizer;
import org.springframework.cloud.client.loadbalancer.RetryLoadBalancerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.ArrayList;

@Configuration
public class RetryInterceptorConfiguration extends LoadBalancerAutoConfiguration.RetryInterceptorAutoConfiguration {

    @Bean
    @Profile("nolb")
    public RestTemplateCustomizer restTemplateCustomizer(final RetryLoadBalancerInterceptor loadBalancerInterceptor) {
        return restTemplate -> restTemplate.setInterceptors(new ArrayList<>(restTemplate.getInterceptors()));
    }
}
