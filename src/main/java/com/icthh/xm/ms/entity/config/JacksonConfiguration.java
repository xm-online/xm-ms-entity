package com.icthh.xm.ms.entity.config;

import com.fasterxml.jackson.datatype.hibernate5.Hibernate5Module;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import com.github.bohnman.squiggly.web.SquigglyRequestFilter;
import com.icthh.xm.ms.entity.domain.Link;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.service.XmSquigglyContextProvider;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.SpringHandlerInstantiator;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class JacksonConfiguration {

    /**
     * Support for Java date and time API.
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
    public SquigglyRequestFilter squigglyRequestFilter(){
        return new SquigglyRequestFilter();
    }

    @Bean
    public FilterRegistrationBean filterRegistrationBean() {
        System.out.println("##### create filter register bean");
        FilterRegistrationBean<SquigglyRequestFilter> filter = new FilterRegistrationBean<>();
        filter.setFilter(squigglyRequestFilter());
        filter.setOrder(1);
        filter.setUrlPatterns(Arrays.asList("/api/xm-entities/*/links/targets"));
        return filter;
    }

    @Bean
    public XmSquigglyContextProvider xmSquigglyContextProvider() {

        String defaultFilter = "**,target.id"
                               + ",target.key"
                               + ",target.typeKey"
                               + ",target.stateKey"
                               + ",target.name"
                               + ",target.startDate"
                               + ",target.startDate"
                               + ",target.updateDate"
                               + ",target.description"
                               + ",target.createdBy"
                               + ",target.removed"
                               + ",target.data"
//                               + ",-target.version"
//                               + ",-target.targets"
//                               + ",-target.sources"
//                               + ",-target.attachments"
//                               + ",-target.locations"
//                               + ",-target.tags"
//                               + ",-target.calendars"
//                               + ",-target.ratings"
//                               + ",-target.comments"
//                               + ",-target.votes"
//                               + ",-target.functionContexts"
//                               + ",-target.events"
//                               + ",-target.uniqueFields"
                               + ",-targets.target.sources"
                               + ",-targets.target.targets";

//        String defaultFilter = "-super";

//        String defaultFilter = "**";


        // TODO think about default filter per beanClass.
        Map<Class, String> defaultFilterByBean = new HashMap<>();

        defaultFilterByBean.put(Link.class, "**,target.id"
                                            + ",target.key"
                                            + ",target.typeKey"
                                            + ",target.stateKey"
                                            + ",target.name"
                                            + ",target.startDate"
                                            + ",target.startDate"
                                            + ",target.updateDate"
                                            + ",target.description"
                                            + ",target.createdBy"
                                            + ",target.removed"
                                            + ",target.data");
        defaultFilterByBean.put(XmEntity.class, "-targets.target.sources"
                                                + ",-targets.target.targets");

        return new XmSquigglyContextProvider(defaultFilterByBean, defaultFilter);
    }

}
