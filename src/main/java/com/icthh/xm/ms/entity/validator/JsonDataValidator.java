package com.icthh.xm.ms.entity.validator;

import static com.github.fge.jsonschema.core.report.LogLevel.ERROR;
import static com.icthh.xm.ms.entity.config.Constants.REGEX_EOL;
import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;
import static org.apache.commons.collections.MapUtils.isEmpty;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import com.icthh.xm.ms.entity.service.json.JsonValidationService;
import com.icthh.xm.ms.entity.service.XmEntitySpecService;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

@Slf4j
@RequiredArgsConstructor
public class JsonDataValidator implements ConstraintValidator<JsonData, XmEntity> {

    private final XmEntitySpecService xmEntitySpecService;
    private final ObjectMapper objectMapper;
    private final JsonValidationService jsonValidationService;

    @Override
    public void initialize(JsonData constraintAnnotation) {
        log.trace("Json data validator inited");
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public boolean isValid(XmEntity value, ConstraintValidatorContext context) {
        TypeSpec typeSpecification = xmEntitySpecService.getTypeSpecByKeyWithoutFunctionFilter(value.getTypeKey()).orElse(null);
        JsonSchema jsonSchema = xmEntitySpecService.getDataJsonSchemaByKey(value.getTypeKey()).orElse(null);

        if (!present(typeSpecification) || dataAndSpecificationEmpty(value, jsonSchema)) {
            return true;
        }

        if (dataWithoutSpecification(value, typeSpecification)) {
            log.error("Data specification null, but data is not null: {}", value.getData());
            return false;
        }

        return validate(value, jsonSchema, context);
    }

    private static boolean present(Object object) {
        return object != null;
    }

    private static boolean dataWithoutSpecification(XmEntity value, TypeSpec typeSpec) {
        return typeSpec.getDataSpec() == null && !isEmpty(value.getData());
    }

    private static boolean dataAndSpecificationEmpty(XmEntity value, JsonSchema jsonSchema) {
        return isEmpty(value.getData()) && jsonSchema == null;
    }

    @SneakyThrows
    private boolean validate(XmEntity value, JsonSchema jsonSchema, ConstraintValidatorContext context) {

        final ProcessingReport report = jsonValidationService.validateJson(value.getData(), jsonSchema);
        boolean isSuccess = report.isSuccess();
        if (!isSuccess) {
            log.error("Validation data report for entity with typeKey {} and id {}: {}",
                    value.getTypeKey(), value.getId(), report.toString().replaceAll(REGEX_EOL, " | "));
            context.disableDefaultConstraintViolation();

            List<?> message = stream(report.spliterator(), false)
                .filter(error -> error.getLogLevel().equals(ERROR)).map(ProcessingMessage::asJson).collect(toList());
            context.buildConstraintViolationWithTemplate(objectMapper.writeValueAsString(message))
                   .addConstraintViolation();
        }
        return isSuccess;
    }

}
