package com.icthh.xm.ms.entity.config;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.icthh.xm.commons.config.client.repository.TenantConfigRepository;
import com.icthh.xm.commons.config.client.repository.TenantListRepository;
import com.icthh.xm.ms.entity.config.tenant.LocalXmEntitySpecService;
import com.icthh.xm.ms.entity.config.tenant.TenantContext;
import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import com.icthh.xm.ms.entity.service.XmEntitySpecService;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.util.*;

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
    public XmEntitySpecService xmEntitySpecService(ApplicationProperties applicationProperties) {
        XmEntitySpecService xmEntitySpecService = createXmEntitySpecService(applicationProperties);
        return xmEntitySpecService;
    }

    @Bean
    public TenantListRepository tenantListRepository() {
        TenantListRepository mockTenantListRepository = mock(TenantListRepository.class);
        doAnswer(mvc -> tenants.add(mvc.getArguments()[0].toString())).when(mockTenantListRepository).addTenant(any());
        doAnswer(mvc -> tenants.remove(mvc.getArguments()[0].toString())).when(mockTenantListRepository).deleteTenant(any());
        when(mockTenantListRepository.getTenants()).thenReturn(tenants);
        return  mockTenantListRepository;
    }

    @Bean
    public TenantConfigRepository tenantConfigRepository() {
        TenantConfigRepository tenantConfigRepository = mock(TenantConfigRepository.class);
        return  tenantConfigRepository;
    }

    public static XmEntitySpecService createXmEntitySpecService(ApplicationProperties applicationProperties) {
        return new LocalXmEntitySpecService(mock(TenantConfigRepository.class), applicationProperties);
    }

    @SneakyThrows
    public static String getXmEntitySpec(String tenantName) {
        String configName = format("config/specs/xmentityspec-%s.yml", tenantName.toLowerCase());
        InputStream cfgInputStream = new ClassPathResource(configName).getInputStream();
        return IOUtils.toString(cfgInputStream, UTF_8);
    }

}
