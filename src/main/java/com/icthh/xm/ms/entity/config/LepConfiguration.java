package com.icthh.xm.ms.entity.config;

import com.icthh.xm.commons.config.client.repository.CommonConfigRepository;
import com.icthh.xm.commons.config.client.service.CommonConfigService;
import com.icthh.xm.commons.domain.FunctionSpecWithFileName;
import com.icthh.xm.commons.lep.TenantScriptStorage;
import com.icthh.xm.commons.lep.groovy.GroovyLepEngineConfiguration;
import com.icthh.xm.commons.service.FunctionManageService;
import com.icthh.xm.ms.entity.lep.ElasticIndexManager;
import com.icthh.xm.ms.entity.lep.ElasticIndexManagerService;
import com.icthh.xm.ms.entity.service.XmEntityFunctionManagementService;
import com.icthh.xm.ms.entity.service.XmEntitySpecService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.support.SimpleTransactionScope;

/**
 * The {@link LepConfiguration} class.
 */
@Configuration
public class LepConfiguration extends GroovyLepEngineConfiguration {

    @Value("${application.lep.tenant-script-storage}")
    private TenantScriptStorage tenantScriptStorageType;

    public LepConfiguration(@Value("${spring.application.name}") String appName,
                            ConfigurableListableBeanFactory factory) {
        super(appName);
        factory.registerScope("transaction", new SimpleTransactionScope());
    }

    @Override
    protected TenantScriptStorage getTenantScriptStorageType() {
        return tenantScriptStorageType;
    }

    @Bean
    public ElasticIndexManagerService elasticIndexManagerService(ApplicationContext context) {
        return new ElasticIndexManagerService() {
            @Override
            public ElasticIndexManager getElasticIndexManager() {
                // using application context to fetch correct bean by scope rules every times
                return context.getBean(ElasticIndexManager.class);
            }
        };
    }

    @Bean
    public FunctionManageService<?, ? extends FunctionSpecWithFileName<?>> functionManageService(XmEntitySpecService xmEntitySpecService,
                                                                                                 CommonConfigRepository commonConfigRepository,
                                                                                                 CommonConfigService commonConfigService) {
        return new XmEntityFunctionManagementService(
            xmEntitySpecService, commonConfigRepository, commonConfigService
        );
    }

}
