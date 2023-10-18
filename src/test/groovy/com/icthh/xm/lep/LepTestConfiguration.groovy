package com.icthh.xm.lep

import com.icthh.xm.commons.lep.TenantScriptStorage
import com.icthh.xm.commons.lep.groovy.GroovyLepEngineConfiguration
import com.icthh.xm.commons.lep.spring.LepUpdateMode
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.core.io.ResourceLoader

/**
 * All LEP related configuration should have @Profile('leptest') annotation to prevent interfering with main java test.
 */
@TestConfiguration
@Profile('leptest')
class LepTestConfiguration extends GroovyLepEngineConfiguration {

    LepTestConfiguration(final ApplicationEventPublisher eventPublisher,
                         final ResourceLoader resourceLoader) {
        super("testApp", eventPublisher, resourceLoader)
    }

    @Override
    protected TenantScriptStorage getTenantScriptStorageType() {
        return TenantScriptStorage.CLASSPATH
    }

    @Override
    @Bean
    LepUpdateMode lepUpdateMode() {
        return LepUpdateMode.SYNCHRONOUS;
    }
}
