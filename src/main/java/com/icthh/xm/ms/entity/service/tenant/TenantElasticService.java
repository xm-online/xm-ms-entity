package com.icthh.xm.ms.entity.service.tenant;

import static org.apache.commons.lang3.time.StopWatch.createStarted;

import com.icthh.xm.commons.gen.model.Tenant;
import com.icthh.xm.commons.logging.aop.IgnoreLogginAspect;
import com.icthh.xm.commons.tenant.PrivilegedTenantContext;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.ms.entity.config.Constants;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
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
@IgnoreLogginAspect
public class TenantElasticService {

    private final ElasticsearchTemplate elasticsearchTemplate;
    private final TenantContextHolder tenantContextHolder;

    private PrivilegedTenantContext getPrivilegedTenantContext() {
        return tenantContextHolder.getPrivilegedContext();
    }

    private void executeInTenantContext(String tenantKey, Runnable runnable) {
        getPrivilegedTenantContext().execute(TenantContextUtils.buildTenant(tenantKey), runnable);
    }

    private static void forEachDomainDocument(Consumer<BeanDefinition> consumer) {
        ClassPathScanningCandidateComponentProvider provider =
            new ClassPathScanningCandidateComponentProvider(false);
        provider.addIncludeFilter(new AnnotationTypeFilter(Document.class));
        provider.findCandidateComponents(Constants.DOMAIN_PACKAGE).forEach(consumer);
    }

    /**
     * Create elastic indexes for tenant.
     *
     * @param tenant the tenant
     */
    public void create(Tenant tenant) {
        final StopWatch stopWatch = createStarted();
        log.info("START - SETUP:CreateTenant:elastic index={}", tenant.getTenantKey());
        executeInTenantContext(tenant.getTenantKey(), () -> {
            createTenant(tenant);
        });
        log.info("STOP - SETUP:CreateTenant:elastic index={}, time={}ms", tenant.getTenantKey(),
                 stopWatch.getTime());
    }

    private void createTenant(Tenant tenant) {
        forEachDomainDocument(beanDefinition -> {
            try {
                Class<?> cl = Class.forName(beanDefinition.getBeanClassName());
                elasticsearchTemplate.createIndex(cl);
                elasticsearchTemplate.putMapping(cl);
            } catch (ClassNotFoundException e) {
                log.error("Error while index {} creation for tenant {}",
                          beanDefinition.getBeanClassName(),
                          tenant.getTenantKey());
                throw new IllegalStateException(e);
            }
        });
    }

    /**
     * Delete indexes for tenant.
     *
     * @param tenantKey tenant key
     */
    public void delete(final String tenantKey) {
        final StopWatch stopWatch = createStarted();
        log.info("START - SETUP:DeleteTenant:elastic index={}", tenantKey);
        executeInTenantContext(tenantKey, () -> {
            deleteTenant(tenantKey);
        });
        log.info("STOP - SETUP:DeleteTenant:elastic index={}, time={}ms", tenantKey, stopWatch.getTime());
    }

    private void deleteTenant(String tenantKey) {
        forEachDomainDocument(beanDefinition -> {
            try {
                Class<?> cl = Class.forName(beanDefinition.getBeanClassName());
                elasticsearchTemplate.deleteIndex(cl);
            } catch (ClassNotFoundException e) {
                log.error("Error while index {} deletion for tenant {}",
                          beanDefinition.getBeanClassName(), tenantKey);
                throw new IllegalStateException(e);
            }
        });
    }

}



