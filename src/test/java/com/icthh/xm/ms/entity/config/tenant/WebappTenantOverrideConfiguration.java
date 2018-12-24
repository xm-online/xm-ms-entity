package com.icthh.xm.ms.entity.config.tenant;

import com.icthh.xm.commons.config.client.repository.TenantConfigRepository;
import com.icthh.xm.ms.entity.domain.listener.handler.XmEntityPersistenceEventHandler;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class WebappTenantOverrideConfiguration {

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
    @Primary
    public XmEntityPersistenceEventHandler mockXmEntityPersistenceEventHandler() {
        return Mockito.mock(XmEntityPersistenceEventHandler.class);
    }
}
