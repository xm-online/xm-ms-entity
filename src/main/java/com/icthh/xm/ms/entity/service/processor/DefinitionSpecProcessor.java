package com.icthh.xm.ms.entity.service.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.ms.entity.domain.spec.DefinitionSpec;
import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import com.icthh.xm.ms.entity.domain.spec.XmEntitySpec;
import com.icthh.xm.ms.entity.service.json.JsonListenerService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.ObjectUtils.firstNonNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.springframework.util.CollectionUtils.isEmpty;

@Slf4j
@Component
public class DefinitionSpecProcessor extends SpecProcessor {

    public static final String XM_ENTITY_DEFINITION = "xmEntityDefinition";
    private static final String REF_DEFINITION_PATTERN = "#/xmEntityDefinition/**/*";
    private static final String KEY_DEFINITION_TEMPLATE = "#/xmEntityDefinition/{key}/**";

    public static final String XM_ENTITY_DATA_SPEC = "xmEntityDataSpec";
    private static final String REF_DATA_SPEC_PATTERN = "#/xmEntityDataSpec/*";
    private static final String KEY_DATA_SPEC_TEMPLATE = "#/xmEntityDataSpec/{key}";

    private final Map<String, Map<String, DefinitionSpec>> definitionsByTenant = new ConcurrentHashMap<>();
    private final Map<String, Map<String, TypeSpec>> originalTypeSpecs = new ConcurrentHashMap<>();
    private final Map<String, Map<String, DefinitionSpec>> processedDefinitionsByTenant = new ConcurrentHashMap<>();

    public DefinitionSpecProcessor(JsonListenerService jsonListenerService) {
        super(jsonListenerService);
    }

    @SneakyThrows
    public void updateDefinitionStateByTenant(String tenant, Map<String, Map<String, String>> typesByTenantByFile) {
        Map<String, TypeSpec> typeSpecs = new LinkedHashMap<>();
        Map<String, DefinitionSpec> defMap = new LinkedHashMap<>();

        Map<String, String> tenantFiles = typesByTenantByFile.getOrDefault(tenant, Map.of());
        for (Map.Entry<String, String> entry : tenantFiles.entrySet()) {
            String config = entry.getValue();
            XmEntitySpec xmEntitySpec = mapper.readValue(config, XmEntitySpec.class);

            List<TypeSpec> types = firstNonNull(xmEntitySpec.getTypes(), List.of());
            types.forEach(typeSpec -> {
                typeSpecs.put(typeSpec.getKey(), typeSpec);
            });

            defMap.putAll(toDefinitionSpecsMap(xmEntitySpec));
        }

        definitionsByTenant.put(tenant, defMap); // important to be before put typeSpecs
        originalTypeSpecs.put(tenant, typeSpecs);

        log.info("typeSpecs.size={}, definitionEntitySpec.size={}", typeSpecs.size(), defMap.size());
    }

    @SneakyThrows
    private Map<String, DefinitionSpec> toDefinitionSpecsMap(XmEntitySpec xmEntitySpec) {
        List<DefinitionSpec> definitionSpecs = xmEntitySpec.getDefinitions();
        if (isEmpty(definitionSpecs)) {
            return Collections.emptyMap();
        } else {
            return definitionSpecs.stream().collect(Collectors.toMap(DefinitionSpec::getKey, Function.identity(),
                (u, v) -> {
                    log.warn("Duplicate key found: {}", u);
                    throw new IllegalStateException(String.format("Duplicate key %s", u));
                }, LinkedHashMap::new));
        }
    }

    public void processDefinitionsItSelf(String tenant) {
        var definitions = definitionsByTenant.getOrDefault(tenant, Map.of());

        Map<String, DefinitionSpec> processed = new LinkedHashMap<>();
        definitions.forEach((key, value) -> {
            String definitionSpec = getDefinitionSpecificationByFile(tenant, value);
            if (isNotBlank(definitionSpec)) {
                Mutable<String> definition = new MutableObject<>(definitionSpec);
                processTypeSpec(tenant, definition::setValue, definition::getValue);
                DefinitionSpec spec = new DefinitionSpec();
                spec.setKey(value.getKey());
                spec.setValue(definition.getValue());
                processed.put(key, spec);
            }
        });
        processedDefinitionsByTenant.put(tenant, Map.copyOf(processed));
    }

    public List<DefinitionSpec> getProcessedDefinitions(String tenant) {
        Collection<DefinitionSpec> defs = processedDefinitionsByTenant.getOrDefault(tenant, Map.of()).values();
        return List.copyOf(defs);
    }

    @Override
    @SneakyThrows
    public void processTypeSpec(String tenant, Consumer<String> setter, Supplier<String> getter) {
        String spec = getter.get();
        if (isNotBlank(spec)) {
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Map<String, Object>> definitions = new LinkedHashMap<>();
            var target = objectMapper.readValue(spec, Map.class);

            processDataSpec(tenant, spec, definitions);

            target.putAll(definitions);
            String mergedJson = objectMapper.writeValueAsString(target);
            setter.accept(mergedJson);
        }
    }

    @SneakyThrows
    private void processDataSpec(String tenant, String value, Map<String, Map<String, Object>> target) {
        Set<String> definitionReferences = findDataSpecReferencesByPattern(value, REF_DEFINITION_PATTERN);
        for (String dataRef : definitionReferences) {
            processDefinition(
                tenant, target, dataRef,
                KEY_DEFINITION_TEMPLATE, XM_ENTITY_DEFINITION,
                definitionsByTenant,
                definitionSpec -> getDefinitionSpecificationByFile(tenant, definitionSpec)
            );
        }

        Set<String> dataSpecReferences = findDataSpecReferencesByPattern(value, REF_DATA_SPEC_PATTERN);
        for (String dataRef : dataSpecReferences) {
            processDefinition(
                tenant, target, dataRef,
                KEY_DATA_SPEC_TEMPLATE, XM_ENTITY_DATA_SPEC,
                originalTypeSpecs,
                TypeSpec::getDataSpec
            );
        }
    }

    private <T> void processDefinition(String tenant,
                                       Map<String, Map<String, Object>> target,
                                       String dataRef,
                                       String keyTemplate,
                                       String sectionName,
                                       Map<String, Map<String, T>> originsMap,
                                       Function<T, String> specMapper) {
        String key = matcher.extractUriTemplateVariables(keyTemplate, dataRef).get("key");
        Map<String, Object> entityDefinitions = target.computeIfAbsent(sectionName, k -> new LinkedHashMap<>());

        if (!entityDefinitions.containsKey(key) && isNotBlank(key)) {
            ofNullable(originsMap.get(tenant))
                .map(x -> x.get(key))
                .map(specMapper)
                .filter(StringUtils::isNotBlank)
                .ifPresentOrElse(
                    specification -> {
                        updateEntityDefinitionSpecification(specification, key, entityDefinitions);
                        processDataSpec(tenant, specification, target);
                    },
                    () -> log.warn("The specification for key:{} and tenant:{} was not found.",
                        key, tenant));
        }
    }

    private String getDefinitionSpecificationByFile(String tenant, DefinitionSpec definitionSpec) {
        return ofNullable(definitionSpec.getValue())
            .orElseGet(() -> jsonListenerService.getSpecificationByTenantRelativePath(tenant, definitionSpec.getRef()));
    }

    private void updateEntityDefinitionSpecification(String specification, String definitionKey,
                                                     Map<String, Object> entityDefinitions) {
        try {
            var mapSpecification = mapper.readValue(specification, Map.class);
            entityDefinitions.put(definitionKey, mapSpecification);
        } catch (JsonProcessingException exception) {
            log.warn("Definition specification by key: {} couldn't be parsed: {}", definitionKey, exception.getMessage());
        }
    }
}
