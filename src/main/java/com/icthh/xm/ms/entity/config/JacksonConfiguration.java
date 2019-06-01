package com.icthh.xm.ms.entity.config;

import com.fasterxml.jackson.datatype.hibernate5.Hibernate5Module;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import com.github.bohnman.squiggly.Squiggly;
import com.icthh.xm.ms.entity.domain.Link;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.serializer.XmSquigglyContextProvider;
import com.icthh.xm.ms.entity.domain.serializer.XmSquigglyFilterCustomizer;
import com.icthh.xm.ms.entity.domain.serializer.XmSquigglyInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.json.SpringHandlerInstantiator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Configuration
public class JacksonConfiguration {

    /**
     * Support for Java date and time API.
     *
     * @return the corresponding Jackson module.
     */
    @Bean
    public JavaTimeModule javaTimeModule() {
        return new JavaTimeModule();
    }

    @Bean
    public Jdk8Module jdk8TimeModule() {
        return new Jdk8Module();
    }

    /*
     * Support for Hibernate types in Jackson.
     */
    @Bean
    public Hibernate5Module hibernate5Module() {
        return new Hibernate5Module();
    }

    /**
     * Jackson Afterburner module to speed up serialization/deserialization.
     */
    @Bean
    public AfterburnerModule afterburnerModule() {
        return new AfterburnerModule();
    }

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jackson2ObjectMapperBuilder(
        AutowireCapableBeanFactory beanFactory) {
        return jacksonObjectMapperBuilder -> jacksonObjectMapperBuilder
            .handlerInstantiator(new SpringHandlerInstantiator(beanFactory));
    }

    @Bean
    public XmSquigglyInterceptor xmSquigglyInterceptor(){
        return new XmSquigglyInterceptor();
    }

    @Bean
    public XmSquigglyFilterCustomizer xmSquigglyFilterCustomizer(){
        return new XmSquigglyFilterCustomizer();
    }

    @Bean
    public XmSquigglyContextProvider xmSquigglyContextProvider() {

        Map<Class, String> defaultFilterByBean = new HashMap<>();

        defaultFilterByBean.put(Link.class, "**,target.id"
                                            + ",target.key"
                                            + ",target.typeKey"
                                            + ",target.stateKey"
                                            + ",target.name"
                                            + ",target.startDate"
                                            + ",target.endDate"
                                            + ",target.updateDate"
                                            + ",target.description"
                                            + ",target.createdBy"
                                            + ",target.removed"
                                            + ",target.data");
        defaultFilterByBean.put(XmEntity.class, "**,-targets.target.sources"
                                                + ",-targets.target.targets");

        return new XmSquigglyContextProvider(defaultFilterByBean, xmSquigglyFilterCustomizer());
    }

    @Bean
    public HttpMessageConverterCustomizer httpMessageConverterCustomizer() {

        Class<MappingJackson2HttpMessageConverter> expectedClass = MappingJackson2HttpMessageConverter.class;

        return converters -> converters.stream()
                                       .filter(mc -> expectedClass.isAssignableFrom(mc.getClass()))
                                       .map(expectedClass::cast)
                                       .forEach(this::initSquiggly);
    }

    public interface HttpMessageConverterCustomizer {

        void customize(List<HttpMessageConverter<?>> converters);

    }

    private void initSquiggly(MappingJackson2HttpMessageConverter converter) {
        log.info("Init Squiggly filter for message converter: {} and objectMapper: {}",
                 converter, converter.getObjectMapper());
        Squiggly.init(converter.getObjectMapper(), xmSquigglyContextProvider());
    }

}
