package com.icthh.xm.ms.entity.validator;

import static java.lang.Boolean.TRUE;

import com.icthh.xm.ms.entity.domain.EntityBaseFields;
import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import com.icthh.xm.ms.entity.service.XmEntitySpecService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;


@Slf4j
public class XmEntityFieldNotNullValidator implements ConstraintValidator<NotNull, EntityBaseFields> {

    @Autowired
    private XmEntitySpecService xmEntitySpecService;

    private NotNullBySpecField field;

    @Override
    public void initialize(NotNull notNull) {
        field = notNull.field();
    }

    @Override
    @SneakyThrows
    public boolean isValid(EntityBaseFields value, ConstraintValidatorContext ctx) {
        String typeKey = value.getTypeKey();
        if (typeKey == null) {
            return true;
        }

        TypeSpec typeSpec = xmEntitySpecService.getTypeSpecByKeyWithoutFunctionFilter(typeKey).orElse(null);
        if (typeSpec == null) {
            return true;
        }

        Boolean isFieldRequired = field.isRequiredExtractor.apply(typeSpec);
        Object fieldValue = field.valueExtractor.apply(value);
        if (TRUE.equals(isFieldRequired) && fieldValue == null) {
            ctx.disableDefaultConstraintViolation();
            ctx.buildConstraintViolationWithTemplate("{jakarta.validation.constraints.NotNull.message}")
                .addPropertyNode(field.fieldName)
                .addConstraintViolation();
            log.error("Field {} is required on entity with typeKey {} and id {}", field.fieldName, typeKey, value.getId());
            return false;
        }
        return true;
    }

}
