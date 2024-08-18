package com.icthh.xm.ms.entity.service.swagger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.icthh.xm.commons.exceptions.BusinessException;
import lombok.SneakyThrows;
import org.apache.commons.collections4.IteratorUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.fasterxml.jackson.databind.node.JsonNodeFactory.instance;
import static com.google.common.collect.Streams.stream;
import static com.icthh.xm.ms.entity.service.processor.DefinitionSpecProcessor.XM_ENTITY_DEFINITION;
import static com.icthh.xm.ms.entity.service.spec.SpecInheritanceProcessor.XM_ENTITY_INHERITANCE_DEFINITION;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class JsonSchemaToSwaggerSchemaConverter {

    public static final String DEFINITIONS = "definitions";
    public static final Set<String> DEFINITION_PREFIXES = Set.of(DEFINITIONS, XM_ENTITY_DEFINITION, XM_ENTITY_INHERITANCE_DEFINITION);

    private static final Set<String> supportedKeywords = Set.of(
        "$ref", DEFINITIONS,

        // Keywords with the same meaning as in JSON Schema
        "title", "pattern", "required", "enum", "minimum", "maximum",
        "exclusiveMinimum", "exclusiveMaximum", "multipleOf", "minLength",
        "maxLength", "minItems", "maxItems", "uniqueItems", "minProperties",
        "maxProperties",

        // Keywords with minor differences
        "type", "format", "description", "items", "properties",
        "additionalProperties", "default", "allOf", "oneOf",
        "anyOf", "not",

        // Additional keywords
        "deprecated", "discriminator", "example", "externalDocs",
        "nullable", "readOnly", "writeOnly", "xml"
    );

    private static final Set<String> validTypes = Set.of(
        "null", "boolean", "object", "array", "number", "string", "integer"
    );
    public final ObjectMapper objectMapper = new ObjectMapper();

    public String transformToSwaggerJson(String typeName, String jsonSchema, Map<String, Object> definitions) {
        JsonNode json = transformToJsonNode(typeName, jsonSchema, definitions);
        return writeJson(json);
    }

    public JsonNode transformToJsonNode(String typeName, String jsonSchema, Map<String, Object> definitions) {
        if (isBlank(jsonSchema)) {
            return instance.nullNode();
        }

        JsonNode json = readJson(jsonSchema);
        if (!json.isObject()) {
            throw new IllegalArgumentException("Json schema should be an object");
        }

        if (!json.has("type") && !json.has("properties")) {
            ObjectNode object = object("type", instance.textNode("object"));
            object.set("properties", json);
            json = object;
        }
        transformToSwaggerJson(typeName, (ObjectNode) json, definitions);
        return json;
    }

    private void transformToSwaggerJson(String typeName, ObjectNode json, Map<String, Object> definitions) {
        if (!json.isObject()) {
            return;
        }

        traverseSchema(instance.nullNode(), typeName, json);
        processDefinitions(typeName, json, definitions);
    }

    private void traverseSchema(JsonNode parent, String fieldName, JsonNode json) {
        if (json == null) {
            return;
        }

        if (json.isArray()) {
            int index = 0;
            json.forEach(it -> {
                traverseSchema(json, fieldName + "__" + index, it);
            });
        } else if (json.isObject()) {
            convert(parent, fieldName, json);
            json.fields().forEachRemaining(it -> {
                traverseSchema(json, it.getKey(), it.getValue());
            });
        }
    }

    private void convert(JsonNode parent, String fieldName, JsonNode json) {
        if (json == null || !json.isObject()) {
            return;
        }

        ObjectNode object = (ObjectNode) json;

        convertNullable(parent, fieldName, object);
        rewriteConst(object);
        rewriteIfThenElse(object);
        rewriteExclusiveMinMax(object);
        convertDependencies(parent, fieldName, object);
        rewriteUnsupportedKeywords(parent, fieldName, object);
        removeEmptyRequired(object);
    }

    private void removeEmptyRequired(ObjectNode object) {
        if (object.has("required") && object.get("required").isArray() && object.get("required").isEmpty()) {
            object.remove("required");
        }
    }

    private void convertDependencies(JsonNode parent, String fieldName, ObjectNode schema) {
        if (insideProperties(parent, fieldName)) {
            return;
        }

        JsonNode deps = schema.get("dependencies");
        if (deps == null || !deps.isObject()) {
            return;
        }

        schema.remove("dependencies");

        ArrayNode allOf;
        if (schema.has("allOf") && schema.get("allOf").isArray()) {
            allOf = (ArrayNode) schema.get("allOf");
        } else {
            allOf = schema.putArray("allOf");
        }

        deps.fields().forEachRemaining(entry -> {
            String key = entry.getKey();
            JsonNode value = entry.getValue();

            List<JsonNode> requiredArray = new ArrayList<>();
            requiredArray.add(schema.textNode(key));
            if (value.isArray()) {
                for (JsonNode item : value) {
                    requiredArray.add(item);
                }
            } else {
                requiredArray.add(value);
            }
            JsonNode allOfItem = object("oneOf", array(List.of(
                object("not", object(
                    "required", array(List.of(schema.textNode(key)))
                )),
                object("required", array(requiredArray))
            )));

            allOf.add(allOfItem);
        });
    }


    private void rewriteUnsupportedKeywords(JsonNode parent, String parentFieldName, ObjectNode json) {
        if (insideProperties(parent, parentFieldName)) {
            return;
        }
        List<String> toRewrite = new ArrayList<>();
        Iterator<String> fieldNames = json.fieldNames();
        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();
            if (!supportedKeywords.contains(fieldName) && !fieldName.startsWith("x-")) {
                toRewrite.add(fieldName);
            }
        }
        toRewrite.forEach(fieldName -> json.set("x-" + fieldName, json.remove(fieldName)));
    }

    private boolean insideProperties(JsonNode parent, String parentFieldName) {
        return parentFieldName.equals("properties") && parent.has("type") && parent.get("type").asText().equals("object");
    }

    private Map<String, String> processDefinitions(String typeName, ObjectNode json, Map<String, Object> definitions) {
        Map<String, String> keyToRef = new HashMap<>();
        DEFINITION_PREFIXES.stream()
            .filter(json::has).map(json::remove)
            .filter(JsonNode::isObject)
            .map(JsonNode::fields).flatMap(it -> stream(() -> it))
            .forEach(definition -> {
                addDefinition(typeName, definition.getKey(), definition.getValue(), definitions, keyToRef);
            });

        return keyToRef;
    }

    private void validateTypes(JsonNode parent, String fieldName, JsonNode typeBlock) {
        JsonNode type = typeBlock.get("type");
        if (type.has("properties") || type.has("type") || type.has("anyOf") || type.has("oneOf")) {
            return;
        }
        if (insideProperties(parent, fieldName)) {
            return;
        }
        if (type.has("$ref")) {
            String ref = type.get("$ref").asText();
            if (DEFINITION_PREFIXES.stream().map(it -> "#/" + it + "/").noneMatch(ref::startsWith)) {
                throw new BusinessException("error.invalid.json.type.ref", "Invalid ref: " + ref,
                    Map.of("json", typeBlock.toString(), "fieldName", fieldName));
            }
            return;
        }
        if (type.isArray()) {
            type.forEach(it -> {
                if (!validTypes.contains(it.asText())) {
                    throw new BusinessException("error.invalid.json.type", "Invalid type: " + it.asText(),
                        Map.of("json", typeBlock.toString(), "fieldName", fieldName));
                }
            });
            return;
        }
        if (!validTypes.contains(type.asText())) {
            throw new BusinessException("error.invalid.json.type", "Invalid type: " + type.asText(),
                Map.of("json", typeBlock.toString(), "fieldName", fieldName));
        }
    }

    private Object convertNullable(JsonNode parent, String fieldName, ObjectNode json) {
        JsonNode type = json.get("type");
        if (type == null) {
            return json;
        }

        validateTypes(parent, fieldName, json);

        if (type.isNull() || type.asText().equals("null")) {
            json.remove("type");
            json.put("nullable", true);
            return json;
        }

        if (type.asText().equals("array") && !json.has("items")) {
            json.putObject("items");
        }

        if (type.isArray()) {
            ArrayNode typeArrayNode = (ArrayNode) type;
            Iterator<JsonNode> elements = typeArrayNode.elements();
            while (elements.hasNext()) {
                var it = elements.next();
                if (it.isNull() || it.asText().equals("null")) {
                    elements.remove();
                    json.put("nullable", true);
                }
            }

            if (typeArrayNode.isEmpty()) {
                json.remove("type");
            } else if (typeArrayNode.size() == 1) {
                json.set("type", typeArrayNode.get(0));
            } else if (typeArrayNode.size() > 1) {
                List<JsonNode> items = IteratorUtils.toList(typeArrayNode.elements());
                var types = items.stream().map(it -> object("type", it)).collect(toList());
                json.set("anyOf", array(types));
                json.remove("type");
            }
        }

        return json;
    }

    private void rewriteConst(ObjectNode jsonNode) {
        if (jsonNode.has("const") && jsonNode.get("const").isValueNode()) {
            JsonNode value = jsonNode.get("const");
            if (value.isNumber()) {
                jsonNode.put("type", "number");
            } else if (value.isTextual()) {
                jsonNode.put("type", "string");
            } else if (value.isBoolean()) {
                jsonNode.put("type", "boolean");
            }
            jsonNode.set("enum", array(List.of(value)));
            jsonNode.remove("const");
        }
    }

    private void rewriteIfThenElse(ObjectNode json) {
        if (json.has("if") && json.has("than")) {
            json.set("oneOf", array(
                List.of(
                    object("allOf", array(List.of(json.get("if"), json.get("then")))),
                    object("allOf", array(List.of(json.get("if"), json.get("else"))))
                )
            ));
        }
    }

    private void rewriteExclusiveMinMax(ObjectNode json) {
        if (json.has("type") && (
            json.get("type").asText().equals("number") || json.get("type").asText().equals("integer")
        )) {
            if (json.has("exclusiveMinimum") && json.get("exclusiveMinimum").isNumber()) {
                json.set("minimum", json.get("exclusiveMinimum"));
                json.put("exclusiveMinimum", true);
            }
            if (json.has("exclusiveMaximum") && json.get("exclusiveMaximum").isNumber()) {
                json.set("maximum", json.get("exclusiveMaximum"));
                json.put("exclusiveMaximum", true);
            }
        }
    }

    private static ArrayNode array(List<? extends JsonNode> items) {
        items = items.stream().filter(Objects::nonNull).collect(toList());
        return instance.arrayNode().addAll(items);
    }

    private static ObjectNode object(String type, JsonNode it) {
        ObjectNode objectNode = instance.objectNode();
        objectNode.set(type, it);
        return objectNode;
    }

    private static void addDefinition(String typeName, String key, Object value, Map<String, Object> definitions, Map<String, String> keyToRef) {
        Object existsDefinition = definitions.get(key);
        if (existsDefinition != null && !existsDefinition.equals(value)) {
            String ref = typeName + key;
            int i = 0;
            while(definitions.containsKey(ref)) {
                i++;
                ref = ref + "_" + i;
            }

            definitions.put(ref, value);
            keyToRef.put(key, ref);
        } else {
            definitions.put(key, value);
            keyToRef.put(key, key);
        }
    }

    @SneakyThrows
    private String writeJson(Object jsonSchema) {
        return objectMapper.writeValueAsString(jsonSchema);
    }

    @SneakyThrows
    private JsonNode readJson(String jsonSchema) {
        return objectMapper.readTree(jsonSchema);
    }

}
