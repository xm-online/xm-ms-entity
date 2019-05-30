package com.icthh.xm.ms.entity.service;

import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.ext.IdOrKey;
import com.icthh.xm.ms.entity.lep.keyresolver.ChangeStateTargetStateLepKeyResolver;
import com.icthh.xm.ms.entity.lep.keyresolver.ChangeStateTransitionLepKeyResolver;
import com.icthh.xm.ms.entity.lep.keyresolver.TargetEntityTypeLepKeyResolver;
import com.icthh.xm.ms.entity.lep.keyresolver.TypeKeyWithExtends;
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
    private final TypeKeyWithExtends typeKeyWithExtends;

    @Resource
    @Lazy
    private ChainedLifecycleLepStrategy internal;

    @LogicExtensionPoint(value = "ChangeState", resolver = ChangeStateTransitionLepKeyResolver.class)
    public XmEntity changeStateByTransition(IdOrKey idOrKey, String baseTypeKey, String xmEntityTypeKey, String prevStateKey, String nextStateKey, Map<String, Object> context) {
        if (typeKeyWithExtends.doInheritance(xmEntityTypeKey)) {
            String nextTypeKey = typeKeyWithExtends.nextTypeKey(xmEntityTypeKey);
            return internal.changeStateByTransition(idOrKey, baseTypeKey, nextTypeKey, prevStateKey, nextStateKey, context);
        } else {
            return internal.changeStateByTargetState(idOrKey, baseTypeKey, baseTypeKey, nextStateKey, context);
        }
    }

    @LogicExtensionPoint(value = "ChangeState", resolver = ChangeStateTargetStateLepKeyResolver.class)
    public XmEntity changeStateByTargetState(IdOrKey idOrKey, String baseTypeKey, String xmEntityTypeKey, String nextStateKey, Map<String, Object> context) {
        if (typeKeyWithExtends.doInheritance(xmEntityTypeKey)) {
            String nextTypeKey = typeKeyWithExtends.nextTypeKey(xmEntityTypeKey);
            return internal.changeStateByTargetState(idOrKey, baseTypeKey, nextTypeKey, nextStateKey, context);
        } else {
            return internal.changeStateByXmEntity(idOrKey, baseTypeKey, baseTypeKey, nextStateKey, context);
        }
    }

    @LogicExtensionPoint(value = "ChangeState", resolver = TargetEntityTypeLepKeyResolver.class)
    public XmEntity changeStateByXmEntity(IdOrKey idOrKey, String baseTypeKey, String xmEntityTypeKey, String nextStateKey, Map<String, Object> context) {
        if (typeKeyWithExtends.doInheritance(xmEntityTypeKey)) {
            String nextTypeKey = typeKeyWithExtends.nextTypeKey(xmEntityTypeKey);
            return internal.changeStateByXmEntity(idOrKey, baseTypeKey, nextTypeKey, nextStateKey, context);
        } else {
            return lifeCycleService.changeState(idOrKey, nextStateKey, context);
        }
    }

    @Override
    public XmEntity changeState(IdOrKey idOrKey, String xmEntityTypeKey, String prevStateKey, String nextStateKey, Map<String, Object> context) {
        return internal.changeStateByTransition(idOrKey, xmEntityTypeKey, xmEntityTypeKey, prevStateKey, nextStateKey, context);
    }

}


