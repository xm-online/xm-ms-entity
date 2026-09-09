package com.icthh.xm.ms.entity.service.search.db.template;

import static com.icthh.xm.commons.exceptions.ErrorConstants.ERR_VALIDATION;

import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.exceptions.EntityNotFoundException;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.tenant.YamlMapperUtils;
import com.icthh.xm.ms.entity.config.ApplicationProperties;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/** Tenant config: {@code entity/jpql-templates.yml} plus {@code entity/jpql-templates/*.yml}, merged per tenant. */
@Slf4j
@Service
@RequiredArgsConstructor
public class XmEntityJpqlTemplatesService implements RefreshableConfiguration {

    private static final String TENANT_NAME = "tenantName";
    private static final ObjectMapper MAPPER = YamlMapperUtils.yamlDefaultMapper();

    private final AntPathMatcher matcher = new AntPathMatcher();
    /** tenant → config path → templates in that file. */
    private final Map<String, Map<String, Map<String, JpqlTemplate>>> templatesByTenant = new ConcurrentHashMap<>();
    private final ApplicationProperties applicationProperties;
    private final TenantContextHolder tenantContextHolder;

    public JpqlTemplate getTemplate(String key) {
        String tenant = TenantContextUtils.getRequiredTenantKeyValue(tenantContextHolder);
        Map<String, JpqlTemplate> merged = new LinkedHashMap<>();
        templatesByTenant.getOrDefault(tenant, Map.of()).values().forEach(file -> file.forEach((k, t) -> {
            if (merged.put(k, t) != null) {
                log.warn("JPQL template {} defined more than once for tenant {}, last file wins", k, tenant);
            }
        }));
        JpqlTemplate template = merged.get(key);
        if (template == null || StringUtils.isBlank(template.getQuery())) {
            throw new EntityNotFoundException("JPQL template not found: " + key);
        }
        return template;
    }

    public Map<String, Object> coerce(JpqlTemplate template, Map<String, ?> rawParams) {
        Map<String, Object> result = new HashMap<>();
        Map<String, String> types = template.getParams() == null ? Map.of() : template.getParams();
        rawParams.forEach((name, value) -> result.put(name, coerceValue(name, types.get(name), value)));
        return result;
    }

    private static Object coerceValue(String name, String type, Object value) {
        if (type == null || !(value instanceof String s)) {
            return value;
        }
        try {
            return switch (type) {
                case "number" -> s.contains(".") ? (Object) Double.valueOf(s) : (Object) Long.valueOf(s);
                case "boolean" -> Boolean.valueOf(s);
                case "instant" -> Instant.parse(s);
                case "list" -> Arrays.stream(s.split(",")).map(String::trim).toList();
                default -> s;
            };
        } catch (NumberFormatException | DateTimeParseException e) {
            throw new BusinessException(ERR_VALIDATION, "Invalid value for template param " + name + ": " + value);
        }
    }

    @Override
    public void onRefresh(String updatedKey, String config) {
        try {
            String tenant = matcher.extractUriTemplateVariables(patternFor(updatedKey), updatedKey).get(TENANT_NAME);
            Map<String, Map<String, JpqlTemplate>> files = templatesByTenant.computeIfAbsent(tenant, t -> new ConcurrentHashMap<>());
            if (StringUtils.isBlank(config)) {
                files.remove(updatedKey);
                log.info("JPQL templates {} removed for tenant {}", updatedKey, tenant);
            } else {
                Map<String, JpqlTemplate> parsed = MAPPER.readValue(config, new TypeReference<Map<String, JpqlTemplate>>() {});
                parsed.forEach((k, t) -> t.setKey(k));
                files.put(updatedKey, parsed);
                log.info("JPQL templates {} updated for tenant {}: {}", updatedKey, tenant, parsed.keySet());
            }
        } catch (Exception e) {
            log.error("Error reading JPQL templates from {}", updatedKey, e);
        }
    }

    @Override
    public boolean isListeningConfiguration(String updatedKey) {
        return matcher.match(applicationProperties.getJpqlTemplatesPathPattern(), updatedKey)
            || matcher.match(applicationProperties.getJpqlTemplatesFolderPathPattern(), updatedKey);
    }

    @Override
    public void onInit(String key, String config) {
        if (isListeningConfiguration(key)) {
            onRefresh(key, config);
        }
    }

    private String patternFor(String key) {
        return matcher.match(applicationProperties.getJpqlTemplatesPathPattern(), key)
            ? applicationProperties.getJpqlTemplatesPathPattern()
            : applicationProperties.getJpqlTemplatesFolderPathPattern();
    }
}
