package com.icthh.xm.ms.entity.validator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

@Documented
@Target({ ElementType.TYPE, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = EventTypeKeyValidator.class)
public @interface EventTypeKey {

    String message() default "{xm.ms.entity.event.typekey.constraint}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};


}
