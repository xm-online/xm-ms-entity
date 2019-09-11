package com.icthh.xm.ms.entity.service.tenant.provisioner;

import com.icthh.xm.commons.gen.model.Tenant;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.tenantendpoint.provisioner.TenantProvisioner;
import com.icthh.xm.ms.entity.config.Constants;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

@Service
@Slf4j
@AllArgsConstructor
public class TenantElasticsearchProvisioner implements TenantProvisioner {

    private final ElasticsearchTemplate elasticsearchTemplate;
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

    private static void forEachDomainDocument(Consumer<BeanDefinition> consumer) {
        ClassPathScanningCandidateComponentProvider provider =
            new ClassPathScanningCandidateComponentProvider(false);
        provider.addIncludeFilter(new AnnotationTypeFilter(Document.class));
        provider.findCandidateComponents(Constants.DOMAIN_PACKAGE).forEach(consumer);
    }

    private void createTenantDocuments(Tenant tenant) {
        forEachDomainDocument(beanDefinition -> {
            try {
                Class<?> cl = Class.forName(beanDefinition.getBeanClassName());
                elasticsearchTemplate.createIndex(cl);
                elasticsearchTemplate.putMapping(cl);
                log.info("created elasticsearch index for class: {}", cl);
            } catch (ClassNotFoundException e) {
                log.error("Error while index {} creation for tenant {}",
                          beanDefinition.getBeanClassName(),
                          tenant.getTenantKey());
                throw new IllegalStateException(e);
            }
        });
    }

    private void deleteTenantDocuments(String tenantKey) {
        forEachDomainDocument(beanDefinition -> {
            try {
                Class<?> cl = Class.forName(beanDefinition.getBeanClassName());
                elasticsearchTemplate.deleteIndex(cl);
                log.info("deleted elasticsearch index for class: {}", cl);
            } catch (ClassNotFoundException e) {
                log.error("Error while index {} deletion for tenant {}", beanDefinition.getBeanClassName(), tenantKey);
                throw new IllegalStateException(e);
            }
        });
    }

}



