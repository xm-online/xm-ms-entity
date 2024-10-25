package com.icthh.xm.ms.entity.config.tenant;

import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.config.domain.Configuration;
import com.icthh.xm.commons.gen.model.Tenant;
import com.icthh.xm.commons.tenantendpoint.provisioner.TenantProvisioner;
import java.util.List;
import lombok.RequiredArgsConstructor;

/**
 * TenantConfigProvisioner upload config to ms config.
 * TenantDefaultUserProfileProvisioner run before config from xm-ms-config arrived and fail with exception.
 * This class apply spec locally to avoid this exception.
 */
@RequiredArgsConstructor
public class TenantEntitySpecLocalProvisioner implements TenantProvisioner {

    private final Configuration configuration;
    private final List<RefreshableConfiguration> refreshableConfigurations;

    @Override
    public void createTenant(Tenant tenant) {
        refreshableConfigurations.stream()
                                  .filter(it -> it.isListeningConfiguration(configuration.getPath()))
                                  .forEach(it -> it.onRefresh(configuration.getPath(), configuration.getContent()));
    }

    @Override
    public void manageTenant(String tenantKey, String state) {

    }

    @Override
    public void deleteTenant(String tenantKey) {

    }
}
