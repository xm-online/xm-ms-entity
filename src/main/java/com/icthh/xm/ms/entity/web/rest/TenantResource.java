package com.icthh.xm.ms.entity.web.rest;

import com.icthh.xm.commons.gen.api.TenantsApiDelegate;
import com.icthh.xm.commons.gen.model.Tenant;
import com.icthh.xm.ms.entity.service.TenantService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@AllArgsConstructor
@Slf4j
public class TenantResource implements TenantsApiDelegate {

    private final TenantService tenantService;

    @Override
    public ResponseEntity<Void> addTenant(Tenant tenant) {
        tenantService.addTenant(tenant);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> deleteTenant(String tenantKey) {
        tenantService.deleteTenant(tenantKey);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<List<com.icthh.xm.commons.gen.model.Tenant>> getAllTenantInfo() {
        return null;
    }

    @Override
    public ResponseEntity<com.icthh.xm.commons.gen.model.Tenant> getTenant(String s) {
        return null;
    }

    @Override
    public ResponseEntity<Void> manageTenant(String tenant, String status) {
        tenantService.manageTenant(tenant, status);
        return ResponseEntity.ok().build();
    }
}
