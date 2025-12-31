package com.icthh.xm.ms.entity.service.tenant.provisioner.wrapper;

import com.icthh.xm.commons.gen.model.Tenant;
import com.icthh.xm.commons.tenantendpoint.provisioner.TenantProvisioner;
import com.icthh.xm.ms.entity.service.tenant.provisioner.TenantElasticsearchProvisioner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service("tenantElasticsearchProvisioner")
@Slf4j
@ConditionalOnProperty(name = "application.elastic-enabled", havingValue = "false")
public class NoOpTenantElasticsearchProvisioner implements TenantProvisioner {

    /**
     * Create elastic indexes for tenant (stub implementation - does nothing).
     */
    @Override
    public void createTenant(final Tenant tenant) {
        log.info("Elasticsearch is disabled. Skipping tenant creation for: {}", tenant.getTenantKey());
    }

    @Override
    public void manageTenant(final String tenantKey, final String state) {
        log.info("Elasticsearch is disabled. Skipping tenant management for: {}, state: {}", tenantKey, state);
    }

    /**
     * Delete indexes for tenant (stub implementation - does nothing).
     */
    @Override
    public void deleteTenant(final String tenantKey) {
        log.info("Elasticsearch is disabled. Skipping tenant deletion for: {}", tenantKey);
    }
}
