package com.icthh.xm.ms.entity.lep;

import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.repository.search.XmEntitySearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization;

@Slf4j
@RequiredArgsConstructor
@TransactionScoped
@Component
@ConditionalOnProperty(name = "application.elastic-enabled", havingValue = "true", matchIfMissing = true)
public class ElasticIndexManager implements IElasticIndexManager {

    private final XmEntitySearchRepository searchRepository;
    private final List<XmEntity> entityToSave = new ArrayList<>();
    private final List<XmEntity> entityToDelete = new ArrayList<>();

    public void addEntityToSave(XmEntity entity) {
        if (entityToSave.isEmpty()) {
            registerSynchronization(new TransactionSynchronizationAdapter() {
                @Override
                public void afterCompletion(int status) {
                    super.afterCompletion(status);
                    if (status == STATUS_COMMITTED) {
                        try {
                            if (!entityToSave.isEmpty()) {
                                searchRepository.saveAll(entityToSave);
                            }
                        } catch (Exception e) {
                            log.error("Error in afterCompletion stage during transaction synchronization for entity: {}",
                                    entityToSave, e);
                        }
                    }
                    entityToSave.clear();
                }
            });
        }
        entityToSave.add(entity);
    }

    public void addEntityToDelete(XmEntity entity) {
        if (entityToDelete.isEmpty()) {
            registerSynchronization(new TransactionSynchronizationAdapter() {
                @Override
                public void afterCompletion(int status) {
                    super.afterCompletion(status);
                    if (status == STATUS_COMMITTED) {
                        try {
                            if (!entityToDelete.isEmpty()) {
                                searchRepository.deleteAll(entityToDelete);
                            }
                        } catch (Exception e) {
                            log.error("Error in afterCompletion stage during transaction synchronization for entity: {}",
                                    entityToDelete, e);
                        }
                    }
                    entityToDelete.clear();
                }
            });
        }
        entityToDelete.add(entity);
    }
}
