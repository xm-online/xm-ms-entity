package com.icthh.xm.ms.entity.lep;

import static com.icthh.xm.ms.entity.lep.XmLepScriptConstants.BINDING_KEY_REPOSITORIES;
import static com.icthh.xm.ms.entity.lep.XmLepScriptConstants.BINDING_KEY_SERVICES;
import static com.icthh.xm.ms.entity.lep.XmLepScriptConstants.BINDING_KEY_TENANT;
import static com.icthh.xm.ms.entity.lep.XmLepScriptConstants.BINDING_KEY_TENANT_NAME;
import static com.icthh.xm.ms.entity.lep.XmLepScriptConstants.BINDING_SUB_KEY_REPOSITORY_XM_ENTITY;
import static com.icthh.xm.ms.entity.lep.XmLepScriptConstants.BINDING_SUB_KEY_SERVICE_XM_ENTITY;
import static com.icthh.xm.ms.entity.lep.XmLepScriptConstants.BINDING_SUB_KEY_SERVICE_XM_TENANT_LC;

import com.icthh.lep.api.ContextScopes;
import com.icthh.lep.api.LepManager;
import com.icthh.lep.api.LepProcessingEvent;
import com.icthh.lep.api.LepProcessingEvent.BeforeExecutionEvent;
import com.icthh.lep.api.LepProcessingListener;
import com.icthh.lep.api.ScopedContext;
import com.icthh.xm.ms.entity.config.tenant.TenantInfo;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
import com.icthh.xm.ms.entity.service.XmTenantLifecycleService;
import com.icthh.xm.ms.entity.service.api.XmEntityService;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;

/**
 * The {@link XmLepProcessingListener} class.
 */
@RequiredArgsConstructor
public class XmLepProcessingListener implements LepProcessingListener {

    private final XmEntityService xmEntityService;
    private final XmTenantLifecycleService xmTenantLifecycleService;
    private final XmEntityRepository xmEntityRepository;

    @Override
    public void accept(LepProcessingEvent event) {
        if (event instanceof BeforeExecutionEvent) {
            onBeforeExecutionEvent(BeforeExecutionEvent.class.cast(event));
        }
    }

    /**
     * Init execution context for script variables bindings.
     *
     * @param event the BeforeExecutionEvent
     */
    private void onBeforeExecutionEvent(BeforeExecutionEvent event) {
        LepManager manager = event.getSource();
        ScopedContext threadContext = manager.getContext(ContextScopes.THREAD);
        if (threadContext == null) {
            throw new IllegalStateException("LEP manager tenant context doesn't initialized");
        }

        TenantInfo ti = threadContext.getValue(XmLepConstants.CONTEXT_KEY_TENANT, TenantInfo.class);
        if (ti == null) {
            throw new IllegalStateException("LEP manager thread context doesn't have value for '"
                                                + XmLepConstants.CONTEXT_KEY_TENANT + "'");
        }

        ScopedContext executionContext = manager.getContext(ContextScopes.EXECUTION);
        // add tenant
        executionContext.setValue(BINDING_KEY_TENANT, ti);
        executionContext.setValue(BINDING_KEY_TENANT_NAME, ti.getTenant());
        addServices(executionContext);
        addRepositories(executionContext);
    }

    private void addServices(ScopedContext executionContext) {
        Map<String, Object> services = new HashMap<>();
        services.put(BINDING_SUB_KEY_SERVICE_XM_ENTITY, xmEntityService);
        services.put(BINDING_SUB_KEY_SERVICE_XM_TENANT_LC, xmTenantLifecycleService);

        executionContext.setValue(BINDING_KEY_SERVICES, services);
    }

    private void addRepositories(ScopedContext executionContext) {
        Map<String, Object> repositories = new HashMap<>();
        repositories.put(BINDING_SUB_KEY_REPOSITORY_XM_ENTITY, xmEntityRepository);

        executionContext.setValue(BINDING_KEY_REPOSITORIES, repositories);
    }

}
