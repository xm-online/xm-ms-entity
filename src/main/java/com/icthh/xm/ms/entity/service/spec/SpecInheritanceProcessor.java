package com.icthh.xm.ms.entity.service.spec;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.icthh.xm.ms.entity.config.XmEntityTenantConfigService;
import com.icthh.xm.ms.entity.domain.ext.TypeSpecParameter;
import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.icthh.xm.ms.entity.domain.ext.TypeSpecParameter.ACCESS;
import static com.icthh.xm.ms.entity.domain.ext.TypeSpecParameter.ATTACHMENTS;
import static com.icthh.xm.ms.entity.domain.ext.TypeSpecParameter.CALENDARS;
import static com.icthh.xm.ms.entity.domain.ext.TypeSpecParameter.FUNCTIONS;
import static com.icthh.xm.ms.entity.domain.ext.TypeSpecParameter.LINKS;
import static com.icthh.xm.ms.entity.domain.ext.TypeSpecParameter.LOCATIONS;
import static com.icthh.xm.ms.entity.domain.ext.TypeSpecParameter.RATINGS;
import static com.icthh.xm.ms.entity.domain.ext.TypeSpecParameter.STATES;
import static com.icthh.xm.ms.entity.domain.ext.TypeSpecParameter.TAGS;
import static com.icthh.xm.ms.entity.service.spec.DataSpecJsonSchemaService.DEFINITION_PREFIXES;
import static com.icthh.xm.ms.entity.util.CustomCollectionUtils.union;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.apache.commons.lang3.ObjectUtils.firstNonNull;

@Slf4j
public class SpecInheritanceProcessor {

    private static final String TYPE_SEPARATOR_REGEXP = "\\.";
    private static final String TYPE_SEPARATOR = ".";
    public static final String XM_ENTITY_INHERITANCE_DEFINITION = "xmEntityInheritanceDefinition";

    private final XmEntityTenantConfigService tenantConfigService;

    public SpecInheritanceProcessor(XmEntityTenantConfigService tenantConfigService) {
        this.tenantConfigService = tenantConfigService;
    }

    /**
     * XM Entity Type specifications support inheritance based on key hierarchy
     * with data extension.
     */
    public void process(Map<String, TypeSpec> types, String tenant) {
        List<String> keys = types.keySet().stream()
            .sorted(Comparator.comparingInt(k -> k.split(TYPE_SEPARATOR_REGEXP).length))
            .collect(Collectors.toList());
        for (String key : keys) {
            int level = key.split(TYPE_SEPARATOR_REGEXP).length;
            if (level > 1) {
                TypeSpec type = types.get(key);
                TypeSpec parentType = types.get(StringUtils.substringBeforeLast(key, TYPE_SEPARATOR));
                if (parentType != null) {
                    types.put(key, extend(type, parentType, tenant));
                }
            }
        }
        log.info("processed {} types", CollectionUtils.size(types));
    }

    /**
     * Xm Entity Type specifications data extention from parent Type.
     *
     * @param type       entity Type specification for extention
     * @param parentType parent entity Type specification for cloning
     * @return entity Type specification that extended from parent Type
     */
    private TypeSpec extend(TypeSpec type, TypeSpec parentType, String tenant) {
        type.setIcon(type.getIcon() != null ? type.getIcon() : parentType.getIcon());
        extendDataSpec(type, parentType, tenant);
        extendDataForm(type, parentType, tenant);
        type.setAccess(ignorableUnion(ACCESS, type, parentType));
        type.setAttachments(ignorableUnion(ATTACHMENTS, type, parentType));
        type.setCalendars(ignorableUnion(CALENDARS, type, parentType));
        type.setFunctions(ignorableUnion(FUNCTIONS, type, parentType));
        type.setLinks(ignorableUnion(LINKS, type, parentType));
        type.setLocations(ignorableUnion(LOCATIONS, type, parentType));
        type.setRatings(ignorableUnion(RATINGS, type, parentType));
        type.setStates(ignorableUnion(STATES, type, parentType));
        type.setTags(ignorableUnion(TAGS, type, parentType));
        return type;
    }

    @SneakyThrows
    private void extendDataSpec(TypeSpec type, TypeSpec parentType, String tenant) {
        XmEntityTenantConfigService.XmEntityTenantConfig entityTenantConfig = this.tenantConfigService.getXmEntityTenantConfig(tenant);
        Boolean isInheritanceEnabled = entityTenantConfig.getEntitySpec().getEnableDataSpecInheritance();
        if (isFeatureEnabled(isInheritanceEnabled, type.getDataSpecInheritance()) && hasDataSpec(type, parentType)) {
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> target = objectMapper.readValue(nullSafeReadJson(type.getDataSpec()), Map.class);
            Map<String, Object> parent = objectMapper.readValue(nullSafeReadJson(parentType.getDataSpec()), Map.class);
            if (parent.containsKey("additionalProperties")) {
                parent.put("additionalProperties", true);
            }

            DEFINITION_PREFIXES.forEach(prefix -> target.put(prefix, makeMapMutable(prefix, target)));
            DEFINITION_PREFIXES.forEach(prefix -> {
                Map<String, Object> defs = (Map<String, Object>) target.get(prefix);
                Map<String, Object> parentDefs = (Map<String, Object>) parent.remove(prefix);
                defs.putAll(firstNonNull(parentDefs, Map.of()));
            });

            Map<String, Object> inheritanceDefinitions = (Map<String, Object>) target.get(XM_ENTITY_INHERITANCE_DEFINITION);
            inheritanceDefinitions.put(parentType.getKey(), parent);
            target.put("$ref", "#/" + XM_ENTITY_INHERITANCE_DEFINITION + "/" + parentType.getKey());
            String mergedJson = objectMapper.writeValueAsString(target);
            type.setDataSpec(mergedJson);
        } else {
            type.setDataSpec(type.getDataSpec() != null ? type.getDataSpec() : parentType.getDataSpec());
        }
    }

    @NotNull
    private Map<String, Object> makeMapMutable(String prefix, Map<String, Object> target) {
        return new HashMap<>((Map<String, Object>)target.getOrDefault(prefix, new HashMap<>()));
    }

    private static String nullSafeReadJson(String dataSpec) {
        return dataSpec != null ? dataSpec : "{}";
    }

    @SneakyThrows
    private void extendDataForm(TypeSpec type, TypeSpec parentType, String tenant) {
        XmEntityTenantConfigService.XmEntityTenantConfig entityTenantConfig = this.tenantConfigService.getXmEntityTenantConfig(tenant);
        Boolean isInheritanceEnabled = entityTenantConfig.getEntitySpec().getEnableDataFromInheritance();
        if (isFeatureEnabled(isInheritanceEnabled, type.getDataFormInheritance()) && hasDataForm(type, parentType)) {
            ObjectMapper objectMapper = new ObjectMapper();
            var defaults = objectMapper.readValue(nullSafeReadJson(parentType.getDataForm()), Map.class);
            ObjectReader updater = objectMapper.readerForUpdating(defaults);
            Map<String, Object> merged = updater.readValue(nullSafeReadJson(type.getDataForm()));
            String mergedJson = objectMapper.writeValueAsString(merged);
            type.setDataForm(mergedJson);
        } else {
            type.setDataForm(type.getDataForm() != null ? type.getDataForm() : parentType.getDataForm());
        }
    }

    private boolean hasDataSpec(TypeSpec type, TypeSpec parentType) {
        return type.getDataSpec() != null && parentType.getDataSpec() != null;
    }

    private boolean isFeatureEnabled(Boolean tenantFlag, Boolean entityFlag) {
        return TRUE.equals(entityFlag) || (TRUE.equals(tenantFlag) && !FALSE.equals(entityFlag));
    }

    private boolean hasDataForm(TypeSpec type, TypeSpec parentType) {
        return type.getDataForm() != null && parentType.getDataForm() != null;
    }

    /**
     * Combine type specifications considering ignore list.
     *
     * @param typeSpecParam  represents enum for type specification
     * @param type       entity Type specification for extention
     * @param parentType parent entity Type specification for cloning
     * @return union list or list from child type in case inheritance prohibited
     */
    private static <T> List<T> ignorableUnion(TypeSpecParameter typeSpecParam, TypeSpec type, TypeSpec parentType) {
        List<T> parameters = (List<T>) typeSpecParam.getParameterResolver().apply(type);
        List<T> parentParameters = (List<T>) typeSpecParam.getParameterResolver().apply(parentType);
        return type.getIgnoreInheritanceFor().contains(typeSpecParam.getType()) ?
            parameters : union(parameters, parentParameters);
    }

}
