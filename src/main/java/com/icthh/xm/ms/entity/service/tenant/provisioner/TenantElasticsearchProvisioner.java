package com.icthh.xm.ms.entity.service.tenant.provisioner;

import com.icthh.xm.commons.gen.model.Tenant;
import com.icthh.xm.commons.search.ElasticsearchOperations;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.tenantendpoint.provisioner.TenantProvisioner;
import com.icthh.xm.ms.entity.domain.XmEntity;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service("tenantElasticsearchProvisioner")
@Slf4j
@AllArgsConstructor
@ConditionalOnProperty(name = "application.elastic-enabled", havingValue = "true", matchIfMissing = true)
public class TenantElasticsearchProvisioner implements TenantProvisioner {

    private final ElasticsearchOperations elasticsearchOperations;
    private final TenantContextHolder tenantContextHolder;

    /**
     * Create elastic indexes for tenant.
     *
     * @param tenant the tenant
     */
    @Override
    public void createTenant(final Tenant tenant) {
        executeInTenantContext(tenant.getTenantKey(), () -> createTenantDocuments(tenant));
    }

    @Override
    public void manageTenant(final String tenantKey, final String state) {
        log.info("Nothing to do with Elasticsearch during manage tenant: {}, state = {}", tenantKey, state);
    }

    /**
     * Delete indexes for tenant.
     *
     * @param tenantKey tenant key
     */
    @Override
    public void deleteTenant(final String tenantKey) {
        executeInTenantContext(tenantKey, () -> deleteTenantDocuments(tenantKey));
    }

    private void executeInTenantContext(String tenantKey, Runnable runnable) {
        tenantContextHolder.getPrivilegedContext().execute(TenantContextUtils.buildTenant(tenantKey), runnable);
    }

    private void createTenantDocuments(Tenant tenant) {
        String idxKey = elasticsearchOperations.composeIndexName(tenant.getTenantKey());
        elasticsearchOperations.createIndex(idxKey);
        elasticsearchOperations.putMapping(XmEntity.class);
        log.info("created elasticsearch index for class: {}", XmEntity.class);
    }

    private void deleteTenantDocuments(String tenantKey) {
        String idxKey = elasticsearchOperations.composeIndexName(tenantKey);
        elasticsearchOperations.deleteIndex(idxKey);
    }
}



