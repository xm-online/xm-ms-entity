package com.icthh.xm.ms.entity.service.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.commons.exceptions.BusinessException;
import com.networknt.schema.Schema;
import com.networknt.schema.Error;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SchemaRegistryConfig;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.networknt.schema.SpecificationVersion.DRAFT_4;
import static com.networknt.schema.path.PathType.LEGACY;

@Slf4j
@Component
@RequiredArgsConstructor
public class JsonValidationService {

    private final ObjectMapper objectMapper;
    private final SchemaRegistry factory = SchemaRegistry.withDefaultDialect(DRAFT_4,
        builder -> builder.schemaRegistryConfig(SchemaRegistryConfig.builder().pathType(LEGACY).build()));

    public List<Error> validateJson(Map<String, Object> data, Schema schema) {
        List<Error> errors = validate(data, schema);
        if (!errors.isEmpty()) {
            log.error("Validation data report: {}", getReportErrorMessage(errors));
        }
        return errors;
    }

    @SneakyThrows
    public void assertJson(Map<String, Object> data, String jsonSchema) {
        Schema schema = factory.getSchema(jsonSchema);
        List<Error> errors = validate(data, schema);
        if (!errors.isEmpty()) {
            String message = getReportErrorMessage(errors);
            log.error("Validation data report: {}", message);
            throw new InvalidJsonException(message);
        }
    }

    private String getReportErrorMessage(List<Error> report) {
        return report.stream()
            .map(error -> error.getInstanceLocation() + ": " + error.getMessage())
            .collect(Collectors.joining(" | "));
    }

    @SneakyThrows
    private List<Error> validate(Map<String, Object> data, Schema jsonSchema) {
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
