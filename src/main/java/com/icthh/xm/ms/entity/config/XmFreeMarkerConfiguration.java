package com.icthh.xm.ms.entity.config;

import freemarker.cache.StringTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.core.TemplateClassResolver;
import freemarker.template.TemplateException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

import java.io.IOException;
import java.util.List;

@Configuration
public class XmFreeMarkerConfiguration {

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
        protected void postProcessConfiguration(freemarker.template.Configuration configuration)
            throws IOException, TemplateException {
            super.postProcessConfiguration(configuration);
            configuration.setNewBuiltinClassResolver(TemplateClassResolver.ALLOWS_NOTHING_RESOLVER);
            configuration.setAPIBuiltinEnabled(false);
        }

        @Override
        protected void postProcessTemplateLoaders(List<TemplateLoader> templateLoaders) {
            super.postProcessTemplateLoaders(templateLoaders);
            templateLoaders.add(emailTemplates);
        }
    }
}


