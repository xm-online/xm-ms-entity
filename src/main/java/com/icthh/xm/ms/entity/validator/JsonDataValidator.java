package com.icthh.xm.ms.entity.validator;

import static com.github.fge.jsonschema.core.report.LogLevel.ERROR;
import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;
import static org.apache.commons.collections.MapUtils.isEmpty;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import com.icthh.xm.ms.entity.service.XmEntitySpecService;
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

    @Override
    public void initialize(JsonData constraintAnnotation) {
        log.trace("Json data validator inited");
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public boolean isValid(XmEntity value, ConstraintValidatorContext context) {
        TypeSpec typeSpecification = xmEntitySpecService.findTypeByKey(value.getTypeKey());

        if (!present(typeSpecification) || dataAndSpecificationEmpty(value, typeSpecification)) {
            return true;
        }

        if (dataWithoutSpecification(value, typeSpecification)) {
            log.error("Data specification null, but data is not null: {}", value.getData());
            return false;
        }

        return validate(value.getData(), typeSpecification.getDataSpec(), context);
    }

    private static boolean present(Object object) {
        return object != null;
    }

    private static boolean dataWithoutSpecification(XmEntity value, TypeSpec typeSpec) {
        return typeSpec.getDataSpec() == null && !isEmpty(value.getData());
    }

    private static boolean dataAndSpecificationEmpty(XmEntity value, TypeSpec typeSpec) {
        return isEmpty(value.getData()) && typeSpec.getDataSpec() == null;
    }

    @SneakyThrows
    private boolean validate(Map<String, Object> data, String jsonSchema, ConstraintValidatorContext context) {

        String stringData = objectMapper.writeValueAsString(data);
        log.debug("Validation data. map: {}, jsonData: {}", data, stringData);

        JsonNode schemaNode = JsonLoader.fromString(jsonSchema);

        JsonNode dataNode = JsonLoader.fromString(stringData);
        JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
        JsonSchema schema = factory.getJsonSchema(schemaNode);
        val report = schema.validate(dataNode);

        boolean isSuccess = report.isSuccess();
        if (!isSuccess) {
            log.error("Validation data report: {}", report.toString().replaceAll("\n", " | "));
            context.disableDefaultConstraintViolation();

            List<?> message = stream(report.spliterator(), false)
                .filter(error -> error.getLogLevel().equals(ERROR)).map(ProcessingMessage::asJson).collect(toList());
            context.buildConstraintViolationWithTemplate(objectMapper.writeValueAsString(message))
                   .addConstraintViolation();
        }
        return isSuccess;
    }

}
