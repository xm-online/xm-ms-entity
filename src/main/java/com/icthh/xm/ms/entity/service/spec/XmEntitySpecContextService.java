package com.icthh.xm.ms.entity.service.spec;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.google.common.collect.Sets;
import com.icthh.xm.ms.entity.config.XmEntityTenantConfigService;
import com.icthh.xm.ms.entity.domain.ext.TypeSpecParameter;
import com.icthh.xm.ms.entity.domain.spec.FunctionSpec;
import com.icthh.xm.ms.entity.domain.spec.StateSpec;
import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import com.icthh.xm.ms.entity.domain.spec.UniqueFieldSpec;
import com.icthh.xm.ms.entity.domain.spec.XmEntitySpec;
import com.icthh.xm.ms.entity.security.SecurityUtils;
import com.icthh.xm.ms.entity.security.access.DynamicPermissionCheckService;
import com.icthh.xm.ms.entity.service.processor.DefinitionSpecProcessor;
import com.icthh.xm.ms.entity.service.processor.FormSpecProcessor;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.github.fge.jackson.NodeType.OBJECT;
import static com.github.fge.jackson.NodeType.getNodeType;
import static com.icthh.xm.ms.entity.domain.ext.TypeSpecParameter.ACCESS;
import static com.icthh.xm.ms.entity.domain.ext.TypeSpecParameter.ATTACHMENTS;
import static com.icthh.xm.ms.entity.domain.ext.TypeSpecParameter.CALENDARS;
import static com.icthh.xm.ms.entity.domain.ext.TypeSpecParameter.FUNCTIONS;
import static com.icthh.xm.ms.entity.domain.ext.TypeSpecParameter.LINKS;
import static com.icthh.xm.ms.entity.domain.ext.TypeSpecParameter.LOCATIONS;
import static com.icthh.xm.ms.entity.domain.ext.TypeSpecParameter.RATINGS;
import static com.icthh.xm.ms.entity.domain.ext.TypeSpecParameter.STATES;
import static com.icthh.xm.ms.entity.domain.ext.TypeSpecParameter.TAGS;
import static com.icthh.xm.ms.entity.util.CustomCollectionUtils.nullSafe;
import static com.icthh.xm.ms.entity.util.CustomCollectionUtils.union;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.springframework.util.CollectionUtils.isEmpty;

@Slf4j
@Service
@RequiredArgsConstructor
public class XmEntitySpecContextService {

    private static final String TYPE_SEPARATOR = ".";
    private static final String TYPE_SEPARATOR_REGEXP = "\\.";
    private static final String XM_ENTITY_INHERITANCE_DEFINITION = "xmEntityInheritanceDefinition";
    private final DefinitionSpecProcessor definitionSpecProcessor;
    private final FormSpecProcessor formSpecProcessor;
    private final XmEntityTenantConfigService tenantConfigService;
    private final DynamicPermissionCheckService dynamicPermissionCheckService;

    private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    private final ConcurrentHashMap<String, Map<String, TypeSpec>> typesByTenant = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Map<String, String>> typesByTenantByFile = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Map<String, com.github.fge.jsonschema.main.JsonSchema>> dataSpecJsonSchemas = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Map<String, FunctionSpec>> functionsByTenant = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, List<TypeSpec>> typesByTenantRole = new ConcurrentHashMap<>();

    public void refreshTenantConfig(String updatedKey, String config, String tenantKey) {
        updateByFileState(updatedKey, config, tenantKey);
        updateByTenantState(tenantKey);
        log.info("Specification was for tenant {} updated from file {}", tenantKey, updatedKey);
    }

    public List<TypeSpec> getDynamicTypeSpec(String tenantKey) {

        String currentUserRole = SecurityUtils.getCurrentUserRole().orElse(null);
        if (StringUtils.isEmpty(currentUserRole)) {
            return List.of();
        }

        final String mapKey = tenantKey + "." + currentUserRole;
        Function<String, List<TypeSpec>> function = filterDynamicSpec(tenantKey);
        return typesByTenantRole.computeIfAbsent(mapKey, function);
    }

    public Map<String, TypeSpec> getTypesByTenant(String tenantKey, boolean strict) {
        if (strict && !typesByTenant.containsKey(tenantKey)) {
            log.error("Tenant configuration {} not found", tenantKey);
            throw new IllegalArgumentException("Tenant configuration not found");
        }
        return nullSafe(typesByTenant.get(tenantKey));
    }

    public Map<String, JsonSchema> getDataSpecJsonSchemas(String tenantKey) {
        return dataSpecJsonSchemas.get(tenantKey);
    }

    public Map<String, FunctionSpec> functionsByTenant(String tenantKey) {
        return nullSafe(functionsByTenant.get(tenantKey));
    }

    @NotNull
    private Function<String, List<TypeSpec>> filterDynamicSpec(String tenantKey) {
        return (key) -> getTypesByTenant(tenantKey, false)
            .values()
            .stream()
            .map(dynamicPermissionCheckService::evaluateDynamicPermissions)
            .collect(Collectors.toList());
    }

    @SneakyThrows
    private void updateByFileState(String updatedKey, String config, String tenant) {
        var byFiles = typesByTenantByFile.computeIfAbsent(tenant, key -> new LinkedHashMap<>());
        if (StringUtils.isBlank(config)) {
            byFiles.remove(updatedKey);
            return;
        }

        byFiles.put(updatedKey, config);
    }

    private void updateByTenantState(String tenant) {
        var tenantEntitySpec = new LinkedHashMap<String, TypeSpec>();
        typesByTenantByFile.get(tenant).values().stream().map(this::toTypeSpecsMap).forEach(tenantEntitySpec::putAll);
        definitionSpecProcessor.updateDefinitionStateByTenant(tenant, typesByTenantByFile);
        formSpecProcessor.updateFormStateByTenant(tenant, typesByTenantByFile);

        if (tenantEntitySpec.isEmpty()) {
            typesByTenant.remove(tenant);
        }

        typesByTenant.put(tenant, tenantEntitySpec);
        inheritance(tenantEntitySpec, tenant);
        processUniqueFields(tenantEntitySpec);

        var functionSpec = tenantEntitySpec.values().stream()
            .map(TypeSpec::getFunctions)
            .filter(Objects::nonNull)
            .flatMap(Collection::stream)
            .collect(Collectors.toMap(FunctionSpec::getKey, fs -> fs, (t, t2) -> t));
        if (functionSpec.isEmpty()) {
            functionsByTenant.remove(tenant);
        }
        functionsByTenant.put(tenant, functionSpec);

        var dataSchemas = new HashMap<String, JsonSchema>();
        JsonSchemaFactory jsonSchemaFactory = JsonSchemaFactory.byDefault();
        for (TypeSpec typeSpec : tenantEntitySpec.values()) {
            processTypeSpec(tenant, typeSpec);
            addJsonSchema(dataSchemas, jsonSchemaFactory, typeSpec);
        }

        dataSpecJsonSchemas.put(tenant, dataSchemas);

        List<String> keys = typesByTenantRole.keySet().stream()
            .filter(item -> StringUtils.startsWith(item, tenant + ".")).collect(Collectors.toList());
        keys.forEach(typesByTenantRole::remove);

    }

    /**
     * XM Entity Type specifications support inheritance based on key hierarchy
     * with data extension.
     */
    private void inheritance(Map<String, TypeSpec> types, String tenant) {
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
    }

    @SneakyThrows
    private void processUniqueFields(Map<String, TypeSpec> types) {
        for (TypeSpec typeSpec: types.values()) {
            if (StringUtils.isBlank(typeSpec.getDataSpec())) {
                continue;
            }

            JsonNode node = JsonLoader.fromString(typeSpec.getDataSpec());
            Set<UniqueFieldSpec> uniqueFields = Sets.newHashSet();
            processNode(node, "$", uniqueFields);
            typeSpec.setUniqueFields(uniqueFields);
        }
    }

    private void processNode(JsonNode node, String jsonPath, Set<UniqueFieldSpec> uniqueFields) {
        if (node.has("unique") && node.get("unique").asBoolean()) {
            uniqueFields.add(new UniqueFieldSpec(jsonPath));
        }

        if (!isObject(node)) {
            return;
        }

        JsonNode properties = node.get("properties");
        properties.fieldNames().forEachRemaining(name -> processNode(properties.get(name), jsonPath + "." + name, uniqueFields));
    }

    private boolean isObject(JsonNode schemaNode) {
        return getNodeType(schemaNode).equals(OBJECT) && schemaNode.has("properties");
    }

    private void processTypeSpec(String tenant, TypeSpec typeSpec) {
        definitionSpecProcessor.processTypeSpec(tenant, typeSpec::setDataSpec, typeSpec::getDataSpec);
        formSpecProcessor.processTypeSpec(tenant, typeSpec::setDataForm, typeSpec::getDataForm);

        Stream.ofNullable(typeSpec.getStates())
            .flatMap(Collection::stream)
            .map(StateSpec::getNext)
            .filter(Objects::nonNull)
            .flatMap(Collection::stream)
            .forEach(nextSpec -> {
                definitionSpecProcessor.processTypeSpec(tenant, nextSpec::setInputSpec, nextSpec::getInputSpec);
                formSpecProcessor.processTypeSpec(tenant,nextSpec::setInputForm, nextSpec::getInputForm);
            });

        Stream.ofNullable(typeSpec.getFunctions())
            .flatMap(Collection::stream)
            .forEach(functionSpec -> {
                definitionSpecProcessor.processTypeSpec(tenant, functionSpec::setInputSpec, functionSpec::getInputSpec);
                definitionSpecProcessor.processTypeSpec(tenant, functionSpec::setContextDataSpec, functionSpec::getContextDataSpec);
                formSpecProcessor.processTypeSpec(tenant,functionSpec::setInputForm, functionSpec::getInputForm);
                formSpecProcessor.processTypeSpec(tenant,functionSpec::setContextDataForm, functionSpec::getContextDataForm);
            });
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
            var target = objectMapper.readValue(type.getDataSpec(), Map.class);
            var parent = objectMapper.readValue(parentType.getDataSpec(), Map.class);
            if (parent.containsKey("additionalProperties")) {
                parent.put("additionalProperties", true);
            }
            target.put(XM_ENTITY_INHERITANCE_DEFINITION, Map.of(parentType.getKey(), parent));
            target.put("$ref", "#/" + XM_ENTITY_INHERITANCE_DEFINITION + "/" + parentType.getKey());
            String mergedJson = objectMapper.writeValueAsString(target);
            type.setDataSpec(mergedJson);
        } else {
            type.setDataSpec(type.getDataSpec() != null ? type.getDataSpec() : parentType.getDataSpec());
        }
    }

    private boolean hasDataSpec(TypeSpec type, TypeSpec parentType) {
        return type.getDataSpec() != null && parentType.getDataSpec() != null;
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

    @SneakyThrows
    private void extendDataForm(TypeSpec type, TypeSpec parentType, String tenant) {
        XmEntityTenantConfigService.XmEntityTenantConfig entityTenantConfig = this.tenantConfigService.getXmEntityTenantConfig(tenant);
        Boolean isInheritanceEnabled = entityTenantConfig.getEntitySpec().getEnableDataFromInheritance();
        if (isFeatureEnabled(isInheritanceEnabled, type.getDataFormInheritance()) && hasDataForm(type, parentType)) {
            ObjectMapper objectMapper = new ObjectMapper();
            var defaults = objectMapper.readValue(parentType.getDataForm(), Map.class);
            ObjectReader updater = objectMapper.readerForUpdating(defaults);
            Map<String, Object> merged = updater.readValue(type.getDataForm());
            String mergedJson = objectMapper.writeValueAsString(merged);
            type.setDataForm(mergedJson);
        } else {
            type.setDataForm(type.getDataForm() != null ? type.getDataForm() : parentType.getDataForm());
        }
    }

    @SneakyThrows
    private Map<String, TypeSpec> toTypeSpecsMap(String config) {
        XmEntitySpec xmEntitySpec = mapper.readValue(config, XmEntitySpec.class);
        List<TypeSpec> typeSpecs = xmEntitySpec.getTypes();
        if (isEmpty(typeSpecs)) {
            return Collections.emptyMap();
        } else {
            // Convert List<TypeSpec> to Map<key, TypeSpec>
            Map<String, TypeSpec> result = typeSpecs.stream()
                .collect(Collectors.toMap(TypeSpec::getKey, Function.identity(),
                    (u, v) -> {
                        throw new IllegalStateException(String.format("Duplicate key %s", u));
                    }, LinkedHashMap::new));
            return result;
        }
    }

    private void addJsonSchema(HashMap<String, com.github.fge.jsonschema.main.JsonSchema> dataSchemas,
                               JsonSchemaFactory jsonSchemaFactory, TypeSpec typeSpec) {
        if (StringUtils.isNotBlank(typeSpec.getDataSpec())) {
            try {
                var jsonSchema = jsonSchemaFactory.getJsonSchema(JsonLoader.fromString(typeSpec.getDataSpec()));
                dataSchemas.put(typeSpec.getKey(), jsonSchema);
            } catch (IOException | ProcessingException e) {
                log.error("Error processing data spec", e);
            }
        }
    }

    private boolean isFeatureEnabled(Boolean tenantFlag, Boolean entityFlag) {
        return TRUE.equals(entityFlag) || (TRUE.equals(tenantFlag) && !FALSE.equals(entityFlag));
    }

    private boolean hasDataForm(TypeSpec type, TypeSpec parentType) {
        return type.getDataForm() != null && parentType.getDataForm() != null;
    }

}
