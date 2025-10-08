package com.icthh.xm.ms.entity.lep;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.icthh.xm.commons.tenant.TenantContext;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantKey;
import com.icthh.xm.ms.entity.AbstractJupiterUnitTest;
import com.icthh.xm.ms.entity.config.ApplicationProperties;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

@Slf4j
@ExtendWith(MockitoExtension.class)
public class TenantLepResourceUnitTest extends AbstractJupiterUnitTest {

    @Mock
    private TenantContextHolder tenantContextHolder;

    @Mock
    private ApplicationProperties applicationProperties;

    @InjectMocks
    private TenantLepResource tenantLepResource;

    @Test
    public void onRefresh() {
        TenantContext context = mock(TenantContext.class);
        when(context.getTenantKey()).thenReturn(Optional.of(new TenantKey("TEST_TENANT")));
        when(tenantContextHolder.getContext()).thenReturn(context);
        ApplicationProperties.Lep lep = new ApplicationProperties.Lep();
        lep.setLepResourcePathPattern("/config/tenants/{tenantName}/entity/lep/resources/**/*");
        when(applicationProperties.getLep()).thenReturn(lep);

        tenantLepResource.onRefresh("/config/tenants/TEST_TENANT/entity/lep/resources/test-template.ftl", "test1");
        tenantLepResource.onRefresh("/config/tenants/TEST_TENANT/entity/lep/resources/folder/subfolder/test-template.ftl", "test2");

        assertEquals(tenantLepResource.getResource("/test-template.ftl"), "test1");
        assertEquals(tenantLepResource.getResource("/folder/subfolder/test-template.ftl"), "test2");
    }

    @Test
    public void isListeningConfiguration() {
        ApplicationProperties.Lep lep = new ApplicationProperties.Lep();
        lep.setLepResourcePathPattern("/config/tenants/{tenantName}/entity/lep/resources/**/*");
        when(applicationProperties.getLep()).thenReturn(lep);
        assertTrue(tenantLepResource.isListeningConfiguration("/config/tenants/TEST_TENANT/entity/lep/resources/test-template.ftl"));
        assertTrue(tenantLepResource.isListeningConfiguration("/config/tenants/TEST_TENANT/entity/lep/resources/folder/subfolder/test-template.ftl"));
        assertFalse(tenantLepResource.isListeningConfiguration("/config/tenants/UNMATCH/test-template.ftl"));
    }
}
