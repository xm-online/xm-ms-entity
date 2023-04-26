package com.icthh.xm.ms.entity.service.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.ms.entity.domain.spec.DefinitionSpec;
import com.icthh.xm.ms.entity.domain.spec.XmEntitySpec;
import com.icthh.xm.ms.entity.service.json.JsonListenerService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
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
import static org.springframework.util.CollectionUtils.isEmpty;

@Slf4j
@Component
public class DefinitionSpecProcessor extends SpecProcessor {

    private static final String XM_ENTITY_DEFINITION = "xmEntityDefinition";
    private static final String REF_DEFINITION_PATTERN = "#/xmEntityDefinition/**/*";
    private static final String KEY_DEFINITION_TEMPLATE = "#/xmEntityDefinition/{definitionKey}/**";
    private final Map<String, Map<String, DefinitionSpec>> definitionsByTenant;

    public DefinitionSpecProcessor(JsonListenerService jsonListenerService) {
        super(jsonListenerService);
        this.definitionsByTenant = new ConcurrentHashMap<>();
    }

    public void updateDefinitionStateByTenant(String tenant, Map<String, Map<String, String>> typesByTenantByFile) {
        var definitionEntitySpec = typesByTenantByFile.get(tenant).values().stream()
            .map(this::toDefinitionSpecsMap)
            .map(Map::entrySet)
            .flatMap(Collection::stream)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (key1, key2) -> key1, LinkedHashMap::new));

        if (definitionEntitySpec.isEmpty()) {
            definitionsByTenant.remove(tenant);
        }
        definitionsByTenant.put(tenant, definitionEntitySpec);

        log.info("definitionEntitySpec.size={}", definitionEntitySpec.size());
    }

    @SneakyThrows
    private Map<String, DefinitionSpec> toDefinitionSpecsMap(String config) {
        XmEntitySpec xmEntitySpec = mapper.readValue(config, XmEntitySpec.class);
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

    @Override
    @SneakyThrows
    public void processTypeSpec(String tenant, Consumer<String> setter, Supplier<String> getter) {
        String spec = getter.get();
        if (StringUtils.isNotBlank(spec) && !definitionsByTenant.get(tenant).isEmpty()) {
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Map<String, Object>> entityDefinitions = new LinkedHashMap<>();
            var target = objectMapper.readValue(spec, Map.class);

            processDataSpec(tenant, spec, entityDefinitions);

            target.put(XM_ENTITY_DEFINITION, entityDefinitions);
            String mergedJson = objectMapper.writeValueAsString(target);
            setter.accept(mergedJson);
        }
    }

    @SneakyThrows
    private void processDataSpec(String tenant, String value, Map<String, Map<String, Object>> entityDefinitions) {
        Set<String> dataSpecReferencesByPattern = findDataSpecReferencesByPattern(value, REF_DEFINITION_PATTERN);
        if (dataSpecReferencesByPattern.isEmpty()) {
            return;
        }
        for (String dataRef : dataSpecReferencesByPattern) {
            String definitionKey = matcher.extractUriTemplateVariables(KEY_DEFINITION_TEMPLATE, dataRef).get("definitionKey");

            if (entityDefinitions.containsKey(definitionKey)) {
                return;
            }
            if (StringUtils.isNotBlank(definitionKey)) {
                ofNullable(definitionsByTenant.get(tenant))
                    .map(x -> x.get(definitionKey))
                    .map(definitionSpec -> getDefinitionSpecificationByFile(tenant, definitionSpec))
                    .filter(StringUtils::isNotBlank)
                    .ifPresentOrElse(
                        specification -> {
                            updateEntityDefinitionSpecification(specification, definitionKey, entityDefinitions);
                            processDataSpec(tenant, specification, entityDefinitions);
                        },
                        () -> log.warn("The definition specification for key:{} and tenant:{} was not found.",
                            definitionKey, tenant));
            }
        }
    }

    private String getDefinitionSpecificationByFile(String tenant, DefinitionSpec definitionSpec) {
        return ofNullable(definitionSpec.getValue())
            .orElseGet(() -> jsonListenerService.getSpecificationByTenantRelativePath(tenant, definitionSpec.getRef()));
    }

    private void updateEntityDefinitionSpecification(String specification, String definitionKey,
                                                     Map<String, Map<String, Object>> entityDefinitions) {
        try {
            var mapSpecification = mapper.readValue(specification, Map.class);
            entityDefinitions.put(definitionKey, mapSpecification);
        } catch (JsonProcessingException exception) {
            log.warn("Definition specification by key: {} couldn't be parsed: {}", definitionKey, exception.getMessage());
        }
    }
}
