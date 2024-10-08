package com.icthh.xm.ms.entity.validator;

import static com.icthh.xm.ms.entity.config.Constants.REGEX_EOL;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.MapUtils.isEmpty;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import com.icthh.xm.ms.entity.service.json.JsonValidationService;
import com.icthh.xm.ms.entity.service.XmEntitySpecService;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.networknt.schema.JsonSchema;
import com.networknt.schema.ValidationMessage;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

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

        final Set<ValidationMessage> report = jsonValidationService.validateJson(value.getData(), jsonSchema);
        boolean isSuccess = report.isEmpty();
        if (!isSuccess) {
            List<?> message = report.stream().map(ValidationMessage::getMessage).toList();
            log.error("Validation data report for entity with typeKey {} and id {}: {}",
                    value.getTypeKey(), value.getId(), StringUtils.join(" | ", message));
            context.disableDefaultConstraintViolation();

            context.buildConstraintViolationWithTemplate(objectMapper.writeValueAsString(message))
                   .addConstraintViolation();
        }
        return isSuccess;
    }

}
