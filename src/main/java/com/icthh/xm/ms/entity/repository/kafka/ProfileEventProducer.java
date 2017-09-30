package com.icthh.xm.ms.entity.repository.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.icthh.xm.commons.logging.util.MDCUtil;
import com.icthh.xm.ms.entity.config.tenant.TenantContext;
import com.icthh.xm.ms.entity.domain.Profile;
import com.icthh.xm.ms.entity.domain.XmEntity;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Kafka event producer for {@link com.icthh.xm.ms.entity.domain.Profile}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileEventProducer {

    private final KafkaTemplate<String, String> template;
    private final ObjectMapper mapper = new ObjectMapper().configure(
        SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false).registerModule(new JavaTimeModule());

    @Value("${spring.application.name}")
    private String appName;

    @Value("${application.kafka-system-queue}")
    private String topicName;

    /**
     * Build message content for kafka's event.
     * @param profile data for kafka message content
     * @param eventType event type for kafka message content
     * @return event content
     */
    public String createEventJson(Profile profile, String eventType) {
        try {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("eventId", MDCUtil.getRid());
            map.put("messageSource", appName);
            map.put("tenantInfo", TenantContext.getCurrent());
            map.put("eventType", eventType);
            map.put("startDate", Instant.now().toString());
            map.put("data", buildDataContent(profile));
            return mapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            log.warn("Error creating profile event", e);
        }
        return null;
    }

    /**
     * Send event to kafka.
     * @param content the event data
     */
    @Async
    public void send(String content) {
        if (StringUtils.isNoneBlank(content)) {
            log.debug("Sending kafka event with data {} to topic {}", content, topicName);
            template.send(topicName, content);
        }
    }

    private Map<String, Object> buildDataContent(Profile profile) {
        XmEntity entity = profile.getXmentity();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", entity.getId());
        data.put("key", entity.getKey());
        data.put("name", entity.getName());
        data.put("imageUrl", entity.getAvatarUrl());
        data.put("createdDate", entity.getStartDate());
        data.put("lastModifiedDate", entity.getUpdateDate());
        data.put("userKey", profile.getUserKey());
        return data;
    }
}
