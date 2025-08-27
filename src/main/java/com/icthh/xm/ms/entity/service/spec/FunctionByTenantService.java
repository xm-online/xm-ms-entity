package com.icthh.xm.ms.entity.service.spec;

import static com.icthh.xm.ms.entity.util.CustomCollectionUtils.nullSafe;
import static java.util.Objects.nonNull;

import com.icthh.xm.ms.entity.domain.spec.FunctionSpec;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.AntPathMatcher;

@Slf4j
public class FunctionByTenantService {

    private final ConcurrentHashMap<String, Map<String, FunctionSpec>> functionsByTenant = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<FunctionSpec>> orderedFunctionsByTenant = new ConcurrentHashMap<>();
    private final Map<String, List<FunctionMetaInfo>> functionsByTenantByFile = new ConcurrentHashMap<>();

    public void processFunctionSpec(String tenantKey, Map<String, XmEntitySpecification> entitySpecs) {
        Map<String, FunctionSpec> functionByKey = new HashMap<>();
        List<FunctionMetaInfo> functionMetaInfos = new ArrayList<>();

        entitySpecs.values().stream().filter(it -> nonNull(it.types()))
            .flatMap(spec -> spec.types().values().stream())
            .filter(it -> nonNull(it.getFunctions())).forEach(type ->
                type.getFunctions().forEach(fs -> {
                    functionByKey.put(fs.getKey(), fs);
                    functionMetaInfos.add(new FunctionMetaInfo(type.getKey(), fs.getKey()));
                })
            );

        if (functionByKey.isEmpty()) {
            functionsByTenant.remove(tenantKey);
            orderedFunctionsByTenant.remove(tenantKey);
            functionsByTenantByFile.remove(tenantKey);
        }
        functionsByTenantByFile.put(tenantKey, functionMetaInfos);
        functionsByTenant.put(tenantKey, functionByKey);
        orderedFunctionsByTenant.put(tenantKey, functionByKey.values()
                                                            .stream()
                                                            .sorted(new PatternComparator())
                                                            .collect(Collectors.toList()));
        log.info("functionByKey.size={}", functionByKey.size());
    }

    public List<FunctionMetaInfo> functionsMetaInfoByTenant(String tenantKey) {
        return nullSafe(functionsByTenantByFile.get(tenantKey));
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
