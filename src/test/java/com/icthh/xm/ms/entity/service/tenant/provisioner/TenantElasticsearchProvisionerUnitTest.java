package com.icthh.xm.ms.entity.service.tenant.provisioner;

import com.icthh.xm.ms.entity.service.search.ElasticsearchTemplateWrapper;
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

        provisioner.createTenant(new Tenant().tenantKey("NEWTENANT"));

        InOrder inOrder = Mockito.inOrder(elasticsearchTemplateWrapper, tenantContextHolder);

        inOrder.verify(tenantContextHolder).getPrivilegedContext();
        inOrder.verify(elasticsearchTemplateWrapper).createIndex(XmEntity.class);
        inOrder.verify(elasticsearchTemplateWrapper).putMapping(XmEntity.class);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void manageTenant() {

        provisioner.manageTenant("NEWTENANT", "ACTIVE");
        verifyZeroInteractions(elasticsearchTemplateWrapper);
        verifyZeroInteractions(tenantContextHolder);

    }

    @Test
    public void deleteTenant() {

        provisioner.deleteTenant("NEWTENANT");

        InOrder inOrder = Mockito.inOrder(elasticsearchTemplateWrapper, tenantContextHolder);

        inOrder.verify(tenantContextHolder).getPrivilegedContext();
        inOrder.verify(elasticsearchTemplateWrapper).deleteIndex(XmEntity.class);
        inOrder.verifyNoMoreInteractions();
    }
}
