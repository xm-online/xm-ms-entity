package com.icthh.xm.ms.entity.service.spec;


import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.module.jsonSchema.JsonSchema;
import tools.jackson.module.jsonSchema.JsonSchemaGenerator;
import tools.jackson.module.jsonSchema.types.ArraySchema;
import tools.jackson.module.jsonSchema.types.ObjectSchema;
import tools.jackson.module.jsonSchema.types.StringSchema;
import com.icthh.xm.ms.entity.domain.spec.XmEntitySpec;
import com.icthh.xm.ms.entity.service.json.JsonSchemaGenerationService;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import java.io.StringWriter;

@Component
public class JsonSchemaGenerationServiceImpl implements JsonSchemaGenerationService {

    @SneakyThrows
    public String generateJsonSchema() {
        ObjectMapper mapper = JsonMapper.builder().build();

        JsonSchemaGenerator schemaGen = new JsonSchemaGenerator(mapper);
        JsonSchema jsonSchema = schemaGen.generateSchema(XmEntitySpec.class);
        rejectAdditionalProperties(jsonSchema);
        StringWriter json = new StringWriter();
        mapper.writerWithDefaultPrettyPrinter().writeValue(json, jsonSchema);
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
