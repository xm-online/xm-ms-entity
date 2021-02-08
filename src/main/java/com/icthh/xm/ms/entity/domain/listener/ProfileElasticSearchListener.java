package com.icthh.xm.ms.entity.domain.listener;

import static com.icthh.xm.ms.entity.util.DatabaseUtil.runAfterTransaction;

import com.icthh.xm.ms.entity.domain.Profile;
import com.icthh.xm.ms.entity.repository.search.XmEntitySearchRepository;
import com.icthh.xm.ms.entity.repository.search.elasticsearch.XmEntityElasticRepository;
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
    private static XmEntityElasticRepository xmEntityElasticRepository;

    @Autowired
    public void setXmEntitySearchRepository(XmEntitySearchRepository xmEntitySearchRepository) {
        this.xmEntitySearchRepository = xmEntitySearchRepository;
    }

    @Autowired
    public void setXmEntityElasticRepository(XmEntityElasticRepository xmEntityElasticRepository) {
        this.xmEntityElasticRepository = xmEntityElasticRepository;
    }

    @PostConstruct
    public void init() {
        log.info("Initializing XmEntity for Listener ["+ xmEntitySearchRepository +"]");
    }

    @PostPersist
    @PostUpdate
    void onPostPersistOrUpdate(Profile profile) {
        log.debug("Save xm entity to elastic {}", profile.getXmentity());
        runAfterTransaction(profile.getXmentity(), xmEntityElasticRepository::save);
    }

    @PostRemove
    void onPostRemove(Profile profile) {
        log.debug("Delete xm entity from elastic {}", profile.getXmentity());
        runAfterTransaction(profile.getXmentity(), xmEntityElasticRepository::delete);
    }

}
