package com.icthh.xm.ms.entity.service.spec;

import com.icthh.xm.ms.entity.domain.spec.FunctionSpec;
import com.icthh.xm.ms.entity.domain.spec.TypeSpec;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.icthh.xm.ms.entity.util.CustomCollectionUtils.nullSafe;

public class FunctionByTenantService {

    private final ConcurrentHashMap<String, Map<String, FunctionSpec>> functionsByTenant = new ConcurrentHashMap<>();

    public void processFunctionSpec(String tenantKey, LinkedHashMap<String, TypeSpec> tenantEntitySpec) {
        var functionSpec = tenantEntitySpec.values().stream()
            .map(TypeSpec::getFunctions)
            .filter(Objects::nonNull)
            .flatMap(Collection::stream)
            .collect(Collectors.toMap(FunctionSpec::getKey, fs -> fs, (t, t2) -> t));
        if (functionSpec.isEmpty()) {
            functionsByTenant.remove(tenantKey);
        }
        functionsByTenant.put(tenantKey, functionSpec);
    }

    public Map<String, FunctionSpec> functionsByTenant(String tenantKey) {
        return nullSafe(functionsByTenant.get(tenantKey));
    }

}
