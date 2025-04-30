package com.icthh.xm.ms.entity.lep;

import com.icthh.xm.ms.entity.domain.XmEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
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
