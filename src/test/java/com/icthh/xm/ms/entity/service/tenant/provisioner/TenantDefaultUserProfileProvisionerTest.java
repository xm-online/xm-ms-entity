package com.icthh.xm.ms.entity.service.tenant.provisioner;

import static org.mockito.ArgumentMatchers.any;

import com.icthh.xm.commons.gen.model.Tenant;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.internal.DefaultTenantContextHolder;
import com.icthh.xm.ms.entity.AbstractUnitTest;
import com.icthh.xm.ms.entity.domain.Profile;
import com.icthh.xm.ms.entity.service.ProfileService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

public class TenantDefaultUserProfileProvisionerTest extends AbstractUnitTest {

    @Mock
    private ProfileService profileService;
    @Spy
    private TenantContextHolder tenantContextHolder = new DefaultTenantContextHolder();

    private TenantDefaultUserProfileProvisioner provisioner;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        provisioner = new TenantDefaultUserProfileProvisioner(profileService, tenantContextHolder);
    }

    @Test
    public void createTenant() {

        provisioner.createTenant(new Tenant().tenantKey("NEWTENANT"));

        InOrder inOrder = Mockito.inOrder(profileService, tenantContextHolder);

        inOrder.verify(tenantContextHolder).getPrivilegedContext();
        inOrder.verify(profileService).save(any(Profile.class));
        inOrder.verifyNoMoreInteractions();

    }

    @Test
    public void manageTenant() {
        provisioner.manageTenant("NEWTENANT", "ACTIVE");

        Mockito.inOrder(profileService, tenantContextHolder).verifyNoMoreInteractions();
    }

    @Test
    public void deleteTenant() {
        provisioner.deleteTenant("NEWTENANT");

        Mockito.inOrder(profileService, tenantContextHolder).verifyNoMoreInteractions();
    }

}
