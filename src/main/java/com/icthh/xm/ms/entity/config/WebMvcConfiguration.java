package com.icthh.xm.ms.entity.config;

import com.icthh.xm.commons.web.spring.TenantInterceptor;
import com.icthh.xm.commons.web.spring.TenantVerifyInterceptor;
import com.icthh.xm.commons.web.spring.XmLoggingInterceptor;
import com.icthh.xm.commons.web.spring.config.XmMsWebConfiguration;
import com.icthh.xm.commons.web.spring.config.XmWebMvcConfigurerAdapter;
import com.icthh.xm.ms.entity.domain.serializer.XmSquigglyInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Configuration
@Import( {
    XmMsWebConfiguration.class
})
public class WebMvcConfiguration extends XmWebMvcConfigurerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebMvcConfiguration.class);

    private static final Collection<String> JSON_FILTER_APPLIED_URI =
        Collections.singletonList("/api/xm-entities/*/links/targets");

    private final ApplicationProperties applicationProperties;
    private final TenantVerifyInterceptor tenantVerifyInterceptor;
    private final XmSquigglyInterceptor xmSquigglyInterceptor;
    private final JacksonConfiguration.HttpMessageConverterCustomizer httpMessageConverterCustomizer;

    public WebMvcConfiguration(TenantInterceptor tenantInterceptor,
                               XmLoggingInterceptor xmLoggingInterceptor,
                               ApplicationProperties applicationProperties,
                               TenantVerifyInterceptor tenantVerifyInterceptor,
                               final XmSquigglyInterceptor xmSquigglyInterceptor,
                               JacksonConfiguration.HttpMessageConverterCustomizer httpMessageConverterCustomizer) {
        super(tenantInterceptor, xmLoggingInterceptor);

        this.applicationProperties = applicationProperties;
        this.tenantVerifyInterceptor = tenantVerifyInterceptor;
        this.xmSquigglyInterceptor = xmSquigglyInterceptor;
        this.httpMessageConverterCustomizer = httpMessageConverterCustomizer;
    }

    public static String[] getJsonFilterAllowedURIs() {
        return JSON_FILTER_APPLIED_URI.toArray(new String[]{});
    }

    @Override
    protected void xmAddInterceptors(InterceptorRegistry registry) {
        registerTenantInterceptorWithIgnorePathPattern(registry, tenantVerifyInterceptor);
        registerJsonFilterInterceptor(registry);
    }

    private void registerJsonFilterInterceptor(InterceptorRegistry registry) {

        registry.addInterceptor(xmSquigglyInterceptor).addPathPatterns(getJsonFilterAllowedURIs());
        LOGGER.info("Added handler interceptor '{}' to urls: {}",
                    xmSquigglyInterceptor.getClass().getSimpleName(), JSON_FILTER_APPLIED_URI);
    }

    @Override
    protected void xmConfigurePathMatch(PathMatchConfigurer configurer) {

    }

    @Override
    protected List<String> getTenantIgnorePathPatterns() {
        return applicationProperties.getTenantIgnoredPathList();
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        // add text/csv and xlsx media types to MappingJackson2HttpMessageConverter
        addSupportedMediaTypesTo(converters, MappingJackson2HttpMessageConverter.class,
            MediaType.parseMediaType("text/csv"),
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

        httpMessageConverterCustomizer.customize(converters);

        super.configureMessageConverters(converters);
    }

    private void addSupportedMediaTypesTo(List<HttpMessageConverter<?>> converters,
        Class<? extends AbstractHttpMessageConverter<?>> targetConverterClass,
        MediaType... mediaTypes) {
        converters.stream()
            .filter(conv -> conv.getClass() == targetConverterClass)
            .map(conv -> (AbstractHttpMessageConverter) conv)
            .forEach(conv -> addSupportedMediaTypes(conv, Arrays.asList(mediaTypes)));
    }

    private void addSupportedMediaTypes(AbstractHttpMessageConverter<?> converter,
        List<MediaType> additionalMediaTypes) {
        ArrayList<MediaType> mediaTypes = new ArrayList<>(converter.getSupportedMediaTypes());
        mediaTypes.addAll(additionalMediaTypes);
        converter.setSupportedMediaTypes(mediaTypes);
    }
}
