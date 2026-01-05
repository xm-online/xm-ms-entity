package com.icthh.xm.ms.entity.domain.listener;

import static com.icthh.xm.ms.entity.util.DatabaseUtil.runAfterTransaction;

import com.icthh.xm.ms.entity.config.ApplicationProperties;
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
public class ProfileElasticSearchListener {

    private static XmEntitySearchRepository xmEntitySearchRepository;
    private static ApplicationProperties applicationProperties;

    @Autowired
    public void setXmEntitySearchRepository(XmEntitySearchRepository xmEntitySearchRepository) {
        this.xmEntitySearchRepository = xmEntitySearchRepository;
    }

    @Autowired
    public void setApplicationProperties(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
    }

    @PostConstruct
    public void init() {
        log.info("Initializing XmEntity for Listener ["+ xmEntitySearchRepository +"]");
    }

    @PostPersist
    @PostUpdate
    void onPostPersistOrUpdate(Profile profile) {
        if (applicationProperties.isElasticEnabled()) {
            log.debug("Save xm entity to elastic {}", profile.getXmentity());
            runAfterTransaction(profile.getXmentity(), xmEntitySearchRepository::save);
        }
    }

    @PostRemove
    void onPostRemove(Profile profile) {
        if (applicationProperties.isElasticEnabled()) {
            log.debug("Delete xm entity from elastic {}", profile.getXmentity());
            runAfterTransaction(profile.getXmentity(), xmEntitySearchRepository::delete);
        }
    }

}
