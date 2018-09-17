package com.icthh.xm.ms.entity.service;

import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;
import static com.google.common.collect.ImmutableMap.of;
import static com.icthh.xm.commons.config.client.repository.TenantConfigRepository.URL;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.codec.digest.DigestUtils.sha1Hex;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.config.client.repository.TenantConfigRepository;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EntityCustomPrivilegeService {

    private final TenantConfigRepository tenantConfigRepository;
    private final TenantContextHolder tenantContextHolder;
    private static final String CUSTOMER_PRIVILEGES_PATH = URL + "custom-privileges.yml";

    private ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    @SneakyThrows
    public void updateApplicationPermission(Map<String, TypeSpec> specs) {
        String tenantKey = tenantContextHolder.getContext().getTenantKey().get().getValue().toUpperCase();
        log.info("Get config from {} to tenant {}", CUSTOMER_PRIVILEGES_PATH, tenantKey);
        String customPrivileges = tenantConfigRepository.getConfigFullPath(tenantKey, CUSTOMER_PRIVILEGES_PATH);

        Map<String, List<Map<String, Object>>> privileges = mapper.readValue(customPrivileges, new TypeReference<Map<String, List<Object>>>() {});
        List<Map<String, Object>> applicationPrivileges = specs.values().stream().filter(TypeSpec::getIsApp).map(this::toPrivilege).collect(toList());
        privileges.put("applications", applicationPrivileges);

        String content = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(privileges);
        tenantConfigRepository.updateConfigFullPath(tenantKey, CUSTOMER_PRIVILEGES_PATH, content, sha1Hex(customPrivileges));
    }

    private Map<String, Object> toPrivilege(TypeSpec spec) {
        return of("key", "APPLICATION." + spec.getKey(), "description", "{}");
    }

}
