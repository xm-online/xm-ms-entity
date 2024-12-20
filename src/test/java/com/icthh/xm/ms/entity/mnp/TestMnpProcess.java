package com.icthh.xm.ms.entity.mnp;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.icthh.xm.ms.entity.config.tenant.WebappTenantOverrideConfiguration.WIRE_MOCK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
import com.icthh.xm.ms.entity.domain.ext.IdOrKey;
import com.icthh.xm.ms.entity.service.FunctionService;
import com.icthh.xm.ms.entity.service.XmEntityService;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
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
    static KafkaContainer kafkaContainer = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.0.1"))
        ;

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

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private FunctionService functionService;

    @BeforeClass
    public static void startContainer() {
        kafkaContainer.start();
        String bootstrapServers = kafkaContainer.getBootstrapServers();
        System.out.println("Kafka is running at: " + bootstrapServers);
        System.setProperty("spring.kafka.bootstrap-servers", bootstrapServers);
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
    public void testPortOutRequest() {
        mockPortOutCreate();

        HashMap<String, Object> tenantConfig = new HashMap<>(tenantConfigService.getConfig());
        tenantConfig.put("disableCrmNotification", true);
        tenantConfig.put("rtm", Map.of("kafka", kafkaContainer.getBootstrapServers()));
        tenantConfigService.onRefresh(TENANT_CONFIG_YML, new ObjectMapper().writeValueAsString(tenantConfig));

        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        XmEntity portOut = objectMapper.readValue(getClass().getClassLoader().getResourceAsStream("mnp/portOutRequest.json"), XmEntity.class);

        XmEntity result = xmEntityService.save(portOut);
        System.out.println(result);
        assertNotNull(result.getId());
        assertSasTables(result, 2, 1, List.of("NEW", "NEW"), List.of("NEW"));

        mockComparePersonalData();
        functionService.execute("COMPARE-DATA-FROM-CRM", IdOrKey.of(result.getId()), new HashMap<>());
        assertSasTables(result, 2, 1, List.of("NEW", "NEW"), List.of("NEW"));

        mockPortOutAccept();
        functionService.execute("PROCESS-STATUS", new HashMap<>(Map.of(
            "messageId", "9938744f-3f08-440a-9124-6db72d386db1",
            "processId", "d9b25a0c-2817-498f-b51b-4ac759fc6445",
            "statusCode", "0"
            )), "GET");
        assertSasTables(result, 2, 2, List.of("ACCEPTED", "REJECTED"), List.of("NEW", "ACCEPTED"));
    }

    private void mockPortOutAccept() {
        WIRE_MOCK.stubFor(post(urlEqualTo("/api/tasks"))
            .withHeader("Authorization", equalTo("bearer mock-token"))
            .withHeader("Content-Type", equalTo("application/json;charset=UTF-8"))
            .withRequestBody(matchingJsonPath("$.typeKey", equalTo("portOutRequestAccepted")))
            .withRequestBody(matchingJsonPath("$.createdBy", equalTo("user")))
            .withRequestBody(matchingJsonPath("$.channelType", equalTo("QUEUE")))
            .withRequestBody(matchingJsonPath("$.scheduleType", equalTo("ONE_TIME")))
            .withRequestBody(matchingJsonPath("$.targetMs", equalTo("entity")))
            .withRequestBody(matchingJsonPath("$.ttl", equalTo("259200")))
            .withRequestBody(containing("d9b25a0c-2817-498f-b51b-4ac759fc6445")));
    }

    private void assertSasTables(XmEntity result, int expectedNumbers, int expectedHistoryRecords, List<String> states, List<String> statesHistory) {
        List<Map<String, Object>> mnpRequests = jdbcTemplate.queryForList("select * from XM_ENTITY.SAS_MNP_REQUEST where REQUEST_ID = ?", result.getId());
        System.out.println(mnpRequests);
        assertEquals(expectedNumbers, mnpRequests.size());
        assertEquals(states.get(0), mnpRequests.get(0).get("NUMBER_STATUS"));
        assertEquals(states.get(1), mnpRequests.get(1).get("NUMBER_STATUS"));
        List<Map<String, Object>> mnpStatusHistory = jdbcTemplate.queryForList("select * from XM_ENTITY.SAS_MNP_REQUEST_STATE where REQUEST_ID = ? ORDER BY STATUS_DATE ASC", result.getId());
        System.out.println(mnpStatusHistory);
        assertEquals(expectedHistoryRecords, mnpStatusHistory.size());
        int i = 0;
        for (String it : statesHistory) {
            assertEquals(it, mnpStatusHistory.get(i++).get("STATUS"));
        }
    }

    private void mockPortOutCreate() {
        WIRE_MOCK.stubFor(post(urlEqualTo("/oauth/token"))
            .withHeader("Authorization", matching("Basic .*"))
            .withHeader("Content-Type", containing("application/x-www-form-urlencoded"))
            .withRequestBody(equalTo("grant_type=client_credentials"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{ \"access_token\": \"mock-token\", \"token_type\": \"bearer\", \"expires_in\": 3600 }")));
        WIRE_MOCK.stubFor(post(urlEqualTo("/api/tasks"))
            .withHeader("Authorization", equalTo("bearer mock-token"))
            .withHeader("Content-Type", equalTo("application/json;charset=UTF-8"))
            .withRequestBody(matchingJsonPath("$.typeKey", equalTo("portOutRequestNew")))
            .withRequestBody(matchingJsonPath("$.createdBy", equalTo("user")))
            .withRequestBody(matchingJsonPath("$.channelType", equalTo("QUEUE")))
            .withRequestBody(matchingJsonPath("$.scheduleType", equalTo("ONE_TIME")))
            .withRequestBody(matchingJsonPath("$.targetMs", equalTo("entity")))
            .withRequestBody(matchingJsonPath("$.ttl", equalTo("259200")))
            .withRequestBody(containing("d9b25a0c-2817-498f-b51b-4ac759fc6445"))
            .willReturn(aResponse().withStatus(201)));
        WIRE_MOCK.stubFor(post(urlEqualTo("/api/communicationManagement/v2/communicationMessage/send"))
            .willReturn(aResponse().withStatus(200)));

    }

    private void mockComparePersonalData() {
        WIRE_MOCK.stubFor(get(urlEqualTo("/api/v1/customer/380669111111"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{ \"calculationMethodCode\": \"3\" }")));
        WIRE_MOCK.stubFor(get(urlEqualTo("/api/v1/customer/380669111111/blockedServiceList"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("[]")));
        WIRE_MOCK.stubFor(get(urlEqualTo("/api/v1/customer/380669000000"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{ \"calculationMethodCode\": \"2\" }")));
    }

}
