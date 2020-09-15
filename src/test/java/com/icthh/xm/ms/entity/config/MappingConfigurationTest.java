package com.icthh.xm.ms.entity.config;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import com.icthh.xm.commons.tenant.TenantContext;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantKey;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class MappingConfigurationTest {

    @Mock
    private TenantContextHolder tenantContextHolder;

    private MappingConfiguration mappingConfiguration;

    @Mock
    TenantContext context;

    @Before
    public void init(){
        when(context.getTenantKey()).thenReturn(Optional.of(new TenantKey("TEST_TENANT")));
        when(tenantContextHolder.getContext()).thenReturn(context);
        mappingConfiguration = new MappingConfiguration(tenantContextHolder, "entity");
    }

    @Test
    public void onRefresh() {
        mappingConfiguration.onRefresh("/config/tenants/TEST_TENANT/entity/mapping.json", "config");
        mappingConfiguration.onRefresh("/config/tenants/TEST_TENANT/entity/mappings/mapping_testMappingName.json", "configByIndex");

        assertEquals(mappingConfiguration.getMapping(), "config");
        assertEquals(mappingConfiguration.getMapping("testMappingName"), "configByIndex");
    }
}
