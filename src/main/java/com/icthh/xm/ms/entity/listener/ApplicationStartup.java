package com.icthh.xm.ms.entity.listener;

import com.icthh.xm.commons.logging.util.MdcUtils;
import com.icthh.xm.commons.permission.inspector.PrivilegeInspector;
import com.icthh.xm.ms.entity.config.ApplicationProperties;
import com.icthh.xm.ms.entity.repository.kafka.SystemTopicConsumer;
import java.util.Map;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("unused")
public class ApplicationStartup implements ApplicationListener<ApplicationReadyEvent> {

    private final ApplicationProperties applicationProperties;
    private final ConsumerFactory<String, String> consumerFactory;
    private final SystemTopicConsumer systemTopicConsumer;
    private final KafkaProperties kafkaProperties;
    private final PrivilegeInspector privilegeInspector;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (applicationProperties.isKafkaEnabled()) {
            createKafkaConsumers();
            privilegeInspector.readPrivileges(MdcUtils.getRid());
        } else {
            log.warn("WARNING! Privileges inspection is disabled by "
                + "configuration parameter 'application.kafka-enabled'");
        }
    }

    private void createKafkaConsumers() {
        createSystemConsumer(applicationProperties.getKafkaSystemTopic(), systemTopicConsumer::consumeEvent);
    }

    private void createSystemConsumer(String name, MessageListener<String, String> consumeEvent) {
        log.info("Creating kafka consumer for topic {}", name);
        ContainerProperties containerProps = new ContainerProperties(name);
        containerProps.setObservationEnabled(true);

        Map<String, Object> props = kafkaProperties.buildConsumerProperties(null);
        if (name.equals(applicationProperties.getKafkaSystemTopic())) {
            props.put(ConsumerConfig.GROUP_ID_CONFIG, UUID.randomUUID().toString());
        }
        props.put(ConsumerConfig.METADATA_MAX_AGE_CONFIG, applicationProperties.getKafkaMetadataMaxAge());
        ConsumerFactory<String, String> factory = new DefaultKafkaConsumerFactory<>(props);

        ConcurrentMessageListenerContainer<String, String> container =
            new ConcurrentMessageListenerContainer<>(factory, containerProps);
        container.setupMessageListener(consumeEvent);
        container.start();
        log.info("Successfully created kafka consumer for topic {}", name);
    }

}
