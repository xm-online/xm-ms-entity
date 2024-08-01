package com.icthh.xm.ms.entity.config;

import com.icthh.xm.commons.web.spring.TenantInterceptor;
import com.icthh.xm.commons.web.spring.XmLoggingInterceptor;
import com.icthh.xm.commons.web.spring.config.WebMvcConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.AsyncHandlerInterceptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Configuration
public class WebMvcConfiguration extends WebMvcConfig {

    private static final Collection<String> JSON_FILTER_APPLIED_URI =
        Collections.singletonList("/api/xm-entities/*/links/targets");

    private final ApplicationProperties applicationProperties;

    public WebMvcConfiguration(@Value("${application.tenant-ignored-path-list}")
                               List<String> tenantIgnoredPathList,
                               TenantInterceptor tenantInterceptor,
                               XmLoggingInterceptor xmLoggingInterceptor,
                               List<AsyncHandlerInterceptor> interceptors,
                               ApplicationProperties applicationProperties
    ) {
        super(tenantIgnoredPathList, tenantInterceptor, xmLoggingInterceptor, interceptors);
        this.applicationProperties = applicationProperties;
    }

    public static String[] getJsonFilterAllowedURIs() {
        return JSON_FILTER_APPLIED_URI.toArray(new String[]{});
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
