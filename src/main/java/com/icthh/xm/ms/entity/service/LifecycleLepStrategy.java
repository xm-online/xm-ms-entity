package com.icthh.xm.ms.entity.service;

import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.ext.IdOrKey;

import java.util.Map;

public interface LifecycleLepStrategy {

    XmEntity changeState(IdOrKey idOrKey, String xmEntityTypeKey, String prevStateKey, String nextStateKey, Map<String, Object> context);

}
