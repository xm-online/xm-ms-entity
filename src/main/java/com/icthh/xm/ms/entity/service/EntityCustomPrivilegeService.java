package com.icthh.xm.ms.entity.service;

import static com.google.common.collect.ImmutableMap.of;
import static com.icthh.xm.commons.config.client.repository.TenantConfigRepository.URL;
import static java.util.stream.Collectors.toList;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.config.client.repository.TenantConfigRepository;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class EntityCustomPrivilegeService {

    private final TenantConfigRepository tenantConfigRepository;
    private final TenantContextHolder tenantContextHolder;
    private static final String CUSTOMER_PRIVILEGES_PATH = URL + "custom-privileges.yml";

    private ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    @SneakyThrows
    public void updateApplicationPermission(Map<String, TypeSpec> specs) {
        Set<String> typeKeys = specs.keySet();
        String tenantKey = tenantContextHolder.getContext().getTenantKey().get().getValue().toUpperCase();
        String customPrivileges = tenantConfigRepository.getConfigFullPath(tenantKey, CUSTOMER_PRIVILEGES_PATH);

        Map<String, List<Map<String, Object>>> privileges = mapper.readValue(customPrivileges, new TypeReference<Map<String, List<Object>>>() {});
        List<Map<String, Object>> applicationPrivileges = typeKeys.stream().map(this::toPrivilege).collect(toList());
        privileges.put("applications", applicationPrivileges);

        tenantConfigRepository.updateConfigFullPath(tenantKey, CUSTOMER_PRIVILEGES_PATH, mapper.writeValueAsString(privileges));
    }

    private Map<String, Object> toPrivilege(String it) {
        return of("key", "APPLICATION." + it, "description", "{}");
    }

}
