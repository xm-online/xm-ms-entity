package com.icthh.xm.ms.entity.service.spec;

import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.icthh.xm.ms.entity.domain.spec.DefinitionSpec;
import com.icthh.xm.ms.entity.domain.spec.StateSpec;
import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import com.icthh.xm.ms.entity.service.processor.DefinitionSpecProcessor;
import com.icthh.xm.ms.entity.service.processor.FormSpecProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static com.icthh.xm.ms.entity.service.processor.DefinitionSpecProcessor.XM_ENTITY_DATA_SPEC;
import static com.icthh.xm.ms.entity.service.processor.DefinitionSpecProcessor.XM_ENTITY_DEFINITION;
import static com.icthh.xm.ms.entity.service.spec.SpecInheritanceProcessor.XM_ENTITY_INHERITANCE_DEFINITION;
import static com.icthh.xm.ms.entity.util.CustomCollectionUtils.nullSafe;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSpecJsonSchemaService {

    public static final String DEFINITIONS = "definitions";
    public static final Set<String> DEFINITION_PREFIXES = Set.of(DEFINITIONS, XM_ENTITY_DEFINITION,
        XM_ENTITY_INHERITANCE_DEFINITION, XM_ENTITY_DATA_SPEC);

    private final ConcurrentHashMap<String, Map<String, JsonSchema>> dataSpecJsonSchemas = new ConcurrentHashMap<>();

    private final DefinitionSpecProcessor definitionSpecProcessor;
    private final FormSpecProcessor formSpecProcessor;

    public Map<String, JsonSchema> dataSpecJsonSchemas(String tenantKey) {
        return nullSafe(dataSpecJsonSchemas.get(tenantKey));
    }

    public void updateByTenant(String tenant, Map<String, Map<String, String>> typesByTenantByFile) {
        definitionSpecProcessor.updateDefinitionStateByTenant(tenant, typesByTenantByFile);
        formSpecProcessor.updateFormStateByTenant(tenant, typesByTenantByFile);
    }

    public void processDataSpec(String tenantKey, LinkedHashMap<String, TypeSpec> tenantEntitySpec) {
        var dataSchemas = new HashMap<String, JsonSchema>();
        JsonSchemaFactory jsonSchemaFactory = JsonSchemaFactory.byDefault();
        for (TypeSpec typeSpec : tenantEntitySpec.values()) {
            processTypeSpec(tenantKey, typeSpec);
            addJsonSchema(dataSchemas, jsonSchemaFactory, typeSpec);
        }
        dataSpecJsonSchemas.put(tenantKey, dataSchemas);
        log.info("dataSchemas.size={}", dataSchemas.size());
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
        definitionSpecProcessor.processDefinitionsItSelf(tenant);
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

    public List<DefinitionSpec> getDefinitions(String tenantKeyValue) {
        return definitionSpecProcessor.getProcessedDefinitions(tenantKeyValue);
    }
}
