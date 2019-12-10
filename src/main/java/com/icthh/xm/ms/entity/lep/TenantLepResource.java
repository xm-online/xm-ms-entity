package com.icthh.xm.ms.entity.lep;

import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantKey;
import com.icthh.xm.ms.entity.config.ApplicationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class TenantLepResource implements RefreshableConfiguration {

    private static final String TENANT_NAME = "tenantName";

    private final AntPathMatcher matcher = new AntPathMatcher();
    private final ApplicationProperties applicationProperties;
    private final TenantContextHolder tenantContextHolder;

    private Map<String, Map<String, String>> lepResources = new ConcurrentHashMap<>();

    @Override
    public void onRefresh(String updatedKey, String config) {
        String lepResourcePathPattern = applicationProperties.getLep().getLepResourcePathPattern();
        String tenantKeyValue = matcher.extractUriTemplateVariables(lepResourcePathPattern, updatedKey).get(TENANT_NAME);
        lepResources.putIfAbsent(tenantKeyValue, new ConcurrentHashMap<>());
        lepResources.get(tenantKeyValue).put("/" + matcher.extractPathWithinPattern(lepResourcePathPattern, updatedKey), config);
    }

    public String getResource(String path) {
        TenantKey tenantKey = tenantContextHolder.getContext().getTenantKey().orElse(null);
        if (tenantKey == null || !lepResources.containsKey(tenantKey.getValue())) {
            return null;
        }
        return lepResources.get(tenantKey.getValue()).get(path);
    }

    @Override
    public boolean isListeningConfiguration(String updatedKey) {
        String lepResourcePathPattern = applicationProperties.getLep().getLepResourcePathPattern();
        return matcher.match(lepResourcePathPattern, updatedKey);
    }

    @Override
    public void onInit(String key, String config) {
        if (isListeningConfiguration(key)) {
            onRefresh(key, config);
        }
    }
}
