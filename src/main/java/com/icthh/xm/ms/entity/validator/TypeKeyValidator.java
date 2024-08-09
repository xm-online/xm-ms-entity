package com.icthh.xm.ms.entity.validator;

import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.service.XmEntitySpecService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.Set;

@Slf4j
public class TypeKeyValidator implements ConstraintValidator<TypeKey, Object> {

    private static final String CLASSNAME_SUFFIX = "Spec";

    private String typeKeyField;

    private String entityField;

    @Autowired
    private XmEntitySpecService xmEntitySpecService;

    @Override
    public void initialize(TypeKey constraintAnnotation) {
        typeKeyField = constraintAnnotation.typeKeyField();
        entityField = constraintAnnotation.entityField();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        try {
            if (value instanceof XmEntity) {
                String typeKey = ((XmEntity)value).getTypeKey();
                return xmEntitySpecService.getTypeSpecByKeyWithoutFunctionFilter(typeKey).isPresent();
            } else {
                String typeKey = (String) FieldUtils.readField(value, typeKeyField, true);
                XmEntity entity = (XmEntity) FieldUtils.readField(value, entityField, true);
                if (entity == null) {
                    return true;
                }
                String entityTypeKey = entity.getTypeKey();
                Map<String, Set<String>> keysByEntityType = xmEntitySpecService.getAllKeys().get(entityTypeKey);
                return !(keysByEntityType == null || keysByEntityType.get(getClassName(value)) == null)
                                && keysByEntityType.get(getClassName(value)).contains(typeKey);
            }
        } catch (IllegalAccessException e) {
            log.debug("Could not get keys for validation", e);
            return false;
        }
    }

    private static String getClassName(Object value) {
        return value.getClass().getSimpleName().concat(CLASSNAME_SUFFIX);
    }

}
