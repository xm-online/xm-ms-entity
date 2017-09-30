package com.icthh.xm.ms.entity.repository.kafka;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.icthh.xm.commons.logging.util.MDCUtil;
import com.icthh.xm.ms.entity.config.Constants;
import com.icthh.xm.ms.entity.config.tenant.TenantContext;
import com.icthh.xm.ms.entity.domain.Profile;
import com.icthh.xm.ms.entity.domain.kafka.SystemEvent;
import com.icthh.xm.ms.entity.repository.util.SystemEventMapper;
import com.icthh.xm.ms.entity.service.ProfileService;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SystemQueueConsumer {

    private final ProfileService profileService;

    public SystemQueueConsumer(ProfileService profileService) {
        this.profileService = profileService;
    }

    /**
     * Consume system event message.
     * @param message the system event message
     */
    @Retryable(maxAttemptsExpression = "${application.retry.max-attempts}",
        backoff = @Backoff(delayExpression = "${application.retry.delay}",
            multiplierExpression = "${application.retry.multiplier}"))
    public void consumeEvent(ConsumerRecord<String, String> message) {
        MDCUtil.put();
        try {
            log.info("Input message {}", message);
            ObjectMapper mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            mapper.registerModule(new JavaTimeModule());
            try {
                SystemEvent event = mapper.readValue(message.value(), SystemEvent.class);
                String command = event.getEventType();
                String userKey = String.valueOf(event.getData().get("userKey"));
                TenantContext.setCurrent(event.getTenantInfo());
                switch (command.toUpperCase()) {
                    case Constants.CREATE_PROFILE:
                        log.info("Start to create profile for userKey='{}'", userKey);
                        Profile newProfile = profileService.getProfile(userKey);
                        if (newProfile != null) {
                            log.error("Failed to create profile. Profile with userKey='{}' already exists.", userKey);
                            break;
                        }
                        newProfile = new Profile();
                        SystemEventMapper.toProfile(event, newProfile);
                        profileService.save(newProfile);
                        break;
                    case Constants.UPDATE_PROFILE:
                        log.info("Start to update profile for userKey='{}'", userKey);
                        Profile oldProfile = profileService.getProfile(userKey);
                        if (null == oldProfile) {
                            log.error("Failed to update profile. Profile with userKey='{}' does not exists.", userKey);
                            break;
                        }
                        SystemEventMapper.toProfile(event, oldProfile);
                        profileService.save(oldProfile);
                        break;
                    default:
                        break;
                }

            } catch (IOException e) {
                log.error("Kafka message has incorrect format ", e);
            }
        } finally {
            MDCUtil.remove();
        }
    }

}
