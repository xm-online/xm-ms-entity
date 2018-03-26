package com.icthh.xm.ms.entity.service;

import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.ext.IdOrKey;
import com.icthh.xm.ms.entity.lep.keyresolver.ChangeStateTargetStateLepKeyResolver;
import com.icthh.xm.ms.entity.lep.keyresolver.ChangeStateTransitionLepKeyResolver;
import com.icthh.xm.ms.entity.lep.keyresolver.TargetEntityTypeLepKeyResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Map;


@Slf4j
@Service
@LepService(group = "lifecycle.chained")
@Transactional
@RequiredArgsConstructor
public class ChainedLifecycleLepStrategy implements LifecycleLepStrategy {

    private final XmEntityLifeCycleService lifeCycleService;

    @Resource
    @Lazy
    private ChainedLifecycleLepStrategy internal;

    @LogicExtensionPoint(value = "ChangeState", resolver = ChangeStateTransitionLepKeyResolver.class)
    public XmEntity changeStateByTransition(IdOrKey idOrKey, String xmEntityTypeKey, String prevStateKey, String nextStateKey, Map<String, Object> context) {
        return internal.changeStateByTargetState(idOrKey, xmEntityTypeKey, nextStateKey, context);
    }

    @LogicExtensionPoint(value = "ChangeState", resolver = ChangeStateTargetStateLepKeyResolver.class)
    public XmEntity changeStateByTargetState(IdOrKey idOrKey, String xmEntityTypeKey, String nextStateKey, Map<String, Object> context) {
        return internal.changeStateByXmEntity(idOrKey, xmEntityTypeKey, nextStateKey, context);
    }

    @LogicExtensionPoint(value = "ChangeState", resolver = TargetEntityTypeLepKeyResolver.class)
    public XmEntity changeStateByXmEntity(IdOrKey idOrKey, String xmEntityTypeKey, String nextStateKey, Map<String, Object> context) {
        return lifeCycleService.changeState(idOrKey, nextStateKey, context);
    }

    @Override
    public XmEntity changeState(IdOrKey idOrKey, String xmEntityTypeKey, String prevStateKey, String nextStateKey, Map<String, Object> context) {
        return internal.changeStateByTransition(idOrKey, xmEntityTypeKey, prevStateKey, nextStateKey, context);
    }

}


