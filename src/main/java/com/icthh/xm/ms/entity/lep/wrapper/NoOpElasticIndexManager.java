package com.icthh.xm.ms.entity.lep.wrapper;

import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.lep.ElasticIndexManager;
import com.icthh.xm.ms.entity.lep.TransactionScoped;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@TransactionScoped
@Component
@ConditionalOnProperty(name = "application.elastic-enabled", havingValue = "false")
public class NoOpElasticIndexManager implements ElasticIndexManager {

    /**
     * Add entity to save queue (stub implementation - does nothing).
     */
    public void addEntityToSave(XmEntity entity) {
        throw new UnsupportedOperationException("Elasticsearch is disabled");
    }

    /**
     * Add entity to delete queue (stub implementation - does nothing).
     */
    public void addEntityToDelete(XmEntity entity) {
        throw new UnsupportedOperationException("Elasticsearch is disabled");
    }
}
