package com.icthh.xm.ms.entity.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

@Configuration
@Slf4j
public class WebMvcConfiguration implements WebMvcConfigurer {

    private static final Collection<String> JSON_FILTER_APPLIED_URI =
        Collections.singletonList("/api/xm-entities/*/links/targets");

    public static String[] getJsonFilterAllowedURIs() {
        return JSON_FILTER_APPLIED_URI.toArray(new String[]{});
    }

    @Override
    public void configureMessageConverters(HttpMessageConverters.ServerBuilder builder) {
        // create custom Jackson converter with additional media types
        builder.addCustomConverter(createCustomJacksonConverter());
    }

    private JacksonJsonHttpMessageConverter createCustomJacksonConverter() {
        JacksonJsonHttpMessageConverter converter = new JacksonJsonHttpMessageConverter();
        ArrayList<MediaType> mediaTypes = new ArrayList<>(converter.getSupportedMediaTypes());
        mediaTypes.add(MediaType.parseMediaType("text/csv"));
        mediaTypes.add(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        converter.setSupportedMediaTypes(mediaTypes);
        return converter;
    }
}
