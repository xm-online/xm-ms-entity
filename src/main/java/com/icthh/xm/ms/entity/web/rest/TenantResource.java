package com.icthh.xm.ms.entity.web.rest;

import com.icthh.xm.commons.gen.api.TenantsApiDelegate;
import com.icthh.xm.commons.gen.model.Tenant;
import com.icthh.xm.commons.permission.annotation.PrivilegeDescription;
import com.icthh.xm.commons.tenantendpoint.TenantManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor
@Component
@Slf4j
public class TenantResource implements TenantsApiDelegate {

    private final TenantManager tenantManager;

    @Override
    @PreAuthorize("hasPermission({'tenant':#tenant}, 'ENTITY.TENANT.CREATE')")
    @PrivilegeDescription("Privilege to create a entity tenant")
    public ResponseEntity<Void> addTenant(Tenant tenant) {
        tenantManager.createTenant(tenant);
        return ResponseEntity.ok().build();
    }

    @Override
    @PreAuthorize("hasPermission({'tenantKey':#tenantKey}, 'ENTITY.TENANT.DELETE')")
    @PrivilegeDescription("Privilege to delete a entity tenant")
    public ResponseEntity<Void> deleteTenant(String tenantKey) {
        tenantManager.deleteTenant(tenantKey);
        return ResponseEntity.ok().build();
    }

    @Override
    @PostAuthorize("hasPermission(null, 'ENTITY.TENANT.GET_LIST')")
    @PrivilegeDescription("Privilege to get all entity tenants")
    public ResponseEntity<List<Tenant>> getAllTenantInfo() {
        return null;
    }

    @Override
    @PostAuthorize("hasPermission({'returnObject': returnObject.body}, 'ENTITY.TENANT.GET_LIST.ITEM')")
    @PrivilegeDescription("Privilege to get entity tenant")
    public ResponseEntity<Tenant> getTenant(String s) {
        return null;
    }

    @Override
    @PreAuthorize("hasPermission({'tenant':#tenant, 'status':#status}, 'ENTITY.TENANT.UPDATE')")
    @PrivilegeDescription("Privilege to update entity tenant")
    public ResponseEntity<Void> manageTenant(String tenant, String status) {
        tenantManager.manageTenant(tenant, status);
        return ResponseEntity.ok().build();
    }
}
