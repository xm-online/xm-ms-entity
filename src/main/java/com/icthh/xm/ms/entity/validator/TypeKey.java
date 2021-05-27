package com.icthh.xm.ms.entity.validator;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

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
