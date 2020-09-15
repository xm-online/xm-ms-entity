package com.icthh.xm.ms.entity.config;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.icthh.xm.commons.tenant.TenantContext;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantKey;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class IndexConfigurationTest {

    @Mock
    private TenantContextHolder tenantContextHolder;

    private IndexConfiguration indexConfiguration;

    @Mock
    TenantContext context;

    @Before
    public void init(){
        when(context.getTenantKey()).thenReturn(Optional.of(new TenantKey("TEST_TENANT")));
        when(tenantContextHolder.getContext()).thenReturn(context);
        indexConfiguration = new IndexConfiguration(tenantContextHolder, "entity");
    }

    @Test
    public void onRefresh() {
        indexConfiguration.onRefresh("/config/tenants/TEST_TENANT/entity/index_config.json", "config");
        indexConfiguration.onRefresh("/config/tenants/TEST_TENANT/entity/indices/index_config_testIndexName.json", "configByIndex");

        assertEquals(indexConfiguration.getMapping(), "config");
        assertEquals(indexConfiguration.getMapping("testIndexName"), "configByIndex");
    }
}
