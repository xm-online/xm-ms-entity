package com.icthh.xm.ms.entity.validator;

import com.google.common.collect.Sets;
import com.icthh.xm.ms.entity.config.Constants;
import com.icthh.xm.ms.entity.config.tenant.TenantContext;
import java.util.Set;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;

/**
 * Does not validate fields for ECS tenant.
 */
@Slf4j
public class NotNullTenantAwareValidator implements ConstraintValidator<NotNullTenantAware, Object> {

    @Override
    public void initialize(final NotNullTenantAware constraintAnnotation) {
        log.trace("Json data validator inited");
    }

    @Override
    public boolean isValid(final Object value, final ConstraintValidatorContext context) {
        return value != null;
    }
}
