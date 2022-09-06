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
import org.apache.commons.lang.StringUtils;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;
import static org.springframework.util.CollectionUtils.isEmpty;

public class FormSpecProcessor extends SpecProcessor {
    private static final String REF_FORM_PATTERN = "#/xmEntityForm/**/*";
    private static final String KEY_FORM_TEMPLATE = "#/xmEntityForm/{formKey}";
    private final ConcurrentHashMap<String, Map<String, FormSpec>> formsByTenant;

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
        List<String> existingReferenceValues = findDataSpecReferencesByPattern(typeSpec.getDataForm(), REF_FORM_PATTERN);

        if (existingReferenceValues.isEmpty()) {
            return typeSpec;
        }

        Map<String, String> specifications = collectTenantSpecifications(existingReferenceValues, tenant);
        resolveReferences(specifications, typeSpec);

        return processTypeSpec(tenant, typeSpec);
    }

    private Map<String, String> collectTenantSpecifications(List<String> existingReferences, String tenant) {
        Map<String, String> specificationsByRelativePath = new LinkedHashMap<>();

        for (String formPath : existingReferences) {
            String formKey = matcher.extractUriTemplateVariables(KEY_FORM_TEMPLATE, formPath).get("formKey");

            if (StringUtils.isNotBlank(formKey)) {

                ofNullable(formsByTenant.get(tenant).get(formKey)).ifPresent(formSpec -> {

                        String tenantSpecification = ofNullable(formSpec.getValue())
                            .orElse(jsonListenerService.getSpecificationByTenantRelativePath(tenant, formSpec.getRef()));

                        specificationsByRelativePath.put(formSpec.getKey(), tenantSpecification);
                    }
                );

            }
        }
        return specificationsByRelativePath;
    }

    private void resolveReferences(Map<String, String> specifications, TypeSpec typeSpec) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        var defaults = objectMapper.readValue(typeSpec.getDataForm(), JsonNode.class);
        JsonNode jsonNode = replaceReferences(defaults, specifications);


        typeSpec.setDataForm(objectMapper.writeValueAsString(jsonNode));
    }


    private JsonNode replaceReferences(JsonNode node, Map<String, String> specifications) {
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            Iterator<Map.Entry<String, JsonNode>> it = objectNode.fields();

            while (it.hasNext()) {
                Map.Entry<String, JsonNode> entry = it.next();
                String jsonKey = entry.getKey();

                if (jsonKey.equals(REF)) {
                    String value = entry.getValue().asText();
                    objectNode.remove(REF);

                    specifications.keySet()
                        .stream()
                        .filter(value::contains)
                        .findAny()
                        .ifPresent(key -> objectNode.setAll(
                            convertSpecificationToObjectNode(specifications.get(key))
                        ));
                }
                replaceReferences(entry.getValue(), specifications);
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
        return (ObjectNode) mapper.readTree(specification);
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
                    throw new IllegalStateException(String.format("Duplicate key %s", u));
                }, LinkedHashMap::new));
        }
    }
}
