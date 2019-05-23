package com.icthh.xm.ms.entity.config;

import com.github.bohnman.squiggly.Squiggly;
import com.icthh.xm.commons.lep.spring.web.LepInterceptor;
import com.icthh.xm.commons.web.spring.TenantInterceptor;
import com.icthh.xm.commons.web.spring.TenantVerifyInterceptor;
import com.icthh.xm.commons.web.spring.XmLoggingInterceptor;
import com.icthh.xm.commons.web.spring.config.XmMsWebConfiguration;
import com.icthh.xm.commons.web.spring.config.XmWebMvcConfigurerAdapter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.icthh.xm.ms.entity.service.XmSquigglyContextProvider;
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

@Configuration
@Import( {
    XmMsWebConfiguration.class
})
public class WebMvcConfiguration extends XmWebMvcConfigurerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebMvcConfiguration.class);

    private final ApplicationProperties applicationProperties;
    private final LepInterceptor lepInterceptor;
    private final TenantVerifyInterceptor tenantVerifyInterceptor;
    private final XmSquigglyContextProvider xmSquigglyContextProvider;

    public WebMvcConfiguration(TenantInterceptor tenantInterceptor,
                               XmLoggingInterceptor xmLoggingInterceptor,
                               ApplicationProperties applicationProperties,
                               LepInterceptor lepInterceptor,
                               TenantVerifyInterceptor tenantVerifyInterceptor,
                               XmSquigglyContextProvider xmSquigglyContextProvider) {
        super(tenantInterceptor, xmLoggingInterceptor);

        this.applicationProperties = applicationProperties;
        this.lepInterceptor = lepInterceptor;
        this.tenantVerifyInterceptor = tenantVerifyInterceptor;
        this.xmSquigglyContextProvider = xmSquigglyContextProvider;
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

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        // add text/csv and xlsx media types to MappingJackson2HttpMessageConverter
        addSupportedMediaTypesTo(converters, MappingJackson2HttpMessageConverter.class,
            MediaType.parseMediaType("text/csv"),
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

        // add Squiggly dynamic json filter possibility to message converters.
        initSquigglyFilterToJacksonMessageConverter(converters);
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

    private void initSquigglyFilterToJacksonMessageConverter(List<HttpMessageConverter<?>> converters) {
        converters.stream()
                  .filter(httpMessageConverter -> MappingJackson2HttpMessageConverter.class.isAssignableFrom(
                      httpMessageConverter.getClass()))
                  .map(MappingJackson2HttpMessageConverter.class::cast)
                  .peek(mc -> LOGGER.info("Init Squiggly filter for message converter: {} and objectMapper: {}", mc, mc.getObjectMapper()))
                  .forEach(mc -> Squiggly.init(mc.getObjectMapper(), xmSquigglyContextProvider));
    }
}
