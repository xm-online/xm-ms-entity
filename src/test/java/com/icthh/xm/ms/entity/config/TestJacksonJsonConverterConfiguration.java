package com.icthh.xm.ms.entity.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import tools.jackson.databind.json.JsonMapper;

/**
 * Exposes the JSON message converter as a bean for tests that build a standalone {@code MockMvc}
 * via {@code MockMvcBuilders.standaloneSetup(...).setMessageConverters(...)}.
 * <p>
 * Deliberately test-only: declaring this converter as a bean in the application context makes Spring Boot
 * register it as a <em>custom</em> converter, ahead of {@code ByteArrayHttpMessageConverter}, which breaks
 * every {@code byte[]} response body (base64). See {@code WebMvcConfiguration#configureMessageConverters}.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestJacksonJsonConverterConfiguration {

    @Bean
    public JacksonJsonHttpMessageConverter converter(JsonMapper jsonMapper) {
        return new JacksonJsonHttpMessageConverter(jsonMapper);
    }
}
