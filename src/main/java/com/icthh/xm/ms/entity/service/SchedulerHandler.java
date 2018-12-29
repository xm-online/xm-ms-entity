package com.icthh.xm.ms.entity.service;

import com.icthh.xm.commons.scheduler.domain.ScheduledEvent;
import com.icthh.xm.commons.scheduler.service.SchedulerEventHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SchedulerHandler implements SchedulerEventHandler {

    @Override
    public void onEvent(ScheduledEvent scheduledEvent) {
        log.info("Receive event {}", scheduledEvent);
    }
}
