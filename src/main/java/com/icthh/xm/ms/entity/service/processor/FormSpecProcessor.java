package com.icthh.xm.ms.entity.service.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ContainerNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.icthh.xm.ms.entity.domain.spec.FormSpec;
import com.icthh.xm.ms.entity.domain.spec.XmEntitySpec;
import com.icthh.xm.ms.entity.service.JsonListenerService;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.modelmapper.internal.util.Objects.firstNonNull;
import static org.springframework.util.CollectionUtils.isEmpty;

@Slf4j
@Component
public class FormSpecProcessor extends SpecProcessor {
    private static final String REF_FORM_PATTERN = "#/xmEntityForm/**/*";
    private static final String KEY_FORM_TEMPLATE = "#/xmEntityForm/{formKey}";
    private static final String KEY_FORM_PREFIX = "#/xmEntityForm/";
    private static final int POSSIBLE_RECURSIVE_LIMIT = 10;
    private final Map<String, Map<String, FormSpec>> formsByTenant;

    public FormSpecProcessor(JsonListenerService jsonListenerService) {
        super(jsonListenerService);
        this.formsByTenant = new ConcurrentHashMap<>();
    }

    public void updateFormStateByTenant(String tenant, Map<String, Map<String, String>> typesByTenantByFile) {
        var formEntitySpec = typesByTenantByFile.get(tenant).values().stream()
            .map(this::toFormSpecsMap)
            .map(Map::entrySet)
            .flatMap(Collection::stream)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (key1, key2) -> key1, LinkedHashMap::new));

        if (formEntitySpec.isEmpty()) {
            formsByTenant.remove(tenant);
        }
        formsByTenant.put(tenant, formEntitySpec);
    }

    @SneakyThrows
    @Override
    public void processTypeSpec(String tenant, Consumer<String> setter, Supplier<String> getter) {
        String typeSpecForm = getter.get();
        if (StringUtils.isNotBlank(typeSpecForm) && !formsByTenant.get(tenant).isEmpty()) {
            String updatedSpecForm = typeSpecForm;

            for (int i = 0; i < POSSIBLE_RECURSIVE_LIMIT; i++) {
                Set<String> existingReferenceValues = findDataSpecReferencesByPattern(updatedSpecForm, REF_FORM_PATTERN);

                if (existingReferenceValues.isEmpty()) {
                    setter.accept(updatedSpecForm);
                    return;
                }

                Map<String, String> specifications = collectTenantSpecifications(existingReferenceValues, tenant);
                updatedSpecForm = resolveReferences(specifications, updatedSpecForm);
            }
            log.warn("Maximum iteration limit reached: {}. Could be recursion and form has not been processed", POSSIBLE_RECURSIVE_LIMIT);
        }
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
                        specification -> specificationsByRelativePath.put(KEY_FORM_PREFIX + formKey, specification),
                        () -> log.warn("The form specification for key:{} and tenant:{} was not found.", formKey, tenant));
            }
        }
        return specificationsByRelativePath;
    }

    private String resolveReferences(Map<String, String> specifications, String typeSpecForm) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode defaults = objectMapper.readValue(typeSpecForm, JsonNode.class);
        JsonNode jsonNode = replaceReferences(defaults, specifications, null);
        return objectMapper.writeValueAsString(jsonNode);
    }

    private String getFormSpecificationByFile(String tenant, FormSpec formSpec) {
        return ofNullable(formSpec.getValue())
            .orElseGet(() -> jsonListenerService.getSpecificationByTenantRelativePath(tenant, formSpec.getRef()));
    }

    private JsonNode replaceReferences(JsonNode node, Map<String, String> specifications, ContainerNode<?> parentNode) {
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;

            Map<String, JsonNode> objectMap = copyToMap(objectNode.fields());
            JsonNode refNode = objectMap.get(REF);
            JsonNode keyNode = objectMap.getOrDefault(KEY, mapper.createObjectNode());

            objectNode.remove(REF);
            if (refNode != null && objectNode.size() == 1) {
                objectNode.remove(KEY);
            }

            String refPath = firstNonNull(refNode, mapper.createObjectNode()).asText();
            String formSpec = specifications.getOrDefault(refPath, "{}");
            var fromSpecNode = this.convertSpecificationToObjectNodes(formSpec);
            processFromSpecNode(fromSpecNode, keyNode.asText());
            injectJsonNode(parentNode, objectNode, fromSpecNode, refPath);

            objectMap.values().forEach(childNode -> replaceReferences(childNode, specifications, objectNode));
        } else if (node.isArray()) {
            List<JsonNode> copiedList = newArrayList(node.iterator());
            for (JsonNode jsonNode : copiedList) {
                replaceReferences(jsonNode, specifications, (ArrayNode) node);
            }
        }
        return node;
    }

    private void processFromSpecNode(JsonNode fromSpecNode, String prefix) {
        if (isBlank(prefix)) {
            return;
        }

        if (fromSpecNode.isObject()) {
            ObjectNode objectNode = (ObjectNode) fromSpecNode;
            Map<String, JsonNode> objectMap = copyToMap(objectNode.fields());
            JsonNode keyNode = objectMap.get(KEY);
            if (keyNode != null) {
                objectNode.put(KEY, prefix + '.' + keyNode.asText());
            } else if (objectMap.containsKey(REF)) {
                objectNode.put(KEY, prefix);
            }
            objectMap.values().forEach(it -> this.processFromSpecNode(it, prefix));
        } else if (fromSpecNode.isArray()) {
            List<JsonNode> copiedList = newArrayList(fromSpecNode.iterator());
            for (JsonNode jsonNode : copiedList) {
                processFromSpecNode(jsonNode, prefix);
            }
        }
    }

    private void injectJsonNode(ContainerNode<?> parentNode, ObjectNode objectNode, ContainerNode<?> jsonNode, String refPath) {
        if (jsonNode.isArray() && parentNode.isObject()) {
            log.warn("Array by $ref {} has been injected to the object instead of array.", refPath);
         } else if (jsonNode.isArray() && parentNode.isArray()) {
            processArrayNode((ArrayNode) jsonNode, (ArrayNode) parentNode, objectNode);
        } else {
            objectNode.setAll((ObjectNode) jsonNode);
            removeEmptyObject(parentNode, objectNode);
        }
    }

    private static void removeEmptyObject(ContainerNode<?> parentNode, ObjectNode objectNode) {
        if (parentNode != null && parentNode.isArray() && objectNode.isEmpty()) {
            List<JsonNode> elements = newArrayList(parentNode.elements());
            elements.remove(indexOfByReference(objectNode, elements));
            parentNode.removeAll();
            ((ArrayNode) parentNode).addAll(elements);
        }
    }

    @SneakyThrows
    private ContainerNode<?> convertSpecificationToObjectNodes(String specification) {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(specification, ContainerNode.class);
    }

    @SneakyThrows
    private Map<String, FormSpec> toFormSpecsMap(String config) {
        XmEntitySpec xmEntitySpec = mapper.readValue(config, XmEntitySpec.class);
        List<FormSpec> formSpecs = xmEntitySpec.getForms();
        if (isEmpty(formSpecs)) {
            return Collections.emptyMap();
        } else {
            return formSpecs.stream().collect(Collectors.toMap(FormSpec::getKey, Function.identity(),
                (u, v) -> {
                    log.warn("Duplicate key found: {}", u);
                    throw new IllegalStateException(String.format("Duplicate key %s", u));
                }, LinkedHashMap::new));
        }
    }

    private Map<String, JsonNode> copyToMap(Iterator<Map.Entry<String, JsonNode>> failFastIterator) {
        Map<String, JsonNode> jsonNodeMap = new LinkedHashMap<>();
        failFastIterator.forEachRemaining((entry) -> jsonNodeMap.put(entry.getKey(), entry.getValue()));
        return jsonNodeMap;
    }

    private void processArrayNode(ArrayNode arrayNode, ArrayNode parentArray, ObjectNode objectNode) {
        List<JsonNode> arrayNodesList = newArrayList(arrayNode.iterator());
        List<JsonNode> newParentArray = newArrayList(parentArray.elements());
        int index = indexOfByReference(objectNode, newParentArray);
        newParentArray.addAll(index + 1, arrayNodesList);
        if (objectNode.isEmpty()) {
            newParentArray.remove(objectNode);
        }
        parentArray.removeAll();
        parentArray.addAll(newParentArray);

    }

    private static int indexOfByReference(ObjectNode objectNode, List<JsonNode> list) {
        for (int i = 0; i < list.size(); i++) {
            if (objectNode == list.get(i)) {
                return i;
            }
        }
        return -1;
    }
}
