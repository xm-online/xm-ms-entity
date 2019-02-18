package com.icthh.xm.lep


import com.icthh.xm.commons.lep.TenantScriptStorage
import com.icthh.xm.commons.lep.spring.LepSpringConfiguration
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.ApplicationEventPublisher
import org.springframework.core.io.ResourceLoader

@TestConfiguration
class LepTestConfig extends LepSpringConfiguration {

    LepTestConfig(final ApplicationEventPublisher eventPublisher,
                  final ResourceLoader resourceLoader) {
        super("testApp", eventPublisher, resourceLoader)
    }

    @Override
    protected TenantScriptStorage getTenantScriptStorageType() {
        return TenantScriptStorage.CLASSPATH
    }

}