package com.icthh.xm.ms.entity.repository.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.icthh.xm.commons.logging.util.MdcUtils;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.ms.entity.domain.Profile;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.kafka.SystemEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

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

    private final TenantContextHolder tenantContextHolder;
    private final XmAuthenticationContextHolder authContextHolder;

    @Value("${spring.application.name}")
    private String appName;

    @Value("${application.kafka-system-queue}")
    private String topicName;

    /**
     * Build message content for kafka's event.
     *
     * @param profile   data for kafka message content
     * @param eventType event type for kafka message content
     * @return event content
     */
    // FIXME must not be public: this is internal implementation protocol
    public String createEventJson(Profile profile, String eventType) {
        SystemEvent event = null;
        try {
            event = buildSystemEvent(eventType);
            event.setData(buildDataContent(profile));
            return mapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            log.warn("Event creation error, eventType = {}, data = {}", eventType, event.getData(), e);
        }
        return null;
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

    /**
     * Send event to XM System Queue.
     *
     * @param content the event data
     */
    // FIXME internal implementation protocol (JSON) should be encapsulated
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

    private SystemEvent buildSystemEvent(String eventType) {
        SystemEvent event = new SystemEvent();
        event.setEventId(MdcUtils.getRid());
        event.setMessageSource(appName);
        event.setEventType(eventType);
        event.setTenantKey(TenantContextUtils.getRequiredTenantKeyValue(tenantContextHolder));
        event.setUserLogin(authContextHolder.getContext().getRequiredLogin());
        event.setStartDate(Instant.now().toString());
        return event;
    }
}
