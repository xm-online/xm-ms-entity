package com.icthh.xm.ms.entity.repository.kafka;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.icthh.xm.commons.config.client.repository.ConfigurationModel;
import com.icthh.xm.commons.config.domain.Configuration;
import com.icthh.xm.commons.logging.util.MdcUtils;
import com.icthh.xm.commons.messaging.event.system.SystemEvent;
import com.icthh.xm.commons.messaging.event.system.SystemEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

@Service
@Slf4j
public class SystemTopicConsumer {
    private final ConfigurationModel configurationModel;

    public SystemTopicConsumer(Optional<ConfigurationModel> configurationModel) {
        this.configurationModel = configurationModel.orElse(null);
    }

    /**
     * Consume tenant command event message.
     * @param message the tenant command event message
     */
    @Retryable(maxAttemptsExpression = "${application.retry.max-attempts}",
        backoff = @Backoff(delayExpression = "${application.retry.delay}",
            multiplierExpression = "${application.retry.multiplier}"))
    public void consumeEvent(ConsumerRecord<String, String> message) {
        MdcUtils.putRid();
        try {
            log.info("Consume event from topic [{}]", message.topic());
            ObjectMapper mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            mapper.registerModule(new JavaTimeModule());
            try {
                SystemEvent event = mapper.readValue(message.value(), SystemEvent.class);

                log.info("Process event from topic [{}], type='{}', source='{}', event_id ='{}'",
                    message.topic(), event.getEventType(), event.getMessageSource(), event.getEventId());

                switch (event.getEventType().toUpperCase()) {
                    case SystemEventType.SAVE_CONFIGURATION:
                        onSaveConfiguration(event);
                        break;
                    default:
                        log.info("Event ignored with type='{}', source='{}', event_id='{}'",
                            event.getEventType(), event.getMessageSource(), event.getEventId());
                        break;
                }
            } catch (IOException e) {
                log.error("System queue message has incorrect format: '{}'", message.value(), e);
            }
        } finally {
            MdcUtils.removeRid();
        }
    }

    private void onSaveConfiguration(SystemEvent event) {
        String path = Objects.toString(event.getDataMap().get("path"), null);
        if (StringUtils.isBlank(path)) {
            throw new IllegalArgumentException("Event '" + event.getEventType() + "' configuration path can't be blank");
        }
        String commit = Objects.toString(event.getDataMap().get("commit"), null);
        if (StringUtils.isBlank(commit)) {
            throw new IllegalArgumentException("Event '" + event.getEventType() + "' configuration commit can't be blank");
        }
        configurationModel.updateConfiguration(new Configuration(path, null, commit));
    }
}
