package com.icthh.xm.ms.entity.service.json;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.logging.LoggingAspectConfig;
import com.networknt.schema.Error;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import java.util.HashSet;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class JsonValidationService {

    private final ObjectMapper objectMapper;
    private final SchemaRegistry factory = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_4);

    @LoggingAspectConfig(inputExcludeParams = "schema")
    public Set<Error> validateJson(Map<String, Object> data, Schema schema) {
        Set<Error> errors = validate(data, schema);
        if (!errors.isEmpty()) {
            log.error("Validation data report: {}", getReportErrorMessage(errors));
        }
        return errors;
    }

    @SneakyThrows
    @LoggingAspectConfig(inputExcludeParams = "jsonSchema")
    public void assertJson(Map<String, Object> data, String jsonSchema) {
        Schema schema = factory.getSchema(jsonSchema);
        Set<Error> errors = validate(data, schema);
        if (!errors.isEmpty()) {
            String message = getReportErrorMessage(errors);
            log.error("Validation data report: {}", message);
            throw new InvalidJsonException(message);
        }
    }

    private String getReportErrorMessage(Set<Error> report) {
        return report.stream()
            .map(Error::toString)
            .collect(Collectors.joining(" | "));
    }

    @SneakyThrows
    @LoggingAspectConfig(inputExcludeParams = "jsonSchema")
    private Set<Error> validate(Map<String, Object> data, Schema jsonSchema) {
        log.debug("Validation data. map: {}", data);
        JsonNode dataNode = objectMapper.valueToTree(data);
        return new HashSet<>(jsonSchema.validate(dataNode));
    }

    public static class InvalidJsonException extends BusinessException {
        public InvalidJsonException(String message) {
            super("error.validation.data.spec", message);
        }
    }

}
