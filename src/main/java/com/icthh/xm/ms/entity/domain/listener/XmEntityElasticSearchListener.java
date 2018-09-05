package com.icthh.xm.ms.entity.domain.listener;

import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.repository.search.XmEntitySearchRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.persistence.PostPersist;
import javax.persistence.PostRemove;
import javax.persistence.PostUpdate;

@Slf4j
@Component
public class XmEntityElasticSearchListener {

    private static XmEntitySearchRepository xmEntitySearchRepository;

    @Autowired
    public void setXmEntitySearchRepository(XmEntitySearchRepository xmEntitySearchRepository) {
        this.xmEntitySearchRepository = xmEntitySearchRepository;
    }

    @PostConstruct
    public void init() {
        log.info("Initializing XmEntity for Listener ["+ xmEntitySearchRepository +"]");
    }

    @PostPersist
    @PostUpdate
    void onPostPersistOrUpdate(XmEntity entity) {
        log.debug("Save xm entity to elastic {}", entity);
        xmEntitySearchRepository.save(entity);
    }

    @PostRemove
    void onPostRemove(XmEntity entity) {
        log.debug("Delete xm entity from elastic {}", entity);
        xmEntitySearchRepository.delete(entity);
    }

}
