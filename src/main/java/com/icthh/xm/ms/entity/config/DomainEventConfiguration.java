package com.icthh.xm.ms.entity.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "application.domain-event")
public class DomainEventConfiguration {
    private boolean enabled;
}
