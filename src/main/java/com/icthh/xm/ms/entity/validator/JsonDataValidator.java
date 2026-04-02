package com.icthh.xm.ms.entity.validator;

import static org.apache.commons.collections.MapUtils.isEmpty;

import tools.jackson.databind.ObjectMapper;
import com.icthh.xm.ms.entity.domain.EntityBaseFields;
import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import com.icthh.xm.ms.entity.service.json.JsonValidationService;
import com.icthh.xm.ms.entity.service.XmEntitySpecService;

import com.networknt.schema.Schema;
import com.networknt.schema.Error;
import java.util.List;
import java.util.Set;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
@RequiredArgsConstructor
public class JsonDataValidator implements ConstraintValidator<JsonData, EntityBaseFields> {

    private final XmEntitySpecService xmEntitySpecService;
    private final ObjectMapper objectMapper;
    private final JsonValidationService jsonValidationService;

    @Override
    public void initialize(JsonData constraintAnnotation) {
        log.trace("Json data validator inited");
    }

    @Override
    public boolean isValid(EntityBaseFields value, ConstraintValidatorContext context) {
        TypeSpec typeSpecification = xmEntitySpecService.getTypeSpecByKeyWithoutFunctionFilter(value.getTypeKey()).orElse(null);
        Schema jsonSchema = xmEntitySpecService.getDataJsonSchemaByKey(value.getTypeKey()).orElse(null);

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

    private static boolean dataWithoutSpecification(EntityBaseFields value, TypeSpec typeSpec) {
        return typeSpec.getDataSpec() == null && !isEmpty(value.getData());
    }

    private static boolean dataAndSpecificationEmpty(EntityBaseFields value, Schema jsonSchema) {
        return isEmpty(value.getData()) && jsonSchema == null;
    }

    @SneakyThrows
    private boolean validate(EntityBaseFields value, Schema jsonSchema, ConstraintValidatorContext context) {

        final Set<Error> report = jsonValidationService.validateJson(value.getData(), jsonSchema);
        boolean isSuccess = report.isEmpty();
        if (!isSuccess) {
            List<?> message = report.stream().map(Error::getMessage).toList();
            log.error("Validation data report for entity with typeKey {} and id {}: {}",
                    value.getTypeKey(), value.getId(), StringUtils.join(" | ", message));
            context.disableDefaultConstraintViolation();

            context.buildConstraintViolationWithTemplate(objectMapper.writeValueAsString(message))
                   .addConstraintViolation();
        }
        return isSuccess;
    }

}
