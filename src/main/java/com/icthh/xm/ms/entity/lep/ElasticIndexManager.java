package com.icthh.xm.ms.entity.lep;

import com.icthh.xm.ms.entity.domain.XmEntity;

public interface ElasticIndexManager {

    void addEntityToSave(XmEntity entity);


    void addEntityToDelete(XmEntity entity);
}
