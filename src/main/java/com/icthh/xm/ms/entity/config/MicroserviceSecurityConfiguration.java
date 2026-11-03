package com.icthh.xm.ms.entity.config;

import com.icthh.xm.commons.permission.access.XmPermissionEvaluator;
import com.icthh.xm.commons.security.XmPermissionCheckService;
import com.icthh.xm.commons.security.jwt.TokenProvider;
import com.icthh.xm.commons.security.spring.config.SecurityConfiguration;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

@Configuration
public class MicroserviceSecurityConfiguration extends SecurityConfiguration {

    public MicroserviceSecurityConfiguration(TokenProvider tokenProvider,
                                             @Value("${jhipster.security.content-security-policy}")
                                             String contentSecurityPolicy,
                                             XmPermissionCheckService permissionCheckService) {
        super(tokenProvider, contentSecurityPolicy, permissionCheckService);
    }

    @Override
    @SneakyThrows
    protected HttpSecurity applyUrlSecurity(HttpSecurity http) {
        http.authorizeHttpRequests(auth ->
            auth.requestMatchers("/api/xm-entities/registration").permitAll()
                .requestMatchers("/api/functions/anonymous/**").permitAll()
                .requestMatchers("/public/**").permitAll()
                .requestMatchers("/api/functions/api-docs").permitAll()
                .requestMatchers("/api/xm-entities/registration/activate/*").permitAll()
        );
        return super.applyUrlSecurity(http);
    }

    @Primary
    @Bean
    static MethodSecurityExpressionHandler expressionHandler(XmPermissionEvaluator customPermissionEvaluator) {
        DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();
        expressionHandler.setPermissionEvaluator(customPermissionEvaluator);
        return expressionHandler;
    }
}
