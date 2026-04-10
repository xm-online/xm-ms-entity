package com.icthh.xm.ms.entity.config;

import com.icthh.xm.commons.config.client.api.ConfigService;
import com.icthh.xm.commons.config.client.repository.CommonConfigRepository;
import com.icthh.xm.commons.domain.FunctionSpecWithFileName;
import com.icthh.xm.commons.service.FunctionManageService;
import com.icthh.xm.ms.entity.security.access.XmEntityDynamicPermissionCheckService;
import com.icthh.xm.ms.entity.service.XmEntityFunctionManagementService;
import com.icthh.xm.ms.entity.service.XmEntitySpecService;
import com.icthh.xm.ms.entity.service.impl.XmEntityFunctionServiceImpl;
import com.icthh.xm.ms.entity.service.json.JsonValidationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class FunctionConfiguration {

    @Bean
    public FunctionManageService<?, ? extends FunctionSpecWithFileName<?>> functionManageService(XmEntitySpecService xmEntitySpecService,
                                                                                                 CommonConfigRepository commonConfigRepository,
                                                                                                 ConfigService configService) {
        return new XmEntityFunctionManagementService(
            xmEntitySpecService, commonConfigRepository, configService
        );
    }

    @Bean
    @Primary
    public XmEntityFunctionServiceImpl functionService(XmEntityDynamicPermissionCheckService dynamicPermissionCheckService,
                                                       XmEntitySpecService xmEntitySpecService,
                                                       XmEntityTenantConfigService xmEntityTenantConfigService,
                                                       JsonValidationService jsonValidationService) {
        return new XmEntityFunctionServiceImpl(
            dynamicPermissionCheckService,
            xmEntitySpecService,
            xmEntityTenantConfigService,
            jsonValidationService
        );
    }
}
