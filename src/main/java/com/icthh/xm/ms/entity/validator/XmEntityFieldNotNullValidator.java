package com.icthh.xm.ms.entity.validator;

import static java.lang.Boolean.TRUE;
import static org.apache.commons.lang3.StringUtils.capitalize;

import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import com.icthh.xm.ms.entity.service.XmEntitySpecService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.lang.reflect.Field;

@Slf4j
public class XmEntityFieldNotNullValidator implements ConstraintValidator<NotNull, XmEntity> {

    @Autowired
    private XmEntitySpecService xmEntitySpecService;

    private NotNullBySpecField field;

    @Override
    public void initialize(NotNull notNull) {
        field = notNull.field();
    }

    @Override
    @SneakyThrows
    public boolean isValid(XmEntity xmEntity, ConstraintValidatorContext ctx) {
        TypeSpec typeSpec = xmEntitySpecService.getTypeSpecByKeyWithoutFunctionFilter(xmEntity.getTypeKey()).orElse(null);

        if (typeSpec == null) {
            return true;
        }

        Boolean isFieldRequired = field.isRequiredExtractor.apply(typeSpec);
        Object fieldValue = field.valueExtractor.apply(xmEntity);
        if (TRUE.equals(isFieldRequired) && fieldValue == null) {
            ctx.disableDefaultConstraintViolation();
            ctx.buildConstraintViolationWithTemplate("{javax.validation.constraints.NotNull.message}")
                .addPropertyNode(field.fieldName)
                .addConstraintViolation();
            return false;
        }
        return true;
    }

}
