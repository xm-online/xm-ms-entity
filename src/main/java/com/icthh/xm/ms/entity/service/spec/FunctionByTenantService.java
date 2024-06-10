package com.icthh.xm.ms.entity.service.spec;

import static com.icthh.xm.ms.entity.util.CustomCollectionUtils.nullSafe;

import com.icthh.xm.ms.entity.domain.spec.FunctionSpec;
import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.AntPathMatcher;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
public class FunctionByTenantService {

    private final ConcurrentHashMap<String, Map<String, FunctionSpec>> functionsByTenant = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, List<FunctionSpec>> orderedFunctionsByTenant = new ConcurrentHashMap<>();

    public void processFunctionSpec(String tenantKey, LinkedHashMap<String, TypeSpec> tenantEntitySpec) {
        var functionSpec = tenantEntitySpec.values().stream()
            .map(TypeSpec::getFunctions)
            .filter(Objects::nonNull)
            .flatMap(Collection::stream)
            .collect(Collectors.toMap(FunctionSpec::getKey, fs -> fs, (t, t2) -> t));
        if (functionSpec.isEmpty()) {
            functionsByTenant.remove(tenantKey);
            orderedFunctionsByTenant.remove(tenantKey);
        }
        functionsByTenant.put(tenantKey, functionSpec);
        orderedFunctionsByTenant.put(tenantKey, functionSpec.values()
                                                            .stream()
                                                            .sorted(new PatternComparator())
                                                            .collect(Collectors.toList()));
        log.info("functionSpec.size={}", functionSpec.size());
    }

    public Map<String, FunctionSpec> functionsByTenant(String tenantKey) {
        return nullSafe(functionsByTenant.get(tenantKey));
    }

    public List<FunctionSpec> functionsByTenantOrdered(String tenantKey) {
        return nullSafe(orderedFunctionsByTenant.get(tenantKey));
    }

    public static class PatternComparator implements Comparator<FunctionSpec> {
        AntPathMatcher matcher = new AntPathMatcher();
        @Override
        public int compare(final FunctionSpec fs1, final FunctionSpec fs2) {
            String path1 = fs1.getPath();
            String path2 = fs2.getPath();
            if (path1 != null && path2 == null) {
                return 1;
            } else if (path1 == null && path2 != null) {
                return -1;
            } else if (path1 == null) {
                return 0;
            } else if (matcher.match(path1, path2)) {
                return 1;
            } else if (matcher.match(path2, path1)) {
                return -1;
            } else {
                return path1.compareTo(path2);
            }
        }
    }

}
