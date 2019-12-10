package com.icthh.xm.ms.entity.web.client.tenant;

import com.icthh.xm.commons.gen.model.Tenant;
import java.util.List;

public interface TenantClient {

    String getName();

    void addTenant(Tenant tenant);

    void deleteTenant(String tenantKey);

    List<Tenant> getAllTenantInfo();

    Tenant getTenant(String tenantKey);

    void manageTenant(String tenantKey ,String status);

}
