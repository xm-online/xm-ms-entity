package com.icthh.xm.ms.entity.service;

import com.icthh.xm.ms.entity.domain.ext.IdOrKey;
import com.icthh.xm.ms.entity.projection.XmEntityIdKeyTypeKey;
import com.icthh.xm.ms.entity.projection.XmEntityStateProjection;

import java.util.Optional;

public interface XmEntityProjectionService {

    Optional<XmEntityStateProjection> findStateProjection(IdOrKey idOrKey);

    Optional<XmEntityIdKeyTypeKey> findXmEntityIdKeyTypeKey(IdOrKey idOrKey);

}
