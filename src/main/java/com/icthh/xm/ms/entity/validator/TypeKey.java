package com.icthh.xm.ms.entity.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target({ ElementType.TYPE, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = TypeKeyValidator.class)
public @interface TypeKey {

    String typeKeyField() default "typeKey";

    String entityField() default "xmEntity";

    String message() default "{xm.ms.entity.typekey.constraint}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
