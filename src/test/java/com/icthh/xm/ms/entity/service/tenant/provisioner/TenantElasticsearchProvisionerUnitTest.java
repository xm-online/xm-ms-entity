package com.icthh.xm.ms.entity.service.tenant.provisioner;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.icthh.xm.commons.gen.model.Tenant;
import com.icthh.xm.commons.search.ElasticsearchOperations;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.internal.DefaultTenantContextHolder;
import com.icthh.xm.ms.entity.AbstractJupiterUnitTest;
import com.icthh.xm.ms.entity.domain.XmEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

public class TenantElasticsearchProvisionerUnitTest extends AbstractJupiterUnitTest {

    public static final String TENANT_NAME = "NEWTENANT";
    public static final String INDEX_NAME = TENANT_NAME.toLowerCase() + "_xmentity";

    @Mock
    private ElasticsearchOperations elasticsearchOperations;
    @Spy
    private TenantContextHolder tenantContextHolder = new DefaultTenantContextHolder();

    private TenantElasticsearchProvisioner provisioner;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        provisioner = new TenantElasticsearchProvisioner(elasticsearchOperations, tenantContextHolder);

        when(elasticsearchOperations.composeIndexName(TENANT_NAME)).thenReturn(INDEX_NAME);
    }

    @Test
    public void createTenant() {

        provisioner.createTenant(new Tenant().tenantKey(TENANT_NAME));

        InOrder inOrder = Mockito.inOrder(elasticsearchOperations, tenantContextHolder);

        inOrder.verify(tenantContextHolder).getPrivilegedContext();
        inOrder.verify(elasticsearchOperations).composeIndexName(eq(TENANT_NAME));
        inOrder.verify(elasticsearchOperations).createIndex(eq(TENANT_NAME.toLowerCase()+"_xmentity"));
        inOrder.verify(elasticsearchOperations).putMapping(XmEntity.class);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void manageTenant() {

        provisioner.manageTenant(TENANT_NAME, "ACTIVE");

        Mockito.inOrder(elasticsearchOperations, tenantContextHolder).verifyNoMoreInteractions();
    }

    @Test
    public void deleteTenant() {

        provisioner.deleteTenant(TENANT_NAME);

        InOrder inOrder = Mockito.inOrder(elasticsearchOperations, tenantContextHolder);

        inOrder.verify(tenantContextHolder).getPrivilegedContext();
        inOrder.verify(elasticsearchOperations).composeIndexName(eq(TENANT_NAME));
        inOrder.verify(elasticsearchOperations).deleteIndex(eq(TENANT_NAME.toLowerCase()+"_xmentity"));
        inOrder.verifyNoMoreInteractions();
    }
}
