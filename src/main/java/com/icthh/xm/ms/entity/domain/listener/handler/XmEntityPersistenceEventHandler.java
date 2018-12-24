package com.icthh.xm.ms.entity.domain.listener.handler;

import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.ms.entity.domain.XmEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.persistence.CascadeType;

@Slf4j
@LepService(group = "listener.entity")
@Component
public class XmEntityPersistenceEventHandler {

    @LogicExtensionPoint(value = "OnPersistenceEvent")
    public void onPersistenceEvent(XmEntity xmEntity, CascadeType action) {
        log.debug("Ignore XmEntity persistence event");
    }
}
