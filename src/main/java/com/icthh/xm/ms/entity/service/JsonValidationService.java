package com.icthh.xm.ms.entity.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.icthh.xm.commons.exceptions.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

import static com.icthh.xm.ms.entity.config.Constants.REGEX_EOL;

@Slf4j
@Service
@RequiredArgsConstructor
public class JsonValidationService {

    private final ObjectMapper objectMapper;

    @SneakyThrows
    public ProcessingReport validateJson(Map<String, Object> data, String jsonSchema) {
        String stringData = objectMapper.writeValueAsString(data);
        log.debug("Validation data. map: {}, jsonData: {}", data, stringData);

        JsonNode schemaNode = JsonLoader.fromString(jsonSchema);
        JsonNode dataNode = JsonLoader.fromString(stringData);
        JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
        JsonSchema schema = factory.getJsonSchema(schemaNode);
        ProcessingReport report = schema.validate(dataNode);
        if (!report.isSuccess()) {
            log.error("Validation data report: {}", report.toString().replaceAll(REGEX_EOL, " | "));
        }
        return report;
    }

    @SneakyThrows
    public ProcessingReport assertJson(Map<String, Object> data, String jsonSchema) {
        String stringData = objectMapper.writeValueAsString(data);
        log.debug("Validation data. map: {}, jsonData: {}", data, stringData);

        JsonNode schemaNode = JsonLoader.fromString(jsonSchema);
        JsonNode dataNode = JsonLoader.fromString(stringData);
        JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
        JsonSchema schema = factory.getJsonSchema(schemaNode);
        ProcessingReport report = schema.validate(dataNode);
        if (!report.isSuccess()) {
            String message = report.toString().replaceAll(REGEX_EOL, " | ");
            log.error("Validation data report: {}", message);
            throw new InvalidJsonException(message);
        }
        return report;
    }

    public static class InvalidJsonException extends BusinessException {
        public InvalidJsonException(String message) {
            super("error.validation.data.spec", message);
        }
    }

}
