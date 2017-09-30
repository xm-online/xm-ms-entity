package com.icthh.xm.ms.entity.config.tenant.hibernate;

import com.icthh.xm.ms.entity.config.tenant.TenantContext;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;

@Component
public class CurrentTenantIdentifierResolverImpl implements CurrentTenantIdentifierResolver {

    @Override
    public String resolveCurrentTenantIdentifier() {
        return TenantContext.getCurrent().getTenant();
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }
}
