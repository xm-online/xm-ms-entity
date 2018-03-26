package com.icthh.xm.ms.entity.web.rest;

import com.icthh.xm.commons.gen.api.TenantsApiDelegate;
import com.icthh.xm.commons.gen.model.Tenant;
import com.icthh.xm.ms.entity.service.TenantService;
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

    private final TenantService tenantService;

    @Override
    @PreAuthorize("hasPermission({'tenant':#tenant}, 'ENTITY.TENANT.CREATE')")
    public ResponseEntity<Void> addTenant(Tenant tenant) {
        tenantService.addTenant(tenant);
        return ResponseEntity.ok().build();
    }

    @Override
    @PreAuthorize("hasPermission({'tenantKey':#tenantKey}, 'ENTITY.TENANT.DELETE')")
    public ResponseEntity<Void> deleteTenant(String tenantKey) {
        tenantService.deleteTenant(tenantKey);
        return ResponseEntity.ok().build();
    }

    @Override
    @PostAuthorize("hasPermission(null, 'ENTITY.TENANT.GET_LIST')")
    public ResponseEntity<List<com.icthh.xm.commons.gen.model.Tenant>> getAllTenantInfo() {
        return null;
    }

    @Override
    @PostAuthorize("hasPermission({'returnObject': returnObject.body}, 'ENTITY.TENANT.GET_LIST.ITEM')")
    public ResponseEntity<com.icthh.xm.commons.gen.model.Tenant> getTenant(String s) {
        return null;
    }

    @Override
    @PreAuthorize("hasPermission({'tenant':#tenant, 'status':#status}, 'ENTITY.TENANT.UPDATE')")
    public ResponseEntity<Void> manageTenant(String tenant, String status) {
        tenantService.manageTenant(tenant, status);
        return ResponseEntity.ok().build();
    }
}
