package com.icthh.xm.ms.entity.domain.listener;

import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.listener.handler.XmEntityPersistenceEventHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.persistence.CascadeType;
import javax.persistence.PostPersist;
import javax.persistence.PostRemove;
import javax.persistence.PostUpdate;

@Slf4j
@Component
public class XmEntityPersistenceListener {

    private static XmEntityPersistenceEventHandler eventHandler;

    @Autowired
    public void setEventHandler(XmEntityPersistenceEventHandler eventHandler) {
        XmEntityPersistenceListener.eventHandler = eventHandler;
    }

    @PostPersist
    public void onPersist(XmEntity xmEntity) {
        eventHandler.onPersistenceEvent(xmEntity, CascadeType.PERSIST);
    }

    @PostUpdate
    void onUpdate(XmEntity xmEntity) {
        eventHandler.onPersistenceEvent(xmEntity, CascadeType.MERGE);
    }

    @PostRemove
    void onRemove(XmEntity xmEntity) {
        eventHandler.onPersistenceEvent(xmEntity, CascadeType.REMOVE);
    }
}
