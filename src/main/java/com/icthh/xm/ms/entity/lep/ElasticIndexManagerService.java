package com.icthh.xm.ms.entity.lep;

import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.repository.search.XmEntitySearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.transaction.support.TransactionSynchronizationManager.bindResource;
import static org.springframework.transaction.support.TransactionSynchronizationManager.getResource;
import static org.springframework.transaction.support.TransactionSynchronizationManager.hasResource;
import static org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization;

@Slf4j
@RequiredArgsConstructor
@Service
@Scope
public abstract class ElasticIndexManagerService {

    public abstract ElasticIndexManager getElasticIndexManager();

    public void addEntityToSave(XmEntity entity) {
        getElasticIndexManager().addEntityToSave(entity);
    }

    public void addEntityToDelete(XmEntity entity) {
        getElasticIndexManager().addEntityToDelete(entity);
    }

}
