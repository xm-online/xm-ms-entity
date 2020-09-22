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
@Constraint(validatedBy = EventDataTypeKeyValidator.class)
public @interface EventDataTypeKey {

    /**
     * The message that will be showed when the input data is not valid.
     * @return the message
     */
    String message() default "{xm.ms.entity.event.dataTypekey.constraint}";

    /**
     * The validation groups, to which this constraint belongs.
     * @return the validation groups
     */
    Class<?>[] groups() default {};

    /**
     * The custom payload object.
     * @return the payload object
     */
    Class<? extends Payload>[] payload() default {};
}
