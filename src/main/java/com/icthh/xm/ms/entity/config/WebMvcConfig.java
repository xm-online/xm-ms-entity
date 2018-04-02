package com.icthh.xm.ms.entity.config;

import com.icthh.xm.commons.lep.spring.web.LepInterceptor;
import com.icthh.xm.commons.web.spring.TenantInterceptor;
import com.icthh.xm.commons.web.spring.TenantVerifyInterceptor;
import com.icthh.xm.commons.web.spring.XmLoggingInterceptor;
import com.icthh.xm.commons.web.spring.config.XmMsWebConfiguration;
import com.icthh.xm.commons.web.spring.config.XmWebMvcConfigurerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;

import java.util.List;

@Configuration
@Import( {
    XmMsWebConfiguration.class
})
public class WebMvcConfig extends XmWebMvcConfigurerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebMvcConfig.class);

    private final ApplicationProperties applicationProperties;
    private final LepInterceptor lepInterceptor;
    private final TenantVerifyInterceptor tenantVerifyInterceptor;

    public WebMvcConfig(
                    TenantInterceptor tenantInterceptor,
                    XmLoggingInterceptor xmLoggingInterceptor,
                    ApplicationProperties applicationProperties,
                    LepInterceptor lepInterceptor,
                    TenantVerifyInterceptor tenantVerifyInterceptor) {
        super(tenantInterceptor, xmLoggingInterceptor);

        this.applicationProperties = applicationProperties;
        this.lepInterceptor = lepInterceptor;
        this.tenantVerifyInterceptor = tenantVerifyInterceptor;
    }

    @Override
    protected void xmAddInterceptors(InterceptorRegistry registry) {
        registerLepInterceptor(registry);
        registerTenantInterceptorWithIgnorePathPattern(registry, tenantVerifyInterceptor);
    }

    private void registerLepInterceptor(InterceptorRegistry registry) {
        registry.addInterceptor(lepInterceptor).addPathPatterns("/**");

        LOGGER.info("Added handler interceptor '{}' to all urls", lepInterceptor.getClass().getSimpleName());
    }

    @Override
    protected void xmConfigurePathMatch(PathMatchConfigurer configurer) {

    }

    @Override
    protected List<String> getTenantIgnorePathPatterns() {
        return applicationProperties.getTenantIgnoredPathList();
    }

}
