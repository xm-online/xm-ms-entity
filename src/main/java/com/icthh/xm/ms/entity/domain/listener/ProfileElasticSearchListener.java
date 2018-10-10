package com.icthh.xm.ms.entity.domain.listener;

import com.icthh.xm.ms.entity.domain.Profile;
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
public class ProfileElasticSearchListener {

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
    void onPostPersistOrUpdate(Profile profile) {
        log.debug("Save xm entity to elastic {}", profile.getXmentity());
        xmEntitySearchRepository.save(profile.getXmentity());
    }

    @PostRemove
    void onPostRemove(Profile profile) {
        log.debug("Delete xm entity from elastic {}", profile.getXmentity());
        xmEntitySearchRepository.delete(profile.getXmentity());
    }

}
