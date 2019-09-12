package com.icthh.xm.ms.entity.config.tenant;

import static com.icthh.xm.commons.config.domain.Configuration.of;
import static com.icthh.xm.commons.tenantendpoint.provisioner.TenantConfigProvisioner.prependTenantPath;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.icthh.xm.commons.config.client.repository.TenantConfigRepository;
import com.icthh.xm.commons.tenantendpoint.TenantManager;
import com.icthh.xm.commons.tenantendpoint.provisioner.TenantConfigProvisioner;
import com.icthh.xm.commons.tenantendpoint.provisioner.TenantDatabaseProvisioner;
import com.icthh.xm.commons.tenantendpoint.provisioner.TenantListProvisioner;
import com.icthh.xm.ms.entity.config.ApplicationProperties;
import com.icthh.xm.ms.entity.config.Constants;
import com.icthh.xm.ms.entity.service.tenant.provisioner.TenantAbilityCheckerProvisioner;
import com.icthh.xm.ms.entity.service.tenant.provisioner.TenantDefaultUserProfileProvisioner;
import com.icthh.xm.ms.entity.service.tenant.provisioner.TenantElasticsearchProvisioner;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;

import java.nio.file.Paths;

@Slf4j
@org.springframework.context.annotation.Configuration
public class TenantManagerConfiguration {

    @Value("${spring.application.name}")
    private String applicationName;

    @Bean
    public TenantManager tenantManager(TenantAbilityCheckerProvisioner abilityCheckerProvisioner,
                                       TenantDatabaseProvisioner databaseProvisioner,
                                       TenantDefaultUserProfileProvisioner profileProvisioner,
                                       TenantConfigProvisioner configProvisioner,
                                       TenantListProvisioner tenantListProvisioner,
                                       TenantElasticsearchProvisioner elasticsearchProvisioner) {

        TenantManager manager = TenantManager.builder()
                                             .service(abilityCheckerProvisioner) // should be first in the sequence!
                                             .service(tenantListProvisioner)
                                             .service(databaseProvisioner)
                                             .service(configProvisioner)
                                             .service(elasticsearchProvisioner)
                                             .service(profileProvisioner) // should be after config provisioning
                                             .build();
        log.info("Configured tenant manager: {}", manager);
        return manager;
    }

    @SneakyThrows
    @Bean
    public TenantConfigProvisioner tenantConfigProvisioner(TenantConfigRepository tenantConfigRepository,
                                                           ApplicationProperties applicationProperties) {

        TenantConfigProvisioner provisioner = TenantConfigProvisioner
            .builder()
            .tenantConfigRepository(tenantConfigRepository)
            .configuration(of().path(toFullPath(applicationProperties.getSpecificationName()))
                               .content(readResource(Constants.ENTITY_CONFIG_PATH))
                               .build())
            .configuration(of().path(toFullPath(applicationProperties.getWebappName(),
                                                applicationProperties.getSpecificationWebappName()))
                               .content(readResource(Constants.WEBAPP_CONFIG_PATH))
                               .build())
            .build();

        log.info("Configured tenant config provisioner: {}", provisioner);
        return provisioner;
    }

    @SneakyThrows
    private String readResource(String location) {
        return IOUtils.toString(new ClassPathResource(location).getInputStream(), UTF_8);
    }

    private String toFullPath(String path) {
        return toFullPath(applicationName, path);
    }

    private String toFullPath(String appName, String path) {
        return prependTenantPath(Paths.get(appName, path).toString());
    }

}
