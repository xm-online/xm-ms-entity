package com.icthh.xm.ms.entity.service;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.icthh.xm.commons.config.client.repository.TenantConfigRepository;
import com.icthh.xm.commons.config.client.repository.TenantListRepository;
import com.icthh.xm.commons.gen.model.Tenant;
import com.icthh.xm.ms.entity.config.ApplicationProperties;
import com.icthh.xm.ms.entity.config.Constants;
import com.icthh.xm.ms.entity.service.tenant.TenantDatabaseService;
import com.icthh.xm.ms.entity.service.tenant.TenantElasticService;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Slf4j
@Service
@AllArgsConstructor
public class TenantService {

    private final TenantDatabaseService tenantDatabaseService;
    private final TenantElasticService tenantElasticService;
    private final TenantListRepository tenantListRepository;
    private final TenantConfigRepository tenantConfigRepository;
    private final ApplicationProperties applicationProperties;

    @Qualifier("webappTenantConfigRepository")
    private final TenantConfigRepository webappTenantConfigRepository;

    public void addTenant(Tenant tenant) {
        StopWatch stopWatch = StopWatch.createStarted();
        log.info("START - SETUP:CreateTenant: tenantKey: {}", tenant);

        try {
            String tenantName = tenant.getTenantKey().toUpperCase();
            tenantListRepository.addTenant(tenantName);
            tenantDatabaseService.create(tenant);
            tenantElasticService.create(tenant);
            addEntitySpecification(tenantName);
            addWebAppSpecification(tenantName);
            log.info("STOP  - SETUP:CreateTenant: tenantKey: {}, result: OK, time = {} ms",
                tenant, stopWatch.getTime());
        } catch (Exception e) {
            log.info("STOP  - SETUP:CreateTenant: tenantKey: {}, result: FAIL, error: {}, time = {} ms",
                tenant, e.getMessage(), stopWatch.getTime());
            throw e;
        }
    }

    @SneakyThrows
    private void addEntitySpecification(String tenantName) {
        String specificationName = applicationProperties.getSpecificationName();
        InputStream in = new ClassPathResource(Constants.ENTITY_CONFIG_PATH).getInputStream();
        String specification = IOUtils.toString(in, UTF_8);
        tenantConfigRepository.updateConfig(tenantName, "/" + specificationName, specification);
    }

    @SneakyThrows
    private void addWebAppSpecification(String tenantName) {
        String specificationName = applicationProperties.getSpecificationWebappName();
        InputStream in = new ClassPathResource(Constants.WEBAPP_CONFIG_PATH).getInputStream();
        String specification = IOUtils.toString(in, UTF_8);
        webappTenantConfigRepository.updateConfig(tenantName, "/" + specificationName, specification);
    }

    public void deleteTenant(String tenantKey) {
        StopWatch stopWatch = StopWatch.createStarted();
        log.info("START - SETUP:DeleteTenant: tenantKey: {}", tenantKey);

        try {
            tenantDatabaseService.drop(tenantKey);
            tenantElasticService.delete(tenantKey);
            tenantListRepository.deleteTenant(tenantKey.toLowerCase());

            String specificationName = applicationProperties.getSpecificationName();
            tenantConfigRepository.deleteConfig(tenantKey.toUpperCase(), "/" + specificationName);

            String webappSpecificationName = applicationProperties.getSpecificationWebappName();
            webappTenantConfigRepository.deleteConfig(tenantKey.toUpperCase(), "/" + webappSpecificationName);

            log.info("STOP  - SETUP:DeleteTenant: tenantKey: {}, result: OK, time = {} ms",
                tenantKey, stopWatch.getTime());
        } catch (Exception e) {
            log.info("STOP  - SETUP:DeleteTenant: tenantKey: {}, result: FAIL, error: {}, time = {} ms",
                tenantKey, e.getMessage(), stopWatch.getTime());
            throw e;
        }
    }

    public void manageTenant(String tenant, String state) {
        StopWatch stopWatch = StopWatch.createStarted();
        log.info("START - SETUP:ManageTenant: tenantKey: {}, state: {}", tenant, state);

        try {
            tenantListRepository.updateTenant(tenant.toLowerCase(), state.toUpperCase());

            log.info("STOP  - SETUP:ManageTenant: tenantKey: {}, state: {}, result: OK, time = {} ms",
                tenant, state, stopWatch.getTime());
        } catch (Exception e) {
            log.info("STOP  - SETUP:ManageTenant: tenantKey: {}, state: {}, result: FAIL, error: {}, time = {} ms",
                tenant, state, e.getMessage(), stopWatch.getTime());
            throw e;
        }
    }


}
