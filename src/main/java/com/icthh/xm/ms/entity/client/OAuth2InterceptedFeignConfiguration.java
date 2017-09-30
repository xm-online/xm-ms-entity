package com.icthh.xm.ms.entity.client;

import com.icthh.xm.ms.entity.config.ApplicationProperties;
import com.icthh.xm.ms.entity.config.Constants;
import com.icthh.xm.ms.entity.config.tenant.TenantContext;
import feign.Request;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;

@Configuration
public class OAuth2InterceptedFeignConfiguration {

    @Bean
    public Request.Options options(ApplicationProperties applicationProperties) {
        return new Request.Options(
            applicationProperties.getTenantClientConnectionTimeout(),
            applicationProperties.getTenantClientReadTimeout()
        );
    }

    /**
     * Create request interceptor for feign client.
     * @return requestInterceptor
     */
    @Bean(name = "oauth2RequestInterceptor")
    public RequestInterceptor requestTokenBearerInterceptor() {
        return requestTemplate -> {
            OAuth2AuthenticationDetails details = (OAuth2AuthenticationDetails) SecurityContextHolder.getContext()
                .getAuthentication().getDetails();

            requestTemplate.header("Authorization", "bearer " + details.getTokenValue());
            requestTemplate.header(Constants.HEADER_TENANT, TenantContext.getCurrent().getTenant());
        };
    }
}
