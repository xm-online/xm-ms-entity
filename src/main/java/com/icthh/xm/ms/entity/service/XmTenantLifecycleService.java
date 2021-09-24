package com.icthh.xm.ms.entity.service;

import static com.icthh.xm.ms.entity.domain.EntityState.ACTIVE;
import static com.icthh.xm.ms.entity.domain.EntityState.DELETED;
import static com.icthh.xm.ms.entity.domain.EntityState.ERROR;
import static com.icthh.xm.ms.entity.domain.EntityState.NEW;
import static com.icthh.xm.ms.entity.domain.EntityState.SUSPENDED;

import com.google.common.collect.Sets;
import com.icthh.xm.commons.gen.model.Tenant;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.ms.entity.config.ApplicationProperties;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.web.client.tenant.TenantClient;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class XmTenantLifecycleService {

    private static final String KEY_SERVICES = "services";

    private static final String CREATE_ACTION = "create";
    private static final String DELETE_ACTION = "delete";
    private static final String MANAGE_ACTION = "manage";

    private static final Set<String> CURRENT_STATE_CREATE_ALLOWED = Sets.newHashSet(NEW.name(), ERROR.name());

    private final List<TenantClient> clients;
    private final ApplicationProperties applicationProperties;
    private final TenantContextHolder tenantContextHolder;

    /**
     * Call services during state change.
     * @param xmEntity entity
     * @param nextStateKey next state
     * @param context additional data
     */
    public void changeState(XmEntity xmEntity, String nextStateKey, Map<String, Object> context) {
        if (!tenantContextHolder.getContext().getTenant()
            .orElseThrow(() -> new IllegalArgumentException("Tenant not supplied")).isSuper()) {
            throw new IllegalArgumentException("Creating new tenants allowed only from super tenant");
        }

        // noinspection unchecked
        List<String> services = (List<String>) Optional.ofNullable(context)
                                                       .map(c -> c.get(KEY_SERVICES))
                                                       .orElse(applicationProperties.getTenantCreateServiceList());
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
                log.error("Error during service {} call: {}", serviceName, e.toString());
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
        if (ACTIVE.name().equals(nextStateKey) && failed) {
            xmEntity.setStateKey(ERROR.name());
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
        String currStateKey = xmEntity.getStateKey();
        if (ACTIVE.name().equals(nextStateKey) && CURRENT_STATE_CREATE_ALLOWED.contains(currStateKey)) {
            return CREATE_ACTION;
        } else if (ACTIVE.name().equals(nextStateKey) && SUSPENDED.name().equals(currStateKey)) {
            return MANAGE_ACTION;
        } else if (SUSPENDED.name().equals(nextStateKey) && ACTIVE.name().equals(currStateKey)) {
            return MANAGE_ACTION;
        } else if (DELETED.name().equals(nextStateKey)) {
            return DELETE_ACTION;
        }
        return null;
    }

    private void manageClient(String serviceName, XmEntity xmEntity, String nextStateKey) {
        if (StringUtils.isNotBlank(serviceName)) {
            TenantClient client = getClient(serviceName);
            String tenantName = xmEntity.getName();
            String currStateKey = xmEntity.getStateKey();
            if (ACTIVE.name().equals(nextStateKey) && CURRENT_STATE_CREATE_ALLOWED.contains(currStateKey)) {
                client.addTenant(new Tenant().tenantKey(tenantName).name(tenantName));
            } else if (ACTIVE.name().equals(nextStateKey) && SUSPENDED.name().equals(currStateKey)) {
                client.manageTenant(tenantName, ACTIVE.name());
            } else if (SUSPENDED.name().equals(nextStateKey) && ACTIVE.name().equals(currStateKey)) {
                client.manageTenant(tenantName, SUSPENDED.name());
            } else if (DELETED.name().equals(nextStateKey)) {
                client.deleteTenant(tenantName);
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
    public static class ServiceInfo implements Serializable {

        private boolean success;
        private String errorMessage;

    }
}
