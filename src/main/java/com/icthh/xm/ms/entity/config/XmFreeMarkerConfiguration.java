package com.icthh.xm.ms.entity.config;

import freemarker.cache.StringTemplateLoader;
import freemarker.cache.TemplateLoader;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

import java.util.List;

@Configuration
public class XmFreeMarkerConfiguration {

    @Bean
    public freemarker.template.Configuration freeMarkerConfiguration() {
        freemarker.template.Configuration configuration = new freemarker.template.Configuration(freemarker.template.Configuration.VERSION_2_3_34);
        configuration.setDefaultEncoding("UTF-8");
        configuration.setClassForTemplateLoading(this.getClass(), "/templates/");
        return configuration;
    }

    @Bean
    public XmFreeMarkerConfigurer xmFreeMarkerConfigurer(StringTemplateLoader emailTemplates) {
        return new XmFreeMarkerConfigurer(emailTemplates);
    }

    @Bean
    public StringTemplateLoader emailTemplates() {
        return new StringTemplateLoader();
    }

    @RequiredArgsConstructor
    public static class XmFreeMarkerConfigurer extends FreeMarkerConfigurer {

        private final StringTemplateLoader emailTemplates;

        @Override
        protected void postProcessTemplateLoaders(List<TemplateLoader> templateLoaders) {
            super.postProcessTemplateLoaders(templateLoaders);
            templateLoaders.add(emailTemplates);
        }
    }
}


