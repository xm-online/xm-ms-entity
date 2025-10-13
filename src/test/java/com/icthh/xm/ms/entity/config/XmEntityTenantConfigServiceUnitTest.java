package com.icthh.xm.ms.entity.config;

import static org.mockito.Mockito.when;

import com.icthh.xm.commons.config.client.config.XmConfigProperties;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.ms.entity.AbstractJupiterUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class XmEntityTenantConfigServiceUnitTest extends AbstractJupiterUnitTest {

    @Mock
    TenantContextHolder tenantContextHolder;
    @Mock
    XmConfigProperties xmConfigProperties;
    @InjectMocks
    XmEntityTenantConfigService service;

    @Test
    public void nullConfigDefaultsTest() {
        when(tenantContextHolder.getTenantKey()).thenReturn("TENANT_KEY");
        service.onRefresh("/config/tenants/TENANT_KEY/tenant-config.yml", "someNotMetterProperty: value");
        assertDefaultValues();

        service.onRefresh("/config/tenants/TENANT_KEY/tenant-config.yml", "entity-functions: null");
        assertDefaultValues();

        service.onRefresh("/config/tenants/TENANT_KEY/tenant-config.yml", "entity-functions:\n  dynamicPermissionCheckEnabled: true");
        Assertions.assertTrue(service.getXmEntityTenantConfig().getEntityFunctions().getDynamicPermissionCheckEnabled());
    }

    private void assertDefaultValues() {
        Assertions.assertFalse(service.getXmEntityTenantConfig().getEntityFunctions().getDynamicPermissionCheckEnabled());
        Assertions.assertFalse(service.getXmEntityTenantConfig().getEntityVersionControl().getEnabled());
        Assertions.assertTrue(service.getXmEntityTenantConfig().getMailSettings().isEmpty());
        Assertions.assertFalse(service.getXmEntityTenantConfig().getLep().getEnableInheritanceTypeKey());
    }

}
