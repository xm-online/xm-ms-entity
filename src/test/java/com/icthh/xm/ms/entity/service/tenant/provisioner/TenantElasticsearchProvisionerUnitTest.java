package com.icthh.xm.ms.entity.service.tenant.provisioner;

import com.icthh.xm.ms.entity.service.search.ElasticsearchTemplateWrapper;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyZeroInteractions;

import com.icthh.xm.commons.gen.model.Tenant;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.internal.DefaultTenantContextHolder;
import com.icthh.xm.ms.entity.AbstractUnitTest;
import com.icthh.xm.ms.entity.domain.XmEntity;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

public class TenantElasticsearchProvisionerUnitTest extends AbstractUnitTest {

    public static final String TENANT_NAME = "NEWTENANT";

    @Mock
    private ElasticsearchTemplateWrapper elasticsearchTemplateWrapper;
    @Spy
    private TenantContextHolder tenantContextHolder = new DefaultTenantContextHolder();

    private TenantElasticsearchProvisioner provisioner;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        provisioner = new TenantElasticsearchProvisioner(elasticsearchTemplateWrapper, tenantContextHolder);
    }

    @Test
    public void createTenant() {

        provisioner.createTenant(new Tenant().tenantKey(TENANT_NAME));

        InOrder inOrder = Mockito.inOrder(elasticsearchTemplateWrapper, tenantContextHolder);

        inOrder.verify(tenantContextHolder).getPrivilegedContext();
        inOrder.verify(elasticsearchTemplateWrapper).createIndex(eq(TENANT_NAME.toLowerCase()+"_xmentity"));
        inOrder.verify(elasticsearchTemplateWrapper).putMapping(XmEntity.class);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void manageTenant() {

        provisioner.manageTenant(TENANT_NAME, "ACTIVE");
        verifyZeroInteractions(elasticsearchTemplateWrapper);
        verifyZeroInteractions(tenantContextHolder);

    }

    @Test
    public void deleteTenant() {

        provisioner.deleteTenant(TENANT_NAME);

        InOrder inOrder = Mockito.inOrder(elasticsearchTemplateWrapper, tenantContextHolder);

        inOrder.verify(tenantContextHolder).getPrivilegedContext();
        inOrder.verify(elasticsearchTemplateWrapper).deleteIndex(eq(TENANT_NAME.toLowerCase()+"_xmentity"));
        inOrder.verifyNoMoreInteractions();
    }
}
