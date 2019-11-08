package com.icthh.xm.ms.entity.service.metrics;

import com.icthh.xm.commons.tenant.TenantContextHolder;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomMetricsContext {

    private final Map<String, Map<String, Object>> metricsContext = new ConcurrentHashMap<>();
    private final TenantContextHolder tenantContextHolder;

    public Map<String, Object> getMetricsContext() {
        String tenantKey = tenantContextHolder.getTenantKey();
        return metricsContext.computeIfAbsent(tenantKey, (key) -> new ConcurrentHashMap<>());
    }

}
