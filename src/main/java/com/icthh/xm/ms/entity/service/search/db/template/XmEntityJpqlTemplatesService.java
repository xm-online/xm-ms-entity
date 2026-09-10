package com.icthh.xm.ms.entity.service.search.db.template;

import static com.icthh.xm.commons.exceptions.ErrorConstants.ERR_VALIDATION;
import static com.icthh.xm.commons.tenant.TenantContextUtils.getRequiredTenantKeyValue;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.exceptions.EntityNotFoundException;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.YamlMapperUtils;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * Tenant config {@value #TEMPLATES_PATH_PATTERN} plus {@value #TEMPLATES_FOLDER_PATH_PATTERN}.
 * {@link #onRefresh} stores each file as is; {@link #refreshFinished} builds the lookup map
 * tenant → template key → template (later file paths win on a key clash).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class XmEntityJpqlTemplatesService implements RefreshableConfiguration {

    public static final String TEMPLATES_PATH_PATTERN = "/config/tenants/{tenantName}/entity/jpql-templates.yml";
    public static final String TEMPLATES_FOLDER_PATH_PATTERN = "/config/tenants/{tenantName}/entity/jpql-templates/*.yml";
    private static final String TENANT_NAME = "tenantName";
    private static final ObjectMapper MAPPER = YamlMapperUtils.yamlDefaultMapper();

    private final AntPathMatcher matcher = new AntPathMatcher();
    /** tenant → config path → templates of that file (raw, as configured). */
    private final Map<String, Map<String, Map<String, JpqlTemplate>>> filesByTenant = new ConcurrentHashMap<>();
    /** tenant → template key → template (built by refreshFinished). */
    private volatile Map<String, Map<String, JpqlTemplate>> templatesByTenant = Map.of();
    private final TenantContextHolder tenantContextHolder;

    public JpqlTemplate getTemplate(String templateKey) {
        String tenant = getRequiredTenantKeyValue(tenantContextHolder);
        JpqlTemplate template = templatesByTenant.getOrDefault(tenant, Map.of()).get(templateKey);
        if (template == null) {
            throw new EntityNotFoundException("JPQL template not found: " + templateKey);
        }
        return template;
    }

    /** Converts String values (GET query params) to the types declared in {@link JpqlTemplate#getParams()}. */
    public Map<String, Object> applyParamTypes(JpqlTemplate template, Map<String, ?> params) {
        Map<String, String> types = template.getParams() == null ? Map.of() : template.getParams();
        Map<String, Object> result = new HashMap<>();
        params.forEach((name, value) -> result.put(name, convert(name, types.get(name), value)));
        return result;
    }

    private static Object convert(String name, String type, Object value) {
        if (type == null || !(value instanceof String text)) {
            return value;
        }
        try {
            return switch (type) {
                case "number" -> text.contains(".") ? (Object) Double.valueOf(text) : (Object) Long.valueOf(text);
                case "boolean" -> Boolean.valueOf(text);
                case "instant" -> Instant.parse(text);
                case "list" -> Arrays.stream(text.split(",")).map(String::trim).toList();
                default -> text;
            };
        } catch (NumberFormatException | DateTimeParseException e) {
            throw new BusinessException(ERR_VALIDATION, "Invalid value for template param " + name + ": " + value);
        }
    }

    @Override
    public void onRefresh(String updatedKey, String config) {
        try {
            String tenant = matcher.extractUriTemplateVariables(patternFor(updatedKey), updatedKey).get(TENANT_NAME);
            Map<String, Map<String, JpqlTemplate>> files = filesByTenant.computeIfAbsent(tenant, t -> new ConcurrentHashMap<>());
            if (isBlank(config)) {
                files.remove(updatedKey);
                log.info("JPQL templates {} removed for tenant {}", updatedKey, tenant);
            } else {
                files.put(updatedKey, MAPPER.readValue(config, new TypeReference<Map<String, JpqlTemplate>>() {}));
                log.info("JPQL templates {} updated for tenant {}", updatedKey, tenant);
            }
        } catch (Exception e) {
            log.error("Error reading JPQL templates from {}", updatedKey, e);
        }
    }

    @Override
    public void refreshFinished(Collection<String> paths) {
        Map<String, Map<String, JpqlTemplate>> result = new HashMap<>();
        filesByTenant.forEach((tenant, files) -> result.put(tenant, mergeFiles(tenant, files)));
        templatesByTenant = result;
    }

    @Override
    public void refreshableConfigurationInited() {
        refreshFinished(filesByTenant.keySet());
    }

    private static Map<String, JpqlTemplate> mergeFiles(String tenant, Map<String, Map<String, JpqlTemplate>> files) {
        Map<String, JpqlTemplate> merged = new HashMap<>();
        new TreeMap<>(files).forEach((path, templates) -> templates.forEach((key, template) -> {
            template.setKey(key);
            if (isBlank(template.getQuery())) {
                log.error("JPQL template {} in {} has no query and is ignored (tenant {})", key, path, tenant);
            } else if (merged.put(key, template) != null) {
                log.warn("JPQL template {} defined more than once for tenant {}, {} wins", key, tenant, path);
            }
        }));
        return merged;
    }

    @Override
    public boolean isListeningConfiguration(String updatedKey) {
        return matcher.match(TEMPLATES_PATH_PATTERN, updatedKey) || matcher.match(TEMPLATES_FOLDER_PATH_PATTERN, updatedKey);
    }

    @Override
    public void onInit(String key, String config) {
        if (isListeningConfiguration(key)) {
            onRefresh(key, config);
        }
    }

    private String patternFor(String key) {
        return matcher.match(TEMPLATES_PATH_PATTERN, key) ? TEMPLATES_PATH_PATTERN : TEMPLATES_FOLDER_PATH_PATTERN;
    }
}
