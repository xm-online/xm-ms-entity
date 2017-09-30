package com.icthh.xm.ms.entity.config;

import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_SINGLETON;

import com.icthh.lep.api.ExtensionService;
import com.icthh.lep.api.LepExecutor;
import com.icthh.lep.api.LepManager;
import com.icthh.lep.api.LepProcessingListener;
import com.icthh.lep.api.LepResourceService;
import com.icthh.lep.core.CoreLepManager;
import com.icthh.lep.groovy.DefaultScriptNameLepResourceKeyMapper;
import com.icthh.lep.groovy.ScriptNameLepResourceKeyMapper;
import com.icthh.lep.groovy.StrategyGroovyLepExecutor;
import com.icthh.xm.ms.entity.lep.XmClasspathLepResourceService;
import com.icthh.xm.ms.entity.lep.XmExtensionService;
import com.icthh.xm.ms.entity.lep.XmGroovyExecutionStrategy;
import com.icthh.xm.ms.entity.lep.XmGroovyScriptEngineProviderStrategy;
import com.icthh.xm.ms.entity.lep.XmLepProcessingListener;
import com.icthh.xm.ms.entity.lep.keyresolver.ChangeStateLepKeyResolver;
import com.icthh.xm.ms.entity.lep.keyresolver.ExecuteFunctionLepKeyResolver;
import com.icthh.xm.ms.entity.lep.keyresolver.OnStateTransitionLepKeyResolver;
import com.icthh.xm.ms.entity.lep.spring.EnableLepServices;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
import com.icthh.xm.ms.entity.service.XmEntityLifeCycleService;
import com.icthh.xm.ms.entity.service.XmTenantLifecycleService;
import com.icthh.xm.ms.entity.service.api.XmEntityService;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

/**
 * The {@link LepConfig} class.
 */
@Configuration
@EnableLepServices(basePackageClasses = XmEntityLifeCycleService.class)
public class LepConfig {

    @Autowired
    @Qualifier("xmEntityServiceResolver")
    private XmEntityService xmEntityService;

    @Bean
    ScriptNameLepResourceKeyMapper scriptNameLepResourceKeyMapper() {
        return new DefaultScriptNameLepResourceKeyMapper();
    }

    @Bean
    XmGroovyScriptEngineProviderStrategy xmGroovyScriptEngineProviderStrategy() {
        return new XmGroovyScriptEngineProviderStrategy(scriptNameLepResourceKeyMapper());
    }

    @Bean
    XmGroovyExecutionStrategy xmGroovyExecutionStrategy() {
        return new XmGroovyExecutionStrategy();
    }

    @Bean
    LepExecutor lepExecutor() {
        return new StrategyGroovyLepExecutor(scriptNameLepResourceKeyMapper(),
            xmGroovyScriptEngineProviderStrategy(),
            xmGroovyExecutionStrategy());
    }

    @Bean
    ExtensionService extensionService() {
        return new XmExtensionService();
    }

    @Bean
    LepResourceService lepResourceService() {
        return new XmClasspathLepResourceService(lepManager());
    }

    @Bean
    @Scope(SCOPE_SINGLETON)
    LepManager lepManager() {
        return new CoreLepManager();
    }

    @Bean
    LepProcessingListener xmLepProcessingListener(@Qualifier("xmEntityServiceResolver") XmEntityService xmEntityService,
        XmTenantLifecycleService xmTenantLifecycleService, XmEntityRepository xmEntityRepository) {
        return new XmLepProcessingListener(xmEntityService, xmTenantLifecycleService, xmEntityRepository);
    }

    @Bean("lepInitializer")
    LepInitializer lepManagerInitializer(@Qualifier("xmEntityServiceResolver") XmEntityService xmEntityService,
        XmTenantLifecycleService xmTenantLifecycleService, XmEntityRepository xmEntityRepository) {
        return new LepInitializer(lepManager(), extensionService(), lepResourceService(),
            lepExecutor(), xmLepProcessingListener(xmEntityService, xmTenantLifecycleService, xmEntityRepository));
    }

    static class LepInitializer {

        private final LepManager manager;
        private final ExtensionService extensionService;
        private final LepResourceService lepResourceService;
        private final LepExecutor lepExecutor;
        private final LepProcessingListener lepProcessingListener;

        private LepInitializer(LepManager manager,
            ExtensionService extensionService,
            LepResourceService lepResourceService,
            LepExecutor lepExecutor,
            LepProcessingListener lepProcessingListener) {
            this.manager = manager;
            this.extensionService = extensionService;
            this.lepResourceService = lepResourceService;
            this.lepExecutor = lepExecutor;
            this.lepProcessingListener = lepProcessingListener;
        }

        @PostConstruct
        public void onInit() {
            manager.init(extensionService, lepResourceService, lepExecutor);
            manager.registerProcessingListener(lepProcessingListener);
        }

        @PreDestroy
        public void onDestroy() {
            manager.unregisterProcessingListener(lepProcessingListener);
            manager.destroy();
        }

    }

    // Key resolvers
    @Bean
    public OnStateTransitionLepKeyResolver lepKeyResolverOnStateTransition() {
        return new OnStateTransitionLepKeyResolver();
    }

    @Bean
    public ChangeStateLepKeyResolver changeStateLepKeyResolver() {
        return new ChangeStateLepKeyResolver();
    }

    @Bean
    public ExecuteFunctionLepKeyResolver lepKeyResolverCallFunction() {
        return new ExecuteFunctionLepKeyResolver();
    }

}
