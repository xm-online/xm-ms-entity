package com.icthh.xm.ms.entity.service.patch.model;

import lombok.Getter;

import javax.validation.ConstraintViolation;
import javax.validation.ValidationException;
import java.util.Set;


@Getter
public class XmTenantPatchValidationException extends ValidationException {
    private final Set<ConstraintViolation<XmTenantPatch>> constraintViolations;

    public XmTenantPatchValidationException(Set<ConstraintViolation<XmTenantPatch>> constraintViolations) {
        super(constraintViolations.toString());
        this.constraintViolations = constraintViolations;
    }
}
