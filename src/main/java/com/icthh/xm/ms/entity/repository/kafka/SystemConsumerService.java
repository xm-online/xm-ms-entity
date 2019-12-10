package com.icthh.xm.ms.entity.repository.kafka;


import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.ms.entity.domain.kafka.SystemEvent;
import com.icthh.xm.ms.entity.lep.keyresolver.SystemQueueConsumerLepKeyResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@LepService(group = "queue.system")
@Transactional
public class SystemConsumerService {

    @LogicExtensionPoint(value = "AcceptSystemEvent", resolver = SystemQueueConsumerLepKeyResolver.class)
    public void acceptSystemEvent(SystemEvent event) {
        log.warn("System event type {} not supported.", event.getEventType());
    }

}
