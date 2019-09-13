package com.icthh.xm.ms.entity.service.tenant.provisioner;

import static com.icthh.xm.ms.entity.config.Constants.TENANT_XM;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.gen.model.Tenant;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.tenantendpoint.provisioner.TenantProvisioner;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TenantAbilityCheckerProvisioner implements TenantProvisioner {

    private final TenantContextHolder tenantContextHolder;

    @Override
    public void createTenant(final Tenant tenant) {
        assertCanManageTenant("create new");
    }

    @Override
    public void manageTenant(final String tenantKey, final String state) {
        assertCanManageTenant("manage");
    }

    @Override
    public void deleteTenant(final String tenantKey) {
        assertCanManageTenant("delete");
    }

    private void assertCanManageTenant(String action) {
        if (!TENANT_XM.equals(TenantContextUtils.getRequiredTenantKeyValue(tenantContextHolder))) {
            throw new BusinessException("Only '" + TENANT_XM + String.format("' tenant allow to %s tenant", action));
        }
    }

}
