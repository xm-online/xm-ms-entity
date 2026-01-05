package com.icthh.xm.ms.entity.lep.wrapper;

import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.lep.ElasticIndexManager;
import com.icthh.xm.ms.entity.lep.IElasticIndexManager;
import com.icthh.xm.ms.entity.lep.TransactionScoped;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@TransactionScoped
@Component
@ConditionalOnProperty(name = "application.elastic-enabled", havingValue = "false")
public class NoOpElasticIndexManager implements IElasticIndexManager {

    /**
     * Add entity to save queue (stub implementation - does nothing).
     */
    public void addEntityToSave(XmEntity entity) {
        log.error("Elasticsearch is disabled. Skipping entity save to elastic: {}", entity.getId());
    }

    /**
     * Add entity to delete queue (stub implementation - does nothing).
     */
    public void addEntityToDelete(XmEntity entity) {
        log.error("Elasticsearch is disabled. Skipping entity delete from elastic: {}", entity.getId());
    }
}
