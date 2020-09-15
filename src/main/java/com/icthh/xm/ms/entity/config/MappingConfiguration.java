package com.icthh.xm.ms.entity.config;

import com.icthh.xm.commons.tenant.TenantContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MappingConfiguration extends ElasticMappingConfiguration {


    public MappingConfiguration(TenantContextHolder tenantContextHolder,
                                @Value("${spring.application.name}") String appName) {
        super(tenantContextHolder,
            "/config/tenants/{tenantName}/" + appName + "/mapping.json",
            "/config/tenants/{tenantName}/" + appName + "/mappings/mapping_{index}.json");
    }


}
