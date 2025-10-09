package com.icthh.xm.ms.entity.config.tenant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.icthh.xm.commons.config.client.repository.TenantConfigRepository;
import com.icthh.xm.commons.config.domain.Configuration;
import com.icthh.xm.commons.gen.model.Tenant;
import com.icthh.xm.commons.migration.db.tenant.provisioner.TenantDatabaseProvisioner;
import com.icthh.xm.commons.tenantendpoint.TenantManager;
import com.icthh.xm.commons.tenantendpoint.provisioner.TenantAbilityCheckerProvisioner;
import com.icthh.xm.commons.tenantendpoint.provisioner.TenantConfigProvisioner;
import com.icthh.xm.commons.tenantendpoint.provisioner.TenantListProvisioner;
import com.icthh.xm.ms.entity.AbstractJupiterUnitTest;
import com.icthh.xm.ms.entity.config.ApplicationProperties;
import com.icthh.xm.ms.entity.service.tenant.provisioner.TenantDefaultUserProfileProvisioner;
import com.icthh.xm.ms.entity.service.tenant.provisioner.TenantElasticsearchProvisioner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

@ExtendWith(MockitoExtension.class)
public class TenantManagerConfigurationUnitTest extends AbstractJupiterUnitTest {

    private TenantManager tenantManager;

    private TenantConfigProvisioner configProvisioner;

    @Spy
    private TenantManagerConfiguration configuration = new TenantManagerConfiguration();

    @Mock
    private TenantAbilityCheckerProvisioner abilityCheckerProvisioner;
    @Mock
    private TenantDatabaseProvisioner databaseProvisioner;
    @Mock
    private TenantEntitySpecLocalProvisioner entitySpecLocalProvisioner;
    @Mock
    private TenantDefaultUserProfileProvisioner profileProvisioner;
    @Mock
    private TenantListProvisioner tenantListProvisioner;
    @Mock
    private TenantElasticsearchProvisioner elasticsearchProvisioner;
    @Mock
    private TenantConfigRepository tenantConfigRepository;
    @Mock
    private ApplicationProperties applicationProperties;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);

        when(configuration.getApplicationName()).thenReturn("entity");

        when(applicationProperties.getSpecificationName()).thenReturn("specification.yml");
        when(applicationProperties.getWebappName()).thenReturn("webapp");
        when(applicationProperties.getSpecificationWebappName()).thenReturn("public-settings.yml");

        configProvisioner = spy(configuration.tenantConfigProvisioner(tenantConfigRepository, applicationProperties));

        tenantManager = configuration.tenantManager(abilityCheckerProvisioner,
                                                    entitySpecLocalProvisioner,
                                                    databaseProvisioner,
                                                    profileProvisioner,
                                                    configProvisioner,
                                                    tenantListProvisioner,
                                                    elasticsearchProvisioner);
    }

    @Test
    public void testCreateTenantConfigProvisioning() {

        tenantManager.createTenant(new Tenant().tenantKey("newtenant"));

        List<Configuration> configurations = new ArrayList<>();
        configurations.add(Configuration.of().path("/config/tenants/{tenantName}/entity/specification.yml").build());
        configurations.add(Configuration.of().path("/config/tenants/{tenantName}/webapp/public-settings.yml").build());
        configurations.add(Configuration.of().path("/config/tenants/{tenantName}/tenant-config.yml").build());

        verify(tenantConfigRepository).createConfigsFullPath(eq("newtenant"), eq(configurations));

    }

    @Test
    public void testCreateTenantProvisioningOrder() {

        tenantManager.createTenant(new Tenant().tenantKey("newtenant"));

        InOrder inOrder = Mockito.inOrder(abilityCheckerProvisioner,
                                          tenantListProvisioner,
                                          databaseProvisioner,
                                            entitySpecLocalProvisioner,
                                          configProvisioner,
                                          elasticsearchProvisioner,
                                          profileProvisioner);

        inOrder.verify(abilityCheckerProvisioner).createTenant(any(Tenant.class));
        inOrder.verify(tenantListProvisioner).createTenant(any(Tenant.class));
        inOrder.verify(databaseProvisioner).createTenant(any(Tenant.class));
        inOrder.verify(entitySpecLocalProvisioner).createTenant(any(Tenant.class));
        inOrder.verify(configProvisioner).createTenant(any(Tenant.class));
        inOrder.verify(elasticsearchProvisioner).createTenant(any(Tenant.class));
        inOrder.verify(profileProvisioner).createTenant(any(Tenant.class));

        verifyNoMoreInteractions(abilityCheckerProvisioner,
                                 entitySpecLocalProvisioner,
                                 tenantListProvisioner,
                                 databaseProvisioner,
                                 configProvisioner,
                                 elasticsearchProvisioner,
                                 profileProvisioner);
    }

}
