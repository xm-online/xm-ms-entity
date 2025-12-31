package com.icthh.xm.ms.entity.domain.listener;

import static com.icthh.xm.ms.entity.util.DatabaseUtil.runAfterTransaction;

import com.icthh.xm.ms.entity.domain.Profile;
import com.icthh.xm.ms.entity.repository.search.XmEntitySearchRepository;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "application.elastic-enabled", havingValue = "true", matchIfMissing = true)
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
        runAfterTransaction(profile.getXmentity(), xmEntitySearchRepository::save);
    }

    @PostRemove
    void onPostRemove(Profile profile) {
        log.debug("Delete xm entity from elastic {}", profile.getXmentity());
        runAfterTransaction(profile.getXmentity(), xmEntitySearchRepository::delete);
    }

}
