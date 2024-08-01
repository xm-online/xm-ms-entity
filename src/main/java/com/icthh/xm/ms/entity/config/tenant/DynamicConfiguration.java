package com.icthh.xm.ms.entity.config.tenant;

import com.icthh.xm.commons.topic.domain.DynamicConsumer;
import com.icthh.xm.commons.topic.service.DynamicConsumerConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class DynamicConfiguration implements DynamicConsumerConfiguration {

    private final Map<String, List<DynamicConsumer>> tenantTopicConsumers = new ConcurrentHashMap<>();

    @Override
    public List<DynamicConsumer> getDynamicConsumers(String tenantKey) {
        if (tenantTopicConsumers.containsKey(tenantKey)) {
            List<DynamicConsumer> dynamicConsumers = tenantTopicConsumers.get(tenantKey);
            dynamicConsumers.forEach(it -> it.getConfig().setGroupId(UUID.randomUUID().toString()));
            return dynamicConsumers;
        }

        return List.of();
    }
}
