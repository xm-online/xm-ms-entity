package com.icthh.xm.ms.entity.config;

import com.icthh.xm.commons.permission.access.XmPermissionEvaluator;
import com.icthh.xm.commons.permission.constants.RoleConstant;
import com.icthh.xm.commons.security.jwt.JWTConfigurer;
import com.icthh.xm.commons.security.jwt.TokenProvider;
import com.icthh.xm.commons.security.spring.config.UnauthorizedEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@RequiredArgsConstructor
@Configuration
@EnableMethodSecurity(securedEnabled = true)
public class MicroserviceSecurityConfiguration {

    private final TokenProvider tokenProvider;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .headers(headers -> headers.
                frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
            )
            .sessionManagement((session) -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth ->
                auth.requestMatchers("/api/profile-info").permitAll()
                    .requestMatchers("/api/xm-entities/registration").permitAll()
                    .requestMatchers("/api/functions/anonymous/**").permitAll()
                    .requestMatchers("/public/**").permitAll()
                    .requestMatchers("/api/functions/api-docs").permitAll()
                    .requestMatchers("/api/xm-entities/registration/activate/*").permitAll()
                    .requestMatchers("/api/**").authenticated()
                    .requestMatchers("/management/health").permitAll()
                    .requestMatchers("/management/prometheus/**").permitAll()
                    .requestMatchers("/management/**").hasAuthority(RoleConstant.SUPER_ADMIN)
                    .requestMatchers("/swagger-resources/configuration/ui").permitAll()
            );
        http.with(securityConfigurerAdapter(), Customizer.withDefaults());
        http.exceptionHandling(handler -> handler.authenticationEntryPoint(new UnauthorizedEntryPoint()));
        return http.build();
    }

    private JWTConfigurer securityConfigurerAdapter() {
        return new JWTConfigurer(tokenProvider);
    }

    @Primary
    @Bean
    static MethodSecurityExpressionHandler expressionHandler(XmPermissionEvaluator customPermissionEvaluator) {
        DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();
        expressionHandler.setPermissionEvaluator(customPermissionEvaluator);
        return expressionHandler;
    }
}
