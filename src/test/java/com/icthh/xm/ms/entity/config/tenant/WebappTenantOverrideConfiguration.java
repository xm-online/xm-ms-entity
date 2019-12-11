package com.icthh.xm.ms.entity.config.tenant;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.icthh.xm.commons.config.client.repository.TenantConfigRepository;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerList;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.netflix.ribbon.StaticServerList;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
    public ServerList<Server> ribbonServerList() {
        WIRE_MOCK.start();
        return new StaticServerList<>(new Server("localhost", WIRE_MOCK.port()));
    }

}
