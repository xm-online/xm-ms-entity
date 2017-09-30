package com.icthh.xm.ms.entity.config;

import io.github.jhipster.config.locale.AngularCookieLocaleResolver;

import java.nio.charset.Charset;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

@Configuration
public class LocaleConfiguration extends WebMvcConfigurerAdapter implements EnvironmentAware {

    @Override
    public void setEnvironment(Environment environment) {
        // unused
    }

    @Bean(name = "localeResolver")
    public LocaleResolver localeResolver() {
        AngularCookieLocaleResolver cookieLocaleResolver = new AngularCookieLocaleResolver();
        cookieLocaleResolver.setCookieName("NG_TRANSLATE_LANG_KEY");
        return cookieLocaleResolver;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        LocaleChangeInterceptor localeChangeInterceptor = new LocaleChangeInterceptor();
        localeChangeInterceptor.setParamName("language");
        registry.addInterceptor(localeChangeInterceptor);
    }

    @Bean
    public MessageSource defaultMessageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasenames("messages", "i18n/messages");
        messageSource.setDefaultEncoding(Charset.forName("UTF-8").name());
        messageSource.setFallbackToSystemLocale(true);
        messageSource.setCacheSeconds(-1);
        messageSource.setAlwaysUseMessageFormat(false);
        messageSource.setUseCodeAsDefaultMessage(true);
        return messageSource;
    }
}
