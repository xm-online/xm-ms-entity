package com.icthh.xm.ms.entity.service.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.logging.LoggingAspectConfig;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class JsonValidationService {

    private final ObjectMapper objectMapper;
    private final JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);

    @LoggingAspectConfig(inputExcludeParams = "schema")
    public Set<ValidationMessage> validateJson(Map<String, Object> data, JsonSchema schema) {
        Set<ValidationMessage> errors = validate(data, schema);
        if (!errors.isEmpty()) {
            log.error("Validation data report: {}", getReportErrorMessage(errors));
        }
        return errors;
    }

    @SneakyThrows
    @LoggingAspectConfig(inputExcludeParams = "jsonSchema")
    public void assertJson(Map<String, Object> data, String jsonSchema) {
        JsonSchema schema = factory.getSchema(jsonSchema);
        Set<ValidationMessage> errors = validate(data, schema);
        if (!errors.isEmpty()) {
            String message = getReportErrorMessage(errors);
            log.error("Validation data report: {}", message);
            throw new InvalidJsonException(message);
        }
    }

    private String getReportErrorMessage(Set<ValidationMessage> report) {
        return report.stream()
            .map(ValidationMessage::getMessage)
            .collect(Collectors.joining(" | "));
    }

    @SneakyThrows
    @LoggingAspectConfig(inputExcludeParams = "jsonSchema")
    private Set<ValidationMessage> validate(Map<String, Object> data, JsonSchema jsonSchema) {
        log.debug("Validation data. map: {}", data);
        JsonNode dataNode = objectMapper.valueToTree(data);
        return jsonSchema.validate(dataNode);
    }

    public static class InvalidJsonException extends BusinessException {
        public InvalidJsonException(String message) {
            super("error.validation.data.spec", message);
        }
    }

}
