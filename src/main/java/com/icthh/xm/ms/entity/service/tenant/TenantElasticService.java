package com.icthh.xm.ms.entity.service.tenant;

import static org.apache.commons.lang3.time.StopWatch.createStarted;

import com.icthh.xm.commons.gen.model.Tenant;
import com.icthh.xm.commons.logging.aop.IgnoreLogginAspect;
import com.icthh.xm.ms.entity.config.Constants;
import com.icthh.xm.ms.entity.config.tenant.TenantContext;
import com.icthh.xm.ms.entity.config.tenant.TenantInfo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@AllArgsConstructor
@IgnoreLogginAspect
public class TenantElasticService {
    private ElasticsearchTemplate elasticsearchTemplate;

    /**
     * Create elastic indexes for tenant.
     *
     * @param tenant - the tenant
     */
    public void create(Tenant tenant) {
        StopWatch stopWatch = createStarted();
        log.info("START - SETUP:CreateTenant:elastic indexes={}", tenant.getTenantKey());
        TenantInfo info = TenantContext.getCurrent();
        try {
            TenantContext.setCurrentQuite(tenant.getTenantKey());

            ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
            provider.addIncludeFilter(new AnnotationTypeFilter(Document.class));
            provider.findCandidateComponents(Constants.DOMAIN_PACKAGE).forEach(beanDefinition -> {
                try {
                    Class<?> cl = Class.forName(beanDefinition.getBeanClassName());
                    elasticsearchTemplate.createIndex(cl);
                } catch (ClassNotFoundException e) {
                    log.error("Error while index {} creation for tenant {}",
                        beanDefinition.getBeanClassName(), tenant.getTenantKey());
                    throw new RuntimeException(e);
                }
            });
        } finally {
            TenantContext.setCurrentQuite(info);
        }
        log.info("STOP - SETUP:CreateTenant:elastic indexes={}, time={}ms", tenant.getTenantKey(),
            stopWatch.getTime());
    }

    /**
     * Delete indexes for tenant.
     *
     * @param tenantKey tenant key
     */
    public void delete(String tenantKey) {
        StopWatch stopWatch = createStarted();
        log.info("START - SETUP:DeleteTenant:elastic indexes={}", tenantKey);
        TenantInfo info = TenantContext.getCurrent();
        try {
            TenantContext.setCurrentQuite(tenantKey);

            ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
            provider.addIncludeFilter(new AnnotationTypeFilter(Document.class));
            provider.findCandidateComponents(Constants.DOMAIN_PACKAGE).forEach(beanDefinition -> {
                try {
                    Class<?> cl = Class.forName(beanDefinition.getBeanClassName());
                    elasticsearchTemplate.deleteIndex(cl);
                } catch (ClassNotFoundException e) {
                    log.error("Error while index {} deletion for tenant {}",
                        beanDefinition.getBeanClassName(), tenantKey);
                    throw new RuntimeException(e);
                }
            });
        } finally {
            TenantContext.setCurrentQuite(info);
        }
        log.info("STOP - SETUP:DeleteTenant:elastic indexes={}, time={}ms", tenantKey, stopWatch.getTime());
    }
}



