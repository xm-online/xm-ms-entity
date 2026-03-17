package com.icthh.xm.ms.entity.config.tenant;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.icthh.xm.commons.config.client.repository.TenantConfigRepository;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;

@Configuration
public class WebappTenantOverrideConfiguration {

    public static WireMockRule WIRE_MOCK = new WireMockRule();

    @Bean
    @Qualifier("webappTenantConfigRepository")
    public TenantConfigRepository webappTenantConfigRepository() {
        TenantConfigRepository repository = Mockito.mock(TenantConfigRepository.class);
        Mockito.doNothing().when(repository).updateConfig(Mockito.anyString(),
                                                          Mockito.anyString(),
                                                          Mockito.anyString());
        return repository;
    }

    @Bean
    public ServiceInstanceListSupplier staticServiceInstanceListSupplier(ConfigurableApplicationContext context) {
        WIRE_MOCK.start();
        return new ServiceInstanceListSupplier() {
            @NotNull
            @Override
            public String getServiceId() {
                return "test-service";
            }

            @Override
            public Flux<List<ServiceInstance>> get() {
                return Flux.just(java.util.List.of(
                    new DefaultServiceInstance("test-service-1", "test-service",
                        "localhost", WIRE_MOCK.port(), false)
                ));
            }
        };
    }

}
