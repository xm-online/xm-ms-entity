package com.icthh.xm.ms.entity.service.spec;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.JsonSchemaGenerator;
import com.fasterxml.jackson.module.jsonSchema.types.ArraySchema;
import com.fasterxml.jackson.module.jsonSchema.types.ObjectSchema;
import com.fasterxml.jackson.module.jsonSchema.types.StringSchema;
import com.icthh.xm.ms.entity.domain.spec.XmEntitySpec;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import java.io.StringWriter;

@Component
public class JsonSchemaGenerationService {

    @SneakyThrows
    public String generateJsonSchema() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true);

        JsonSchemaGenerator schemaGen = new JsonSchemaGenerator(mapper);
        JsonSchema jsonSchema = schemaGen.generateSchema(XmEntitySpec.class);
        rejectAdditionalProperties(jsonSchema);
        StringWriter json = new StringWriter();
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        mapper.writeValue(json, jsonSchema);
        return json.toString();
    }

    private static void rejectAdditionalProperties(JsonSchema jsonSchema) {
        if (jsonSchema.isObjectSchema()) {
            ObjectSchema objectSchema = jsonSchema.asObjectSchema();
            ObjectSchema.AdditionalProperties additionalProperties = objectSchema.getAdditionalProperties();
            if (additionalProperties instanceof ObjectSchema.SchemaAdditionalProperties) {
                rejectAdditionalProperties(((ObjectSchema.SchemaAdditionalProperties) additionalProperties).getJsonSchema());
            } else {
                for (JsonSchema property : objectSchema.getProperties().values()) {
                    rejectAdditionalProperties(property);
                }
                objectSchema.rejectAdditionalProperties();
            }

            // fix for correct schema validation (i found only string usage, and correct validation required string in this place),
            // but we need keep backward capability
            if ("urn:jsonschema:com:icthh:xm:ms:entity:domain:spec:StateSpec".equals(objectSchema.getId())) {
                objectSchema.getProperties().put("icon", new StringSchema());
                objectSchema.getProperties().put("color", new StringSchema());
            }
        } else if (jsonSchema.isArraySchema()) {
            ArraySchema.Items items = jsonSchema.asArraySchema().getItems();
            if (items.isSingleItems()) {
                rejectAdditionalProperties(items.asSingleItems().getSchema());
            } else if (items.isArrayItems()) {
                for (JsonSchema schema : items.asArrayItems().getJsonSchemas()) {
                    rejectAdditionalProperties(schema);
                }
            }
        }
    }

}
