package com.icthh.xm.ms.entity.config;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.icthh.xm.commons.config.client.repository.TenantListRepository;
import java.util.Set;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * Oracle runs Liquibase once per tenant schema, and on Oracle a schema is a user that the platform does not
 * create (see SchemaResolver). The Oracle tests therefore work with the single tenant whose user the container
 * provides, instead of the full mocked tenant list.
 *
 * <p>Restricted to the {@code oracle-test} profile on purpose: {@code EntityApp} declares its own
 * {@code @ComponentScan}, which replaces the Spring Boot one together with its test-type exclude filter, so this
 * class is picked up by the scan of every test context. Without the profile guard the {@code @Primary} mock
 * would shrink the tenant list of the H2 suite to {@code TEST} and no other tenant schema would be created.
 */
@Profile("oracle-test")
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
