package com.icthh.xm.ms.entity.service.metrics;

import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.api.LepManagementService;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.commons.logging.util.MdcUtils;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.ms.entity.service.metrics.CustomMetricsConfiguration.CustomMetric;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import static com.icthh.xm.commons.tenant.TenantContextUtils.buildTenant;
import static java.util.Collections.emptyMap;

@Slf4j
@Component
@LepService(group = "metrics")
@RequiredArgsConstructor
public class CustomMetricsService {

    private final Map<String, Map<String, Object>> metricsCache = new ConcurrentHashMap<>();
    @Setter(onMethod = @__(@Autowired))
    private CustomMetricsService self;

    private final TenantContextHolder tenantContextHolder;
    private final LepManagementService lepManagementService;

    public Object getMetric(String name, CustomMetric config, String tenantKey) {
        return runInTenantContext(tenantKey, () -> {
            if (config.getUpdatePeriodSeconds() == null) {
                return self.metricByName(name);
            }
            return metricsCache.getOrDefault(tenantKey, emptyMap()).get(name);
        });
    }

    public void updateMetric(String metricName, String tenant) {
        try {
            MdcUtils.putRid(MdcUtils.generateRid() + ":" + tenant);
            Object metricValue = runInTenantContext(tenant, () -> self.metricByName(metricName));
            Map<String, Object> metrics = metricsCache.computeIfAbsent(tenant, (key) -> new ConcurrentHashMap<>());
            metrics.put(metricName, metricValue);
        } catch (Throwable e) {
            log.error("Error update metric", e);
        } finally {
            MdcUtils.clear();
        }
    }

    private Object runInTenantContext(String tenant, Supplier<Object> operation) {
        return tenantContextHolder.getPrivilegedContext().execute(buildTenant(tenant), () -> {
            try(var context = lepManagementService.beginThreadContext()) {
                return operation.get();
            }
        });
    }

    @LogicExtensionPoint(value = "Metric", resolver = MetricKeyResolver.class)
    public Object metricByName(String metricName) {
        return self.metric(metricName);
    }

    @LogicExtensionPoint(value = "Metric")
    public Object metric(String metricName) {
        log.warn("No lep for metric {} found", metricName);
        return null;
    }

}
