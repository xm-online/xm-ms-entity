package com.icthh.xm.ms.entity.service;

import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.commons.logging.LoggingAspectConfig;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.ext.IdOrKey;

import java.util.Map;

/**
 * The {@link XmEntityLifeCycleService} interface.
 */
@LepService(group = "lifecycle")
public interface XmEntityLifeCycleService {

    @LoggingAspectConfig(resultDetails = false)
    @LogicExtensionPoint(value = "ChangeState")
    XmEntity changeState(IdOrKey idOrKey, String nextStateKey, Map<String, Object> context);

}
