package com.icthh.xm.ms.entity.service.processor;

import com.icthh.xm.ms.entity.domain.spec.DefinitionSpec;
import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import com.icthh.xm.ms.entity.domain.spec.XmEntitySpec;
import com.icthh.xm.ms.entity.service.JsonListenerService;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.springframework.util.CollectionUtils.isEmpty;

@Component
public class DefinitionSpecProcessor extends SpecProcessor {
    private static final String XM_ENTITY_DEFINITION = "xmEntityDefinition";
    private static final String REF_DEFINITION_PATTERN = "#/xmEntityDefinition/**/*";
    private static final String KEY_DEFINITION_TEMPLATE = "#/xmEntityDefinition/{definitionKey}/*";
    private static final String VALUE_DEFINITION_TEMPLATE = "#/{definitionKey}/*";
    private final ConcurrentHashMap<String, Map<String, DefinitionSpec>> definitionsByTenant;
    private final ConcurrentHashMap<String, Map<String, Object>> entityDefinitions;

    public DefinitionSpecProcessor(JsonListenerService jsonListenerService) {
        super(jsonListenerService);
        this.definitionsByTenant = new ConcurrentHashMap<>();
        this.entityDefinitions = new ConcurrentHashMap<>();
    }

    public void updateDefinitionStateByTenant(String tenant, Map<String, Map<String, String>> typesByTenantByFile) {
        var definitionEntitySpec = new LinkedHashMap<String, DefinitionSpec>();
        typesByTenantByFile.get(tenant).values().stream().map(this::toDefinitionSpecsMap).forEach(definitionEntitySpec::putAll);

        if (definitionEntitySpec.isEmpty()) {
            definitionsByTenant.remove(tenant);
        }
        definitionsByTenant.put(tenant, definitionEntitySpec);
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
                    throw new IllegalStateException(String.format("Duplicate key %s", u));
                }, LinkedHashMap::new));
        }
    }

    @Override
    @SneakyThrows
    public TypeSpec processTypeSpec(String tenant, TypeSpec typeSpec) {
        var target = mapper.readValue(typeSpec.getDataSpec(), Map.class);
        List<String> existingReferences = findDataSpecReferencesByPattern(typeSpec.getDataSpec(), REF_DEFINITION_PATTERN);

        for (String definitionPath : existingReferences) {
            String definitionKey = matcher.extractUriTemplateVariables(KEY_DEFINITION_TEMPLATE, definitionPath).get("definitionKey");

            if (StringUtils.isNotBlank(definitionKey)) {

                DefinitionSpec definitionSpec = definitionsByTenant.get(tenant).get(definitionKey);

                String tenantSpecification = Optional.ofNullable(definitionSpec.getValue())
                    .orElse(jsonListenerService.getSpecificationByTenantRelativePath(tenant, definitionSpec.getRef()));

                var mapSpecification = mapper.readValue(tenantSpecification, Map.class);
                entityDefinitions.put(definitionKey, mapSpecification);
                processValue(tenant, tenantSpecification);
            }
        }

        target.put(XM_ENTITY_DEFINITION, entityDefinitions);

        String mergedJson = mapper.writeValueAsString(target);
        typeSpec.setDataSpec(mergedJson);
        return typeSpec;
    }

    @SneakyThrows
    private void processValue(String tenant, String value) {
        List<String> dataSpecReferencesByPattern = findDataSpecReferencesByPattern(value, VALUE_DEFINITION_TEMPLATE);

        if (dataSpecReferencesByPattern.isEmpty()) {
            return;
        }

        for (String dataRef : dataSpecReferencesByPattern) {
            String definitionKey = matcher.extractUriTemplateVariables(VALUE_DEFINITION_TEMPLATE, dataRef).get("definitionKey");

            if (entityDefinitions.containsKey(definitionKey)) {
                return;
            }

            DefinitionSpec definitionSpec = definitionsByTenant.get(tenant).get(definitionKey);

            String tenantSpecification = Optional.ofNullable(definitionSpec.getValue())
                .orElse(jsonListenerService.getSpecificationByTenantRelativePath(tenant, definitionSpec.getRef()));

            var mapSpecification = mapper.readValue(tenantSpecification, Map.class);
            entityDefinitions.put(definitionKey, mapSpecification);
            processValue(tenant, tenantSpecification);

        }
    }

}
