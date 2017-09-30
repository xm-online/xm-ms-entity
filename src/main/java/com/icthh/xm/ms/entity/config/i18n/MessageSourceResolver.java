package com.icthh.xm.ms.entity.config.i18n;

import java.util.Locale;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.stereotype.Component;

@Component("messageSource")
public class MessageSourceResolver implements MessageSource {

    private MessageSource defaultMessageSource;

    public MessageSourceResolver(@Qualifier("defaultMessageSource") MessageSource defaultMessageSource) {
        this.defaultMessageSource = defaultMessageSource;
    }

    @Override
    public String getMessage(String code, Object[] args, String defaultMessage, Locale locale) {
        return getMessageSource().getMessage(code, args, defaultMessage, locale);
    }

    @Override
    public String getMessage(String code, Object[] args, Locale locale) {
        return getMessageSource().getMessage(code, args, locale);
    }

    @Override
    public String getMessage(MessageSourceResolvable resolvable, Locale locale) {
        return getMessageSource().getMessage(resolvable, locale);
    }

    private MessageSource getMessageSource() {
        return defaultMessageSource;
    }
}
