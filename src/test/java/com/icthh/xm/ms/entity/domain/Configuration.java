package com.icthh.xm.ms.entity.domain;

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.json.SpringHandlerInstantiator;


@org.springframework.context.annotation.Configuration
public class Configuration {
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jackson2ObjectMapperBuilder(
        AutowireCapableBeanFactory beanFactory) {
        return jacksonObjectMapperBuilder -> jacksonObjectMapperBuilder
            .handlerInstantiator(new SpringHandlerInstantiator(beanFactory));
    }
}

