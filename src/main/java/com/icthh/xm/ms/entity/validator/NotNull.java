package com.icthh.xm.ms.entity.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(
    validatedBy = XmEntityFieldNotNullValidator.class
)
@Repeatable(XmEntityFieldNotNulls.class)
public @interface NotNull {

    NotNullBySpecField field();

    String message() default "{jakarta.validation.constraints.NotNull.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
