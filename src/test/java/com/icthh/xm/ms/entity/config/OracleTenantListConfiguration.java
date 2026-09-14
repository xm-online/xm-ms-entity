package com.icthh.xm.ms.entity.config;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.icthh.xm.commons.config.client.repository.TenantListRepository;
import java.util.Set;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Oracle runs Liquibase once per tenant schema, and on Oracle a schema is a user that the platform does not
 * create (see SchemaResolver). The Oracle tests therefore work with the single tenant whose user the container
 * provides, instead of the full mocked tenant list.
 */
@TestConfiguration
public class OracleTenantListConfiguration {

    @Bean
    @Primary
    public TenantListRepository oracleTenantListRepository() {
        TenantListRepository repository = mock(TenantListRepository.class);
        when(repository.getTenants()).thenReturn(Set.of("TEST"));
        when(repository.getSuspendedTenants()).thenReturn(Set.of());
        return repository;
    }

}
