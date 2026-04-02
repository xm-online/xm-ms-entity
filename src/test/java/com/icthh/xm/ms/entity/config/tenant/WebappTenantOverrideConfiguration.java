package com.icthh.xm.ms.entity.config.tenant;

import com.icthh.xm.commons.config.client.repository.TenantConfigRepository;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import okhttp3.mockwebserver.MockWebServer;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;

import java.util.Collections;

@Configuration(proxyBeanMethods = false)
@Slf4j
public class WebappTenantOverrideConfiguration {

    public static MockWebServer MOCK_WEB_SERVER = new MockWebServer();

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
    public ServiceInstanceListSupplier staticServiceInstanceListSupplier() throws Exception {
        if (MOCK_WEB_SERVER.getPort() == -1) {
            MOCK_WEB_SERVER.start();
        }
        return new ServiceInstanceListSupplier() {
            @Override
            public String getServiceId() {
                return "test-service";
            }

            @Override
            public Flux<java.util.List<ServiceInstance>> get() {
                ServiceInstance instance = new DefaultServiceInstance(
                    "test-service-1",
                    "test-service",
                    "localhost",
                    MOCK_WEB_SERVER.getPort(),
                    false
                );
                return Flux.just(Collections.singletonList(instance));
            }
        };
    }

}
