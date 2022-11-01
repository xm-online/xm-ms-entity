package com.icthh.xm.ms.entity.repository;

import com.icthh.xm.ms.entity.projection.XmEntityIdKeyTypeKey;
import com.icthh.xm.ms.entity.projection.XmEntityStateProjection;

public interface XmEntityProjectionRepository {

    XmEntityStateProjection findStateProjectionByKey(String key);

    XmEntityStateProjection findStateProjectionById(Long id);

    XmEntityIdKeyTypeKey findOneIdKeyTypeKeyByKey(String key);

    XmEntityIdKeyTypeKey findOneIdKeyTypeKeyById(Long id);

}
