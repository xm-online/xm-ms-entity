package com.icthh.xm.ms.entity.service.swagger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Streams.stream;
import static com.icthh.xm.ms.entity.service.processor.DefinitionSpecProcessor.XM_ENTITY_DEFINITION;
import static com.icthh.xm.ms.entity.service.spec.SpecInheritanceProcessor.XM_ENTITY_INHERITANCE_DEFINITION;

public class SwaggerGenerator {

    public static final String DEFINITIONS = "definitions";
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
        "null",
        "boolean",
        "object",
        "array",
        "number",
        "string",
        "integer"
    );


    public String transformJsonSchemaToSwaggerSchema(String typeName, String jsonSchema, Map<String, Object> definitions) {
        JsonNode json = readJson(jsonSchema);
        if (!json.isObject()) {
            throw new IllegalArgumentException("Json schema should be an object");
        }
        transformJsonSchemaToSwaggerSchema(typeName, (ObjectNode) json, definitions);
        return writeJson(json);
    }

    private void transformJsonSchemaToSwaggerSchema(String typeName, ObjectNode json, Map<String, Object> definitions) {
        processDefinitions(typeName, json, definitions);
    }

    private Map<String, String> processDefinitions(String typeName, ObjectNode json, Map<String, Object> definitions) {
        Map<String, String> keyToRef = new HashMap<>();
        Set.of(DEFINITIONS, XM_ENTITY_DEFINITION, XM_ENTITY_INHERITANCE_DEFINITION).stream()
            .filter(json::has).map(json::remove)
            .filter(JsonNode::isObject).map(ObjectNode.class::cast)
            .map(ObjectNode::fields).flatMap(it -> stream(() -> it))
            .forEach(definition -> {
                addDefinition(typeName, definition.getKey(), definition.getValue(), definitions, keyToRef);
            });

        return keyToRef;
    }

    private void validateTypes(Object typeBlock) {

    }

    private Object convertTypes(Object json) {
        if (!(json instanceof Map)) {
            return json;
        }
        Object type = ((Map<?, ?>) json).get("type");
        if (StringUtils.isNotBlank(type)) {
            JsonNode jsonNode = new ObjectMapper().valueToTree(json);

        }
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
        return new ObjectMapper().writeValueAsString(jsonSchema);
    }

    @SneakyThrows
    private JsonNode readJson(String jsonSchema) {
        return new ObjectMapper().valueToTree(jsonSchema);
    }

    @Data
    @AllArgsConstructor
    public static class SwaggerFunction {
        private String operationId;
        private String path;
        private String name;
        private String inputJsonSchema;
        private String outputJsonSchema;

        /**
         type: string, format: binary =>
         requestBody:
           required: true
           content:
             custom/content-type:
               schema:
                 type: string
                 format: binary
         */
        private String customBinaryDataType;

    }

}
