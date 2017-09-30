package com.icthh.xm.ms.entity.service;

import com.icthh.xm.commons.gen.model.Tenant;
import com.icthh.xm.ms.entity.config.ApplicationProperties;
import com.icthh.xm.ms.entity.domain.EntityState;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.web.client.tenant.TenantClient;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class XmTenantLifecycleService {

    private static final String KEY_SERVICES = "services";

    private static final String CREATE_ACTION = "create";
    private static final String DELETE_ACTION = "delete";
    private static final String MANAGE_ACTION = "manage";

    private final List<TenantClient> clients;
    private final ApplicationProperties applicationProperties;

    /**
     * Call services during state change.
     * @param xmEntity entity
     * @param nextStateKey next state
     * @param context additional data
     */
    public void changeState(XmEntity xmEntity, String nextStateKey, Map<String, Object> context) {
        // noinspection unchecked
        List<String> services = (List<String>) context.getOrDefault(KEY_SERVICES,
                        applicationProperties.getTenantCreateServiceList());
        executeServices(services, xmEntity, nextStateKey);
    }

    private void executeServices(List<String> services, XmEntity xmEntity, String nextStateKey) {
        String action = getAction(xmEntity,nextStateKey);
        if (StringUtils.isBlank(action)) {
            log.info("Change entity state from {} to {} no need to create or delete tenant.", xmEntity.getStateKey(), nextStateKey);
            return;
        }

        boolean failed = false;
        for (String serviceName : services) {
            // skip if service was called before successfully

            Map callInfo = getServiceInfo(action, serviceName, xmEntity);
            if (!MANAGE_ACTION.equals(action) && null != callInfo.get("success") && (boolean) callInfo.get("success")) {
                continue;
            }

            boolean success = true;
            Exception ex = null;
            try {
                manageClient(serviceName, xmEntity, nextStateKey);

            } catch (Exception e) {
                log.error("Error during service {} call", serviceName, e);
                success = false;
                ex = e;
                failed = true;
            } finally {
                setServiceInfo(action, serviceName, xmEntity, new ServiceInfo(success, ex != null ? ex.getMessage() : null));
            }
        }
        setEntityState(xmEntity, nextStateKey, failed);

    }

    private void setEntityState(XmEntity xmEntity, String nextStateKey, boolean failed) {
        if (EntityState.ACTIVE.name().equals(nextStateKey) && failed) {
            xmEntity.setStateKey(EntityState.ERROR.name());
            return;
        }
        xmEntity.setStateKey(nextStateKey);
    }


    @SuppressWarnings("unchecked")
    private static Map<String, Object> getServiceInfo(String action, String serviceName, XmEntity xmEntity) {
        return (Map<String, Object>) ((Map) xmEntity.getData()
            .getOrDefault(serviceName, new HashMap<>())).getOrDefault(action, new HashMap<>());
    }

    @SuppressWarnings("unchecked")
    private static void setServiceInfo(String action, String serviceName, XmEntity xmEntity, ServiceInfo serviceInfo) {
        xmEntity.getData().putIfAbsent(serviceName, new HashMap<>());

        ((Map) xmEntity.getData().get(serviceName)).put(action, serviceInfo);
    }

    private static String getAction(XmEntity xmEntity, String nextStateKey) {
        if (EntityState.ACTIVE.name().equals(nextStateKey)
            && (EntityState.NEW.name().equals(xmEntity.getStateKey())
                || EntityState.ERROR.name().equals(xmEntity.getStateKey()))) {
            return CREATE_ACTION;
        } else if (EntityState.ACTIVE.name().equals(nextStateKey)
            && EntityState.SUSPENDED.name().equals(xmEntity.getStateKey())) {
            return MANAGE_ACTION;
        } else if (EntityState.SUSPENDED.name().equals(nextStateKey)
            && EntityState.ACTIVE.name().equals(xmEntity.getStateKey())) {
            return MANAGE_ACTION;
        } else if (EntityState.DELETED.name().equals(nextStateKey)) {
            return DELETE_ACTION;
        }
        return null;
    }

    private void manageClient(String serviceName, XmEntity xmEntity, String nextStateKey) {
        if (StringUtils.isNotBlank(serviceName)) {
            TenantClient client = getClient(serviceName);
            if (EntityState.ACTIVE.name().equals(nextStateKey)
                && (EntityState.NEW.name().equals(xmEntity.getStateKey())
                    || EntityState.ERROR.name().equals(xmEntity.getStateKey()))) {
                client.addTenant(new Tenant().tenantKey(xmEntity.getName()).name(xmEntity.getName()));
            } else if (EntityState.ACTIVE.name().equals(nextStateKey)
                && EntityState.SUSPENDED.name().equals(xmEntity.getStateKey())) {
                client.manageTenant(xmEntity.getName(), EntityState.ACTIVE.name());
            } else if (EntityState.SUSPENDED.name().equals(nextStateKey)
                && EntityState.ACTIVE.name().equals(xmEntity.getStateKey())) {
                client.manageTenant(xmEntity.getName(), EntityState.SUSPENDED.name());
            } else if (EntityState.DELETED.name().equals(nextStateKey)) {
                client.deleteTenant(xmEntity.getName());
            }
        }
    }

    private TenantClient getClient(String name) {
        for (TenantClient client : clients) {
            if (client.getName().equalsIgnoreCase(name)) {
                return client;
            }
        }
        throw new IllegalArgumentException("Service client not found: " + name);
    }

    @Value
    public static class ServiceInfo {

        private boolean success;
        private String errorMessage;

    }
}
