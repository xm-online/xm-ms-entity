package com.icthh.xm.ms.entity.service;

import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.ext.IdOrKey;
import com.icthh.xm.ms.entity.lep.LogicExtensionPoint;
import com.icthh.xm.ms.entity.lep.spring.LepService;

import java.util.Map;

/**
 * The {@link XmEntityLifeCycleService} interface.
 */
@LepService(group = "xm.entity.lifecycle")
public interface XmEntityLifeCycleService {

    @LogicExtensionPoint(value = "ChangeState")
    XmEntity changeState(IdOrKey idOrKey, String nextStateKey, Map<String, Object> context);

    // TODO add LEP for specific entity-type for example ChangeState$$ACCOUNT$ADMIN$$default.groovy
    //@LogicExtensionPoint(value = "ChangeState", resolver = ChangeStateLepKeyResolver.class)
    //void changeState(Long xmEntityId, String xmEntityTypeKey, String nextStateKey, Map<String, Object> context);

    // TODO add LEP implementation
    //@LogicExtensionPoint(value = "OnStateTransition", resolver = OnStateTransitionLepKeyResolver.class)
    //void onStateTransition(Long xmEntityId, String xmEntityTypeKey, XmEntity currentState, XmEntity nextState);

}
