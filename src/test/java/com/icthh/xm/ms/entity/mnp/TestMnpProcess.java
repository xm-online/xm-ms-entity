package com.icthh.xm.ms.entity.mnp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.config.client.service.TenantConfigService;
import com.icthh.xm.commons.lep.XmLepScriptConfigServerResourceLoader;
import com.icthh.xm.commons.lep.api.LepManagementService;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.ms.entity.AbstractSpringBootTest;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.service.XmEntityService;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

@Slf4j
@ContextConfiguration(classes = {
    TestMnpUpdateModeConfiguration.class
})
@ActiveProfiles("standard-spec-path")
@WithMockUser(authorities = {"SUPER-ADMIN"})
public class TestMnpProcess extends AbstractSpringBootTest {

    public static final String TENANT_CONFIG_YML = "/config/tenants/XM/tenant-config.yml";
    static KafkaContainer kafkaContainer = new KafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:7.0.1")
    );

    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Autowired
    private XmAuthenticationContextHolder authContextHolder;

    @Autowired
    private LepManagementService lepManager;

    @MockBean
    private RestTemplate loadBalancedRestTemplateWithTimeout;

    @Autowired
    private List<RefreshableConfiguration> refreshableConfigurations;

    @Autowired
    private XmLepScriptConfigServerResourceLoader loader;

    @Autowired
    private XmEntityService xmEntityService;

    @Autowired
    private TenantConfigService tenantConfigService;

    @BeforeClass
    public static void startContainer() {
        kafkaContainer.start();
        System.setProperty("spring.kafka.bootstrap-servers", kafkaContainer.getBootstrapServers());
    }

    @SneakyThrows
    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        TenantContextUtils.setTenant(tenantContextHolder, "XM");


        var basePath = "/home/ssenko/work/mw-ms-config-repository";

        Collection<File> files = FileUtils.listFiles(new File(basePath + "/config/tenants/XM/entity"), null, true);

        Map<String, String> fileCache = new HashMap<>();
        refreshableConfigurations.stream().filter(it -> it instanceof XmLepScriptConfigServerResourceLoader).forEach(it -> {
            refresh(it, files, fileCache, basePath);
        });
        lepManager.beginThreadContext();
        Collection<File> tenantConfig = List.of(new File(basePath + TENANT_CONFIG_YML));
        refreshableConfigurations.stream().filter(it -> !(it instanceof XmLepScriptConfigServerResourceLoader)).forEach(it -> {
            refresh(it, files, fileCache, basePath);
            refresh(it, tenantConfig, fileCache, basePath);
        });
    }

    private static void refresh(RefreshableConfiguration it, Collection<File> files, Map<String, String> fileCache, String basePath) {
        List<String> updatedPath = new ArrayList<>();
        for (File file : files) {
            if (!file.isDirectory()) {
                String content = fileCache.computeIfAbsent(file.getAbsolutePath(),
                    i -> readFile(file));
                String relativePath = file.getAbsolutePath().replace(basePath, "");
                if (it.isListeningConfiguration(relativePath)) {
                    log.info(relativePath);
                    it.onRefresh(relativePath, content);
                    updatedPath.add(relativePath);
                }
            }
        }
        if (!updatedPath.isEmpty()) {
            it.refreshFinished(updatedPath);
        }
    }

    @SneakyThrows
    private static String readFile(File file) {
        return FileUtils.readFileToString(file, "UTF-8");
    }

    @After
    public void tearDown() {
        lepManager.endThreadContext();
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
    }

    @Test
    @SneakyThrows
    public void testPortingRequest() {
        HashMap<String, Object> tenantConfig = new HashMap<>(tenantConfigService.getConfig());
        tenantConfig.put("disableCrmNotification", true);
        tenantConfigService.onRefresh(TENANT_CONFIG_YML, new ObjectMapper().writeValueAsString(tenantConfig));

        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        XmEntity portOut = objectMapper.readValue(getClass().getClassLoader().getResourceAsStream("mnp/postringRequest.json"), XmEntity.class);
        XmEntity result = xmEntityService.save(portOut);
    }


}
