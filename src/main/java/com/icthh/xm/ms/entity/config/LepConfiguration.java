package com.icthh.xm.ms.entity.config;

import com.icthh.xm.commons.lep.TenantScriptStorage;
import com.icthh.xm.commons.lep.spring.EnableLepServices;
import com.icthh.xm.commons.lep.spring.web.WebLepSpringConfiguration;
import com.icthh.xm.ms.entity.lep.keyresolver.FunctionLepKeyResolver;
import com.icthh.xm.ms.entity.lep.keyresolver.FunctionWithXmEntityLepKeyResolver;
import com.icthh.xm.ms.entity.lep.keyresolver.OnStateTransitionLepKeyResolver;
import com.icthh.xm.ms.entity.lep.keyresolver.SystemQueueConsumerLepKeyResolver;
import com.icthh.xm.ms.entity.service.XmEntityLifeCycleService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;

/**
 * The {@link LepConfiguration} class.
 */
@Configuration
@EnableLepServices(basePackageClasses = XmEntityLifeCycleService.class)
public class LepConfiguration extends WebLepSpringConfiguration {

    @Value("${application.lep.tenant-script-storage}")
    private TenantScriptStorage tenantScriptStorageType;

    public LepConfiguration(@Value("${spring.application.name}") String appName,
                            ApplicationEventPublisher eventPublisher,
                            ResourceLoader resourceLoader) {
        super(appName, eventPublisher, resourceLoader);
    }

    @Override
    protected TenantScriptStorage getTenantScriptStorageType() {
        return tenantScriptStorageType;
    }

    // Key resolvers

    @Bean
    public OnStateTransitionLepKeyResolver lepKeyResolverOnStateTransition() {
        return new OnStateTransitionLepKeyResolver();
    }

    @Bean
    public FunctionWithXmEntityLepKeyResolver functionWithXmEntityLepKeyResolver() {
        return new FunctionWithXmEntityLepKeyResolver();
    }

    @Bean
    public FunctionLepKeyResolver functionLepKeyResolver() {
        return new FunctionLepKeyResolver();
    }

    @Bean
    public SystemQueueConsumerLepKeyResolver systemQueueConsumerLepKeyResolver() {
        return new SystemQueueConsumerLepKeyResolver();
    }

}
