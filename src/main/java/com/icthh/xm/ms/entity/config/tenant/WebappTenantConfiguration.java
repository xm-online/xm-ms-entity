package com.icthh.xm.ms.entity.config.tenant;

import com.icthh.xm.commons.config.client.config.XmConfigProperties;
import com.icthh.xm.commons.config.client.repository.TenantConfigRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class WebappTenantConfiguration {
    @Bean
    @Qualifier("webappTenantConfigRepository")
    public TenantConfigRepository webappTenantConfigRepository(@Qualifier("xm-config-rest-template") RestTemplate restTemplate, @Value("${application.webapp-name}") String applicationName, XmConfigProperties applicationProperties) {
        return new TenantConfigRepository(restTemplate, applicationName, applicationProperties);
    }
}
