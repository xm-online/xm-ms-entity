package com.icthh.xm.ms.entity.config.topic;

import com.icthh.xm.commons.config.client.repository.TenantListRepository;
import com.icthh.xm.commons.topic.message.LepMessageHandler;
import com.icthh.xm.commons.topic.service.DynamicConsumerConfiguration;
import com.icthh.xm.commons.topic.service.DynamicConsumerConfigurationService;
import com.icthh.xm.commons.topic.service.TopicConfigurationService;
import com.icthh.xm.commons.topic.service.TopicManagerService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class TopicManagerConfiguration {

    private final LepMessageHandler messageHandler;

    public TopicManagerConfiguration(@Lazy LepMessageHandler messageHandler) {
        this.messageHandler = messageHandler;
    }

    @Bean
    public TopicConfigurationService topicConfigurationService(@Value("${spring.application.name}") String appName,
                                                               TopicManagerService topicManagerService,
                                                               TenantListRepository tenantListRepository) {
        List<DynamicConsumerConfiguration> dynamicConsumerConfigurations = new ArrayList<>();

        TopicConfigurationService topicConfigurationService = new TopicConfigurationService(appName,
            dynamicConsumerConfigurationService(topicManagerService, tenantListRepository, dynamicConsumerConfigurations),
            messageHandler);

        dynamicConsumerConfigurations.add(topicConfigurationService);

        return topicConfigurationService;
    }

    @Bean
    public DynamicConsumerConfigurationService dynamicConsumerConfigurationService(
        TopicManagerService topicService,
        TenantListRepository tenantListRepository,
        List<DynamicConsumerConfiguration> consumerConfigurations) {
        return new DynamicConsumerConfigurationService(consumerConfigurations, topicService, tenantListRepository);
    }
}
