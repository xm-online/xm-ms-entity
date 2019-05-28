package com.icthh.xm.ms.entity.config;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import com.icthh.xm.commons.config.client.config.XmConfigProperties;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.ms.entity.AbstractUnitTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class XmEntityTenantConfigServiceUnitTest extends AbstractUnitTest {

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
        assertTrue(service.getXmEntityTenantConfig().getEntityFunctions().getDynamicPermissionCheckEnabled());
    }

    private void assertDefaultValues() {
        assertFalse(service.getXmEntityTenantConfig().getEntityFunctions().getDynamicPermissionCheckEnabled());
        assertFalse(service.getXmEntityTenantConfig().getEntityVersionControl().getEnabled());
        assertTrue(service.getXmEntityTenantConfig().getMailSettings().isEmpty());
        assertFalse(service.getXmEntityTenantConfig().getLep().getEnableInheritanceTypeKey());
    }

}
