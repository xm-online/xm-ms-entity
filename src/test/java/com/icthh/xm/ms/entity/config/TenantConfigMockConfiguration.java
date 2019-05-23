package com.icthh.xm.ms.entity.config;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.icthh.xm.commons.config.client.config.XmConfigProperties;
import com.icthh.xm.commons.config.client.repository.CommonConfigRepository;
import com.icthh.xm.commons.config.client.repository.TenantConfigRepository;
import com.icthh.xm.commons.config.client.repository.TenantListRepository;
import com.icthh.xm.commons.config.client.service.TenantConfigService;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.web.spring.TenantVerifyInterceptor;
import com.icthh.xm.ms.entity.config.tenant.LocalXmEntitySpecService;
import com.icthh.xm.ms.entity.security.access.DynamicPermissionCheckService;
import com.icthh.xm.ms.entity.service.EntityCustomPrivilegeService;
import com.icthh.xm.ms.entity.service.XmEntitySpecService;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

@Configuration
public class TenantConfigMockConfiguration {

    private Set<String> tenants = new HashSet<>();

    {
        tenants.add("XM");
        tenants.add("DEMO");
        tenants.add("TEST");
        tenants.add("RESINTTEST");
    }

    @Bean
    public XmEntitySpecService xmEntitySpecService(ApplicationProperties applicationProperties,
                                                   TenantContextHolder tenantContextHolder,
                                                   EntityCustomPrivilegeService entityCustomPrivilegeService,
                                                   DynamicPermissionCheckService dynamicPermissionCheckService) {
        return new LocalXmEntitySpecService(tenantConfigRepository(),
                                            applicationProperties,
                                            tenantContextHolder,
                                            entityCustomPrivilegeService,
                                            dynamicPermissionCheckService);
    }

    @Bean
    public TenantListRepository tenantListRepository() {
        TenantListRepository mockTenantListRepository = mock(TenantListRepository.class);
        doAnswer(mvc -> tenants.add(mvc.getArguments()[0].toString())).when(mockTenantListRepository).addTenant(any());
        doAnswer(mvc -> tenants.remove(mvc.getArguments()[0].toString())).when(mockTenantListRepository).deleteTenant(any());
        when(mockTenantListRepository.getTenants()).thenReturn(tenants);
        return mockTenantListRepository;
    }

    @Bean
    public XmConfigProperties xmConfigProperties() {
        return mock(XmConfigProperties.class);
    }

    @Bean
    public TenantConfigRepository tenantConfigRepository() {
        return mock(TenantConfigRepository.class);
    }

    @Bean
    public CommonConfigRepository commonConfigRepository() {
        return mock(CommonConfigRepository.class);
    }

    @SneakyThrows
    public static String getXmEntitySpec(String tenantName) {
        String configName = format("config/specs/xmentityspec-%s.yml", tenantName.toLowerCase());
        InputStream cfgInputStream = new ClassPathResource(configName).getInputStream();
        return IOUtils.toString(cfgInputStream, UTF_8);
    }

    @SneakyThrows
    public static String getXmEntityTemplatesSpec(String tenantName) {
        String configName = format("config/templates/search-templates-%s.yml", tenantName.toLowerCase());
        InputStream cfgInputStream = new ClassPathResource(configName).getInputStream();
        return IOUtils.toString(cfgInputStream, UTF_8);
    }

    @Bean
    @ConditionalOnMissingBean(TenantConfigService.class)
    public TenantConfigService tenantContigService() {
        return mock(TenantConfigService.class);
    }

    @Bean
    public TenantVerifyInterceptor tenantVerifyInterceptor() {
        return mock(TenantVerifyInterceptor.class);
    }
}
