package com.icthh.xm.ms.entity.service.json;

import com.icthh.xm.commons.logging.aop.IgnoreLogginAspect;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Slf4j
@Service
@IgnoreLogginAspect
public class JsonListenerService {

    private final Map<String, Map<String, String>> tenantsSpecificationsByPath = new LinkedHashMap<>();

    public void processTenantSpecification(String tenantName, String relativePath, String config) {
        if (isBlank(config)) {
            tenantsSpecificationsByPath.remove(tenantName);
            return;
        }

        tenantsSpecificationsByPath.putIfAbsent(tenantName, new ConcurrentHashMap<>());
        tenantsSpecificationsByPath.get(tenantName).put(relativePath, config);
    }

    public String getSpecificationByTenantRelativePath(String tenant, String relativePath) {
        return ofNullable(getSpecificationByTenant(tenant))
            .map(xm -> xm.get(relativePath))
            .orElse(EMPTY);
    }

    public Map<String, String> getSpecificationByTenant(String tenant) {
        return tenantsSpecificationsByPath.get(tenant);
    }
}
