package com.icthh.xm.ms.entity.client;

import static com.icthh.xm.commons.tenant.TenantContextUtils.getRequiredTenantKeyValue;

import com.icthh.xm.commons.security.XmAuthenticationContext;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.security.spring.config.XmAuthenticationContextConfiguration;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.XmTenantConstants;
import com.icthh.xm.commons.tenant.spring.config.TenantContextConfiguration;
import com.icthh.xm.ms.entity.config.ApplicationProperties;
import feign.Request;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.Optional;

@Configuration
@Import(value = {
    TenantContextConfiguration.class,
    XmAuthenticationContextConfiguration.class
})
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
     *
     * @return requestInterceptor
     */
    @Bean(name = "oauth2RequestInterceptor")
    public RequestInterceptor requestTokenBearerInterceptor(XmAuthenticationContextHolder authContextHolder,
                                                            TenantContextHolder tenantContextHolder) {
        return requestTemplate -> {

            // set token
            XmAuthenticationContext authContext = authContextHolder.getContext();
            Optional<String> tokenValue = authContext.getTokenValue();
            Optional<String> tokenType = authContext.getTokenType();
            if (!tokenValue.isPresent() || !tokenType.isPresent()) {
                throw new IllegalStateException("Authentication not initialized yet, can't create feign request interceptor");
            }
            requestTemplate.header("Authorization", tokenType.get() + " " + tokenValue.get());

            // set tenant name
            requestTemplate.header(XmTenantConstants.HTTP_HEADER_TENANT_NAME,
                                   getRequiredTenantKeyValue(tenantContextHolder));
        };
    }

}
