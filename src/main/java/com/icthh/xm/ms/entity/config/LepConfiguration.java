package com.icthh.xm.ms.entity.config;

import com.icthh.xm.commons.lep.TenantScriptStorage;
import com.icthh.xm.commons.lep.spring.EnableLepServices;
import com.icthh.xm.commons.lep.spring.web.WebLepSpringConfiguration;
import com.icthh.xm.ms.entity.lep.ElasticIndexManager;
import com.icthh.xm.ms.entity.lep.ElasticIndexManagerService;
import com.icthh.xm.ms.entity.lep.keyresolver.FunctionLepKeyResolver;
import com.icthh.xm.ms.entity.lep.keyresolver.SystemQueueConsumerLepKeyResolver;
import com.icthh.xm.ms.entity.repository.search.XmEntitySearchRepository;
import com.icthh.xm.ms.entity.service.XmEntityLifeCycleService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.transaction.support.SimpleTransactionScope;

import java.util.concurrent.TimeUnit;

/**
 * The {@link LepConfiguration} class.
 */
@Slf4j
@Configuration
@EnableLepServices(basePackageClasses = XmEntityLifeCycleService.class)
public class LepConfiguration extends WebLepSpringConfiguration {

    @Value("${application.lep.tenant-script-storage}")
    private TenantScriptStorage tenantScriptStorageType;

    public LepConfiguration(@Value("${spring.application.name}") String appName,
                            ApplicationEventPublisher eventPublisher,
                            ResourceLoader resourceLoader,
                            ConfigurableListableBeanFactory factory) {
        super(appName, eventPublisher, resourceLoader);
        factory.registerScope("transaction", new SimpleTransactionScope());
    }

    @Override
    protected TenantScriptStorage getTenantScriptStorageType() {
        return tenantScriptStorageType;
    }

    @Bean
    public FunctionLepKeyResolver functionLepKeyResolver() {
        return new FunctionLepKeyResolver();
    }

    @Bean
    public SystemQueueConsumerLepKeyResolver systemQueueConsumerLepKeyResolver() {
        return new SystemQueueConsumerLepKeyResolver();
    }

    @Bean
    public ElasticIndexManagerService elasticIndexManagerService(ApplicationContext context) {
        return new ElasticIndexManagerService() {
            @Override
            public ElasticIndexManager getElasticIndexManager() {
                // using application context to fetch correct bean by scope rules every times
                log.info("START get ElasticIndexManager");
                StopWatch stopWatch = StopWatch.createStarted();
                var bean = context.getBean(ElasticIndexManager.class);
                log.info("STOP get ElasticIndexManager {}", stopWatch.getTime(TimeUnit.MILLISECONDS));
                return bean;
            }
        };
    }

}
