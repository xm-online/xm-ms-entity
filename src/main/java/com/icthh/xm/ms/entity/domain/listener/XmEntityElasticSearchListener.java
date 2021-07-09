package com.icthh.xm.ms.entity.domain.listener;

import static com.icthh.xm.ms.entity.util.DatabaseUtil.runAfterTransaction;

import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import com.icthh.xm.ms.entity.lep.ElasticIndexManager;
import com.icthh.xm.ms.entity.lep.ElasticIndexManagerService;
import com.icthh.xm.ms.entity.repository.search.XmEntitySearchRepository;
import com.icthh.xm.ms.entity.service.XmEntitySpecService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import javax.annotation.PostConstruct;
import javax.persistence.PostPersist;
import javax.persistence.PostRemove;
import javax.persistence.PostUpdate;

@Slf4j
@Component
public class XmEntityElasticSearchListener {

    private static ElasticIndexManagerService elasticIndexManagerService;

    private static XmEntitySpecService xmEntitySpecService;

    @Autowired
    public void setElasticIndexManagerService(ElasticIndexManagerService elasticIndexManagerService) {
        this.elasticIndexManagerService = elasticIndexManagerService;
    }

    @Autowired
    public void setXmEntitySpecService(XmEntitySpecService xmEntitySpecService) {
        this.xmEntitySpecService = xmEntitySpecService;
    }

    @PostConstruct
    public void init() {
        log.info("Initializing Listener for XmEntity [{}, {}]", elasticIndexManagerService, xmEntitySpecService);
    }

    @PostPersist
    @PostUpdate
    void onPostPersistOrUpdate(XmEntity entity) {
        if (isFeatureEnabled(entity, TypeSpec::getIndexAfterSaveEnabled)) {
            log.debug("START: Add to save xm entity to elastic {}", entity);
            StopWatch stopWatch = StopWatch.createStarted();
            elasticIndexManagerService.addEntityToSave(entity);
            log.debug("STOP: Add to xm entity to elastic {}", stopWatch.getTime(TimeUnit.MILLISECONDS));
        }
    }

    @PostRemove
    void onPostRemove(XmEntity entity) {
        if(isFeatureEnabled(entity, TypeSpec::getIndexAfterDeleteEnabled)){
            log.debug("Delete xm entity from elastic {}", entity);
            elasticIndexManagerService.addEntityToDelete(entity);
        }
    }

    private boolean isFeatureEnabled(XmEntity entity, Function<TypeSpec, Boolean> flag) {
        return xmEntitySpecService.getTypeSpecByKey(entity.getTypeKey())
                                  .map(flag)
                                  .orElse(false);
    }
}
