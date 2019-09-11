package com.icthh.xm.ms.entity.service;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.gen.model.Tenant;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;

@Slf4j
@Service
@AllArgsConstructor
public class TenantService {

    private static final String TENANT_KEY_FORMAT_CODE = "xm.xmEntity.tenant.error.tenantKeyFormat";
    private final Validator validator;

    /**
     * Validate tenant key format.
     *
     * @param tenantKey the tenant key
     */
    public void validateTenantKey(String tenantKey) {
        Set<ConstraintViolation<Tenant>> errors = validator.validate(new Tenant().tenantKey(tenantKey));
        if (CollectionUtils.isNotEmpty(errors)) {
            errors.stream().findAny().map(tenantConstraintViolation -> {
                throw new BusinessException(TENANT_KEY_FORMAT_CODE, tenantConstraintViolation.getMessage());
            });
        }
    }
}
