package com.icthh.xm.ms.entity.service;

import com.icthh.xm.commons.exceptions.EntityNotFoundException;
import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.commons.logging.LoggingAspectConfig;
import com.icthh.xm.commons.logging.aop.IgnoreLogginAspect;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.ext.IdOrKey;
import com.icthh.xm.ms.entity.lep.keyresolver.ChangeStateTargetStateLepKeyResolver;
import com.icthh.xm.ms.entity.lep.keyresolver.ChangeStateTransitionLepKeyResolver;
import com.icthh.xm.ms.entity.lep.keyresolver.TargetEntityTypeLepKeyResolver;
import com.icthh.xm.ms.entity.lep.keyresolver.TypeKeyWithExtends;
import com.icthh.xm.ms.entity.projection.XmEntityStateProjection;
import com.icthh.xm.ms.entity.security.access.DynamicPermissionCheckService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Map;
import java.util.function.Supplier;


@Slf4j
@Service
@LepService(group = "lifecycle.chained")
@Transactional
@RequiredArgsConstructor
public class ChainedLifecycleLepStrategy implements LifecycleLepStrategy {

    public static final String CHANE_STATE_PERMISSION = "XMENTITY.STATE";

    private final XmEntityLifeCycleService lifeCycleService;
    private final TypeKeyWithExtends typeKeyWithExtends;

    private final DynamicPermissionCheckService dynamicPermissionCheckService;

    private final XmEntityService xmEntityService;

    @Resource
    @Lazy
    private ChainedLifecycleLepStrategy internal;

    @IgnoreLogginAspect
    @LogicExtensionPoint(value = "ChangeState", resolver = ChangeStateTransitionLepKeyResolver.class)
    public XmEntity changeStateByTransition(IdOrKey idOrKey, String xmEntityTypeKey, String prevStateKey, String nextStateKey, Map<String, Object> context, String baseTypeKey) {
        if (typeKeyWithExtends.doInheritance(xmEntityTypeKey)) {
            String nextTypeKey = typeKeyWithExtends.nextTypeKey(xmEntityTypeKey);
            return internal.changeStateByTransition(idOrKey, nextTypeKey, prevStateKey, nextStateKey, context, baseTypeKey);
        } else {
            return internal.changeStateByTargetState(idOrKey, baseTypeKey, nextStateKey, context);
        }
    }

    @IgnoreLogginAspect
    @LogicExtensionPoint(value = "ChangeState", resolver = ChangeStateTargetStateLepKeyResolver.class)
    public XmEntity changeStateByTargetState(IdOrKey idOrKey, String xmEntityTypeKey, String nextStateKey, Map<String, Object> context, String baseTypeKey) {
        if (typeKeyWithExtends.doInheritance(xmEntityTypeKey)) {
            String nextTypeKey = typeKeyWithExtends.nextTypeKey(xmEntityTypeKey);
            return internal.changeStateByTargetState(idOrKey, nextTypeKey, nextStateKey, context, baseTypeKey);
        } else {
            return internal.changeStateByXmEntity(idOrKey, baseTypeKey, nextStateKey, context);
        }
    }

    @IgnoreLogginAspect
    @LogicExtensionPoint(value = "ChangeState", resolver = TargetEntityTypeLepKeyResolver.class)
    public XmEntity changeStateByXmEntity(IdOrKey idOrKey, String xmEntityTypeKey, String nextStateKey, Map<String, Object> context, String baseTypeKey) {
        if (typeKeyWithExtends.doInheritance(xmEntityTypeKey)) {
            String nextTypeKey = typeKeyWithExtends.nextTypeKey(xmEntityTypeKey);
            return internal.changeStateByXmEntity(idOrKey, nextTypeKey, nextStateKey, context, baseTypeKey);
        } else {
            dynamicPermissionCheckService.checkContextPermission(DynamicPermissionCheckService.FeatureContext.CHANGE_STATE,
                CHANE_STATE_PERMISSION,
                buildPrefix(idOrKey, xmEntityTypeKey, nextStateKey));
            return lifeCycleService.changeState(idOrKey, nextStateKey, context);
        }
    }

    @IgnoreLogginAspect
    @LogicExtensionPoint(value = "ChangeState", resolver = ChangeStateTransitionLepKeyResolver.class)
    public XmEntity changeStateByTransition(IdOrKey idOrKey, String xmEntityTypeKey, String prevStateKey, String nextStateKey, Map<String, Object> context) {
        return changeStateByTransition(idOrKey, xmEntityTypeKey, prevStateKey, nextStateKey, context, xmEntityTypeKey);
    }

    @IgnoreLogginAspect
    @LogicExtensionPoint(value = "ChangeState", resolver = ChangeStateTargetStateLepKeyResolver.class)
    public XmEntity changeStateByTargetState(IdOrKey idOrKey, String xmEntityTypeKey, String nextStateKey, Map<String, Object> context) {
        return changeStateByTargetState(idOrKey, xmEntityTypeKey, nextStateKey, context, xmEntityTypeKey);
    }

    @IgnoreLogginAspect
    @LogicExtensionPoint(value = "ChangeState", resolver = TargetEntityTypeLepKeyResolver.class)
    public XmEntity changeStateByXmEntity(IdOrKey idOrKey, String xmEntityTypeKey, String nextStateKey, Map<String, Object> context) {
        return changeStateByXmEntity(idOrKey, xmEntityTypeKey, nextStateKey, context, xmEntityTypeKey);
    }

    @LoggingAspectConfig(resultDetails = false)
    @Override
    public XmEntity changeState(IdOrKey idOrKey, String xmEntityTypeKey, String prevStateKey, String nextStateKey, Map<String, Object> context) {
        return internal.changeStateByTransition(idOrKey, xmEntityTypeKey, prevStateKey, nextStateKey, context);
    }

    protected Supplier<String> buildPrefix(IdOrKey idOrKey, String typeKey, String nextState) {
        return () -> {
            XmEntityStateProjection stateProjectionById = xmEntityService.findStateProjectionById(idOrKey)
                .orElseThrow(() -> new EntityNotFoundException("XmEntity with key [" + idOrKey + "] not found"));

            if (!typeKey.equals(stateProjectionById.getTypeKey())) {
                log.error("Entity typeKey={} differs from expected type {}", stateProjectionById.getTypeKey(), typeKey);
                throw new EntityNotFoundException("XmEntity with key [" + idOrKey + "] not found");
            }
            return typeKey + "." + stateProjectionById.getStateKey() + "." + nextState;
        };
    }

}


