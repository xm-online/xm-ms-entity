package com.icthh.xm.ms.entity.util;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

// FIXME @emonastyrev Why we need ApplicationContextHolder class?
// 1. Using static method ApplicationContextHolder.getInstance() in classes such as AvatarUrlListener do them
//    hard to test.
// 2. ApplicationContextHolder is a Spring bean and Java singleton in the same time, so using it in 'static' manner
//    can lead to bugs related to different initialization flows of Spring-beans and Java-classes initialization.
//    Because call of getInstance() may occurs before Spring context injection in ApplicationContextHolder Spring bean.
//    Or call of getInstance() may occurs after Spring context destroy.
//
@Component
public final class ApplicationContextHolder implements ApplicationContextAware {

    private static final ApplicationContextHolder INSTANCE = new ApplicationContextHolder();
    private static volatile ApplicationContext applicationContext;

    private ApplicationContextHolder() {
    }

    @Override
    public void setApplicationContext(final ApplicationContext applicationContext) {
        setApplicationContextToHolder(applicationContext);
    }

    private static void setApplicationContextToHolder(ApplicationContext applicationContext) {
        ApplicationContextHolder.applicationContext = applicationContext;
    }

    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    public static ApplicationContextHolder getInstance() {
        return INSTANCE;
    }

}
