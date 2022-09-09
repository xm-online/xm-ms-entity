package com.icthh.xm.ms.entity.service.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.icthh.xm.ms.entity.domain.spec.FormSpec;
import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import com.icthh.xm.ms.entity.domain.spec.XmEntitySpec;
import com.icthh.xm.ms.entity.service.JsonListenerService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;
import static org.springframework.util.CollectionUtils.isEmpty;

@Slf4j
@Component
public class FormSpecProcessor extends SpecProcessor {
    private static final String REF_FORM_PATTERN = "#/xmEntityForm/**/*";
    private static final String KEY_FORM_TEMPLATE = "#/xmEntityForm/{formKey}";
    private static final int POSSIBLE_RECURSIVE_LIMIT = 10;
    private final Map<String, Map<String, FormSpec>> formsByTenant;

    public FormSpecProcessor(JsonListenerService jsonListenerService) {
        super(jsonListenerService);
        this.formsByTenant = new ConcurrentHashMap<>();
    }

    public void updateFormStateByTenant(String tenant, Map<String, Map<String, String>> typesByTenantByFile) {
        var definitionEntitySpec = new LinkedHashMap<String, FormSpec>();
        typesByTenantByFile.get(tenant).values().stream().map(this::toFormSpecsMap).forEach(definitionEntitySpec::putAll);

        if (definitionEntitySpec.isEmpty()) {
            formsByTenant.remove(tenant);
        }
        formsByTenant.put(tenant, definitionEntitySpec);
    }

    @SneakyThrows
    @Override
    public TypeSpec processTypeSpec(String tenant, TypeSpec typeSpec) {
        if (StringUtils.isNotBlank(typeSpec.getDataForm()) && !formsByTenant.get(tenant).isEmpty()) {
            TypeSpec updatedTypeSpec = typeSpec.toBuilder().build();

            for (int i = 0; i < POSSIBLE_RECURSIVE_LIMIT; i++) {
                Set<String> existingReferenceValues = findDataSpecReferencesByPattern(updatedTypeSpec.getDataForm(), REF_FORM_PATTERN);

                if (existingReferenceValues.isEmpty()) {
                    typeSpec.setDataForm(updatedTypeSpec.getDataForm());
                    return typeSpec;
                }

                Map<String, String> specifications = collectTenantSpecifications(existingReferenceValues, tenant);
                resolveReferences(specifications, updatedTypeSpec);
            }
            log.warn("Maximum iteration limit reached: {}. Could be recursion and form has not been processed", POSSIBLE_RECURSIVE_LIMIT);
        }
        return typeSpec;
    }

    private Map<String, String> collectTenantSpecifications(Set<String> existingReferences, String tenant) {
        Map<String, String> specificationsByRelativePath = new LinkedHashMap<>();

        for (String formPath : existingReferences) {
            String formKey = matcher.extractUriTemplateVariables(KEY_FORM_TEMPLATE, formPath).get("formKey");

            if (StringUtils.isNotBlank(formKey)) {
                ofNullable(formsByTenant.get(tenant))
                    .map(x -> x.get(formKey))
                    .map(formSpec -> getFormSpecificationByFile(tenant, formSpec))
                    .filter(StringUtils::isNotBlank)
                    .ifPresentOrElse(
                        specification -> specificationsByRelativePath.put(formKey, specification),
                        () -> log.warn("The form specification for key:{} and tenant:{} was not found.", formKey, tenant));
            }
        }
        return specificationsByRelativePath;
    }

    private void resolveReferences(Map<String, String> specifications, TypeSpec typeSpec) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode defaults = objectMapper.readValue(typeSpec.getDataForm(), JsonNode.class);
        JsonNode jsonNode = replaceReferences(defaults, specifications);

        typeSpec.setDataForm(objectMapper.writeValueAsString(jsonNode));
    }

    private String getFormSpecificationByFile(String tenant, FormSpec formSpec) {
        return ofNullable(formSpec.getValue())
            .orElseGet(() -> jsonListenerService.getSpecificationByTenantRelativePath(tenant, formSpec.getRef()));
    }

    private JsonNode replaceReferences(JsonNode node, Map<String, String> specifications) {
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            Iterator<Map.Entry<String, JsonNode>> it = convertToFailSafeIterator(objectNode.fields());

            while (it.hasNext()) {
                Map.Entry<String, JsonNode> entry = it.next();
                String jsonKey = entry.getKey();
                JsonNode value = entry.getValue();
                if (jsonKey.equals(REF)) {
                    String textValue = entry.getValue().asText();
                    objectNode.remove(REF);

                    specifications.keySet()
                        .stream()
                        .filter(textValue::contains)
                        .findAny()
                        .ifPresent(key -> objectNode.setAll(
                            convertSpecificationToObjectNode(specifications.get(key))));
                }
                replaceReferences(value, specifications);
            }
        }
        if (node.isArray()) {
            for (JsonNode jsonNode : node) {
                replaceReferences(jsonNode, specifications);
            }
        }
        return node;
    }

    @SneakyThrows
    private ObjectNode convertSpecificationToObjectNode(String specification) {
        ObjectMapper objectMapper = new ObjectMapper();
        return (ObjectNode) objectMapper.readTree(specification);
    }

    @SneakyThrows
    private Map<String, FormSpec> toFormSpecsMap(String config) {
        XmEntitySpec xmEntitySpec = mapper.readValue(config, XmEntitySpec.class);
        List<FormSpec> definitionSpecs = xmEntitySpec.getForms();
        if (isEmpty(definitionSpecs)) {
            return Collections.emptyMap();
        } else {
            return definitionSpecs.stream().collect(Collectors.toMap(FormSpec::getKey, Function.identity(),
                (u, v) -> {
                    log.warn("Duplicate key found: {}", u);
                    throw new IllegalStateException(String.format("Duplicate key %s", u));
                }, LinkedHashMap::new));
        }
    }

    private Iterator<Map.Entry<String, JsonNode>> convertToFailSafeIterator(Iterator<Map.Entry<String, JsonNode>> failFastIterator) {
        ConcurrentHashMap<String, JsonNode> concurrentHashMap = new ConcurrentHashMap<>();

        failFastIterator.forEachRemaining((entry) -> concurrentHashMap.put(entry.getKey(), entry.getValue()));
        return concurrentHashMap.entrySet().iterator();
    }
}
