package com.icthh.xm.ms.entity.service.spec;

import com.icthh.xm.commons.domain.DefinitionSpec;
import com.icthh.xm.commons.domain.SpecificationItem;
import com.icthh.xm.commons.service.SpecificationProcessingService;
import com.icthh.xm.ms.entity.domain.spec.StateSpec;
import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import com.icthh.xm.ms.entity.domain.spec.XmEntitySpec;
import com.icthh.xm.ms.entity.service.processor.XmEntityDefinitionSpecProcessor;
import com.icthh.xm.ms.entity.service.processor.XmEntityDataFormSpecProcessor;
import com.icthh.xm.ms.entity.service.processor.XmEntityTypeSpecProcessor;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaException;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static com.icthh.xm.commons.utils.DataSpecConstants.DEFINITIONS;
import static com.icthh.xm.ms.entity.service.processor.XmEntityDefinitionSpecProcessor.XM_ENTITY_DEFINITION;
import static com.icthh.xm.ms.entity.service.processor.XmEntityTypeSpecProcessor.XM_ENTITY_DATA_SPEC;
import static com.icthh.xm.ms.entity.service.spec.SpecInheritanceProcessor.XM_ENTITY_INHERITANCE_DEFINITION;
import static com.icthh.xm.ms.entity.util.CustomCollectionUtils.nullSafe;

@Slf4j
@Service
public class DataSpecJsonSchemaService implements SpecificationProcessingService<XmEntitySpec> {

    public static final Set<String> DEFINITION_PREFIXES = Set.of(DEFINITIONS, XM_ENTITY_DEFINITION,
        XM_ENTITY_INHERITANCE_DEFINITION, XM_ENTITY_DATA_SPEC);

    private final ConcurrentHashMap<String, Map<String, JsonSchema>> dataSpecJsonSchemas = new ConcurrentHashMap<>();

    private final XmEntityDefinitionSpecProcessor definitionSpecProcessor;
    private final XmEntityDataFormSpecProcessor formSpecProcessor;
    private final XmEntityTypeSpecProcessor typeSpecProcessor;

    public DataSpecJsonSchemaService(XmEntityDefinitionSpecProcessor definitionSpecProcessor,
                                     XmEntityDataFormSpecProcessor formSpecProcessor,
                                     XmEntityTypeSpecProcessor typeSpecProcessor) {
        this.definitionSpecProcessor = definitionSpecProcessor;
        this.formSpecProcessor = formSpecProcessor;
        this.typeSpecProcessor = typeSpecProcessor;
    }

    public Map<String, JsonSchema> dataSpecJsonSchemas(String tenantKey) {
        return nullSafe(dataSpecJsonSchemas.get(tenantKey));
    }

    @Override
    public <I extends SpecificationItem> Collection<I> processDataSpecifications(String tenant, String dataSpecKey, Collection<I> specifications) {
        var dataSchemas = new HashMap<String, JsonSchema>();
        JsonSchemaFactory jsonSchemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
        for (I typeSpec : specifications) {
            processDataSpecification(tenant, dataSpecKey, (TypeSpec) typeSpec);
            addJsonSchema(dataSchemas, jsonSchemaFactory, (TypeSpec) typeSpec);
        }
        dataSpecJsonSchemas.put(tenant, dataSchemas);
        log.info("dataSchemas.size={}", dataSchemas.size());
        return specifications;
    }

    @Override
    public void updateByTenantState(String tenant, String dataSpecKey, Collection<XmEntitySpec> specifications) {
        specifications.stream()
            .filter(Objects::nonNull)
            .forEach(spec -> {
                definitionSpecProcessor.updateStateByTenant(tenant, dataSpecKey, spec.getDefinitions());
                typeSpecProcessor.updateStateByTenant(tenant, dataSpecKey, spec.getTypes());
                formSpecProcessor.updateStateByTenant(tenant, dataSpecKey, spec.getForms());
            });
    }

    public void processDataSpecification(String tenant, String dataSpecKey, TypeSpec typeSpec) {

        definitionSpecProcessor.processDataSpec(tenant, dataSpecKey, typeSpec::setDataSpec, typeSpec::getDataSpec);
        formSpecProcessor.processDataSpec(tenant, dataSpecKey, typeSpec::setDataForm, typeSpec::getDataForm);

        Stream.ofNullable(typeSpec.getStates())
            .flatMap(Collection::stream)
            .map(StateSpec::getNext)
            .filter(Objects::nonNull)
            .flatMap(Collection::stream)
            .forEach(nextSpec -> {
                definitionSpecProcessor.processDataSpec(tenant, dataSpecKey, nextSpec::setInputSpec, nextSpec::getInputSpec);
                formSpecProcessor.processDataSpec(tenant, dataSpecKey,nextSpec::setInputForm, nextSpec::getInputForm);
            });

        Stream.ofNullable(typeSpec.getFunctions())
            .flatMap(Collection::stream)
            .forEach(functionSpec -> {
                definitionSpecProcessor.processDataSpec(tenant, dataSpecKey, functionSpec::setInputSpec, functionSpec::getInputSpec);
                definitionSpecProcessor.processDataSpec(tenant, dataSpecKey, functionSpec::setContextDataSpec, functionSpec::getContextDataSpec);
                formSpecProcessor.processDataSpec(tenant, dataSpecKey, functionSpec::setInputForm, functionSpec::getInputForm);
                formSpecProcessor.processDataSpec(tenant, dataSpecKey, functionSpec::setContextDataForm, functionSpec::getContextDataForm);
            });
        definitionSpecProcessor.processDefinitionsItSelf(tenant, dataSpecKey);
    }

    private void addJsonSchema(HashMap<String, JsonSchema> dataSchemas,
                               JsonSchemaFactory jsonSchemaFactory, TypeSpec typeSpec) {
        if (StringUtils.isNotBlank(typeSpec.getDataSpec())) {
            try {
                var jsonSchema = jsonSchemaFactory.getSchema(typeSpec.getDataSpec());
                dataSchemas.put(typeSpec.getKey(), jsonSchema);
            } catch (JsonSchemaException e) {
                log.error("Error processing data spec", e);
            }
        }
    }

    public List<DefinitionSpec> getDefinitions(String tenantKeyValue, String dataSpecKey) {
        return new ArrayList<>(definitionSpecProcessor.getProcessedSpecsCopy(tenantKeyValue, dataSpecKey));
    }
}
