package com.icthh.xm.ms.entity.repository.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.icthh.xm.commons.logging.util.MdcUtils;
import com.icthh.xm.ms.entity.domain.kafka.SystemEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
public class SystemTopicEventProducer {

    private final KafkaTemplate<String, String> template;
    private final ObjectMapper mapper = new ObjectMapper().configure(
        SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false).registerModule(new JavaTimeModule());

    @Autowired
    public SystemTopicEventProducer(KafkaTemplate<String, String> template) {
        this.template = template;
    }

    @Value("${spring.application.name}")
    private String appName;

    @Value("${application.kafka-system-topic}")
    private String topicName;

    /**
     * Send event to XM System Topic.
     *
     * @param content the event data
     */
    @Async
    public void send(String content) {
        if (StringUtils.isNoneBlank(content)) {
            log.debug("Sending kafka event with data {} to topic {}", content, topicName);
            template.send(topicName, content);
        }
    }

    public String createEventJson(Object data, String eventType) {
        SystemEvent event = null;
        try {
            event = buildSystemEvent(eventType);
            event.setData(data);
            return mapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            log.warn("Event creation error, eventType = {}, data = {}", eventType, event.getData(), e);
        }
        return null;
    }

    private SystemEvent buildSystemEvent(String eventType) {
        SystemEvent event = new SystemEvent();
        event.setEventId(MdcUtils.getRid());
        event.setMessageSource(appName);
        event.setEventType(eventType);
        event.setStartDate(Instant.now().toString());
        return event;
    }
}
