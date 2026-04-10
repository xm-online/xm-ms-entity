package com.icthh.xm.ms.entity.util;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

// FIXME @emonastyrev Why we need AutowireHelper class?
// 1. Using static method AutowireHelper.getInstance() in classes such as AvatarUrlListener do them
//    hard to test.
// 2. AutowireHelper is a Spring bean and Java singleton in the same time, so using it in 'static' manner
//    can lead to bugs related to different initialization flows of Spring-beans and Java-classes initialization.
//    Because call of getInstance() may occurs before Spring context injection in AutowireHelper Spring bean.
//    Or call of getInstance() may occurs after Spring context destroy.
//
@Component
public final class AutowireHelper implements ApplicationContextAware {

    private static final AutowireHelper INSTANCE = new AutowireHelper();
    private static volatile ApplicationContext applicationContext;

    private AutowireHelper() {
    }

    @Override
    public void setApplicationContext(final ApplicationContext applicationContext) {
        setApplicationContextToHolder(applicationContext);
    }

    private static void setApplicationContextToHolder(ApplicationContext applicationContext) {
        AutowireHelper.applicationContext = applicationContext;
    }

    public static void autowire(Object classToAutowire, Object... beansToAutowireInClass) {
        for (Object bean : beansToAutowireInClass) {
            if (bean == null) {
                applicationContext.getAutowireCapableBeanFactory().autowireBean(classToAutowire);
                return;
            }
        }
    }
    public static AutowireHelper getInstance() {
        return INSTANCE;
    }

}
