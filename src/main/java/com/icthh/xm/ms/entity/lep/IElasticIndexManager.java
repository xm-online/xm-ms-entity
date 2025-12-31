package com.icthh.xm.ms.entity.lep;

import com.icthh.xm.ms.entity.domain.XmEntity;

public interface IElasticIndexManager {

    void addEntityToSave(XmEntity entity);


    void addEntityToDelete(XmEntity entity);
}
