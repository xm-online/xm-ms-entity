package com.icthh.xm.ms.entity.mnp;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.icthh.xm.ms.entity.config.tenant.WebappTenantOverrideConfiguration.WIRE_MOCK;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.config.client.service.TenantConfigService;
import com.icthh.xm.commons.lep.XmLepScriptConfigServerResourceLoader;
import com.icthh.xm.commons.lep.api.LepManagementService;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.topic.domain.TopicConfig;
import com.icthh.xm.commons.topic.message.MessageService;
import com.icthh.xm.ms.entity.AbstractSpringBootTest;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.ext.IdOrKey;
import com.icthh.xm.ms.entity.service.FunctionService;
import com.icthh.xm.ms.entity.service.XmEntityService;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
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

    @Autowired
    private MessageService messageService;

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
    public void testPortInRequest() {
        mockPortInCreate();

        prepareTenantConfig();

        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        XmEntity portIn = objectMapper.readValue(getClass().getClassLoader().getResourceAsStream("mnp/portInRequest.json"), XmEntity.class);

        XmEntity result = xmEntityService.save(portIn);
        System.out.println(result);
        assertNotNull(result.getId());
        assertSasTables(result, List.of("VERIFICATION-STARTED", "VERIFICATION-STARTED", "VERIFICATION-STARTED"), List.of("VERIFICATION-STARTED"));

        mockLdbCreateRequest(result, objectMapper);

        TopicConfig topicConfig = new TopicConfig();
        topicConfig.setTopicName("pending-portin-requests-queue");
        topicConfig.setTypeKey("pending-portin-requests");
        byte[] value = ("{\"entityId\": " + result.getId() + "}").getBytes(UTF_8);
        messageService.onMessage(Base64.getEncoder().encodeToString(value), topicConfig);
        assertSasTables(result, List.of("VERIFICATION-STARTED", "VERIFICATION-STARTED", "VERIFICATION-STARTED"), List.of("VERIFICATION-STARTED"));

        functionService.execute("PROCESS-STATUS", new HashMap<>(Map.of(
            "messageId", "431834d8-1bc7-40e6-a65f-42ebb157d001",
            "processId", "d9b25a0c-2817-498f-b51b-4ac759fc6445",
            "messageType", "ValidationResponse",
            "portingDate", "2066-11-26T11:00:00Z",
            "statusCode", "0"
        )), "GET");

        assertSasTables(result, List.of("REGISTERED", "REGISTERED", "REGISTERED"),
            List.of("VERIFICATION-STARTED", "REGISTERED"));

        xmEntityService.updateState(IdOrKey.of(result.getId()), "ACCEPTED", Map.of(
            "numbers", List.of(
                Map.of(
                    "msisdn", "380985333333",
                    "statusCode", 406,
                    "statusCodeDescription", "Numbers in the request do not belong to the same customer. Failed to compare: Surname,Name"
                )
            ),
            "messageId", "431834d8-1bc7-40e6-a65f-42ebb157d002",
            "numberBlocks", List.of()
        ));
        assertSasTables(result, List.of("ACCEPTED", "ACCEPTED", "REJECTED"),
            List.of("VERIFICATION-STARTED", "REGISTERED", "ACCEPTED"));

        xmEntityService.updateState(IdOrKey.of(result.getId()), "CONFIRMED", Map.of());
        assertSasTables(result, List.of("ACCEPTED", "ACCEPTED", "REJECTED"),
            List.of("VERIFICATION-STARTED", "REGISTERED", "ACCEPTED"));

        functionService.execute("PROCESS-STATUS", new HashMap<>(Map.of(
            "messageType", "ValidationResponse",
            "statusCode", "0",
            "statusDescription", "OK",
            "messageId", "431834d8-1bc7-40e6-a65f-42ebb157d003",
            "processId", "d9b25a0c-2817-498f-b51b-4ac759fc6445",
            "portingDate", "2024-11-26T11:00:00Z"
        )), "GET");
        assertSasTables(result, List.of("CONFIRMED", "CONFIRMED", "REJECTED"),
            List.of("VERIFICATION-STARTED", "REGISTERED", "ACCEPTED", "CONFIRMED"));

        var entity = xmEntityService.findById(result.getId());

        List.of("380985111111", "380985222222").forEach(number -> assertActivateNumber(entity, number));
        xmEntityService.updateState(IdOrKey.of(result.getId()), "STARTED", Map.of(
            "numbers", List.of(Map.of("msisdn", "380985111111"), Map.of("msisdn", "380985222222")),
            "messageId", "431834d8-1bc7-40e6-a65f-42ebb157d004"
        ));
        assertSasTables(result, List.of("STARTED", "STARTED", "REJECTED"),
            List.of("VERIFICATION-STARTED", "REGISTERED", "ACCEPTED", "CONFIRMED", "STARTED"));

        functionService.execute("NOTIFY-NUMBER-PROCESSED", IdOrKey.of(result.getId()), new HashMap<>(Map.of(
            "msisdn", "380985222222"
        )));
        assertSasTables(result,
            List.of("STARTED", "STARTED", "REJECTED"),
            List.of("VERIFICATION-STARTED", "REGISTERED", "ACCEPTED", "CONFIRMED", "STARTED"));

        functionService.execute("NOTIFY-NUMBER-PROCESSED", IdOrKey.of(result.getId()), new HashMap<>(Map.of(
            "msisdn", "380985111111"
        )));

        functionService.execute("PROCESS-STATUS", new HashMap<>(Map.of(
            "messageId", "431834d8-1bc7-40e6-a65f-42ebb157d005",
            "messageType", "ValidationResponse",
            "processId", "d9b25a0c-2817-498f-b51b-4ac759fc6445",
            "statusCode", "0"
        )), "GET");
        assertSasTables(result,
            List.of("ACTIVATED", "ACTIVATED", "REJECTED"),
            List.of("VERIFICATION-STARTED", "REGISTERED", "ACCEPTED", "CONFIRMED", "STARTED", "ACTIVATED"));


        xmEntityService.updateState(IdOrKey.of(result.getId()), "PORTED", Map.of(
            "msisdn", "380985222222",
            "messageId", "431834d8-1bc7-40e6-a65f-42ebb157d006"
        ));
        assertSasTables(result,
            List.of("ACTIVATED", "ACTIVATED", "REJECTED"),
            List.of("VERIFICATION-STARTED", "REGISTERED", "ACCEPTED", "CONFIRMED", "STARTED", "ACTIVATED"));

        xmEntityService.updateState(IdOrKey.of(result.getId()), "PORTED", Map.of(
            "msisdn", "380985111111",
            "messageId", "431834d8-1bc7-40e6-a65f-42ebb157d007"
        ));
        assertSasTables(result,
            List.of("PORTED", "PORTED", "REJECTED"),
            List.of("VERIFICATION-STARTED", "REGISTERED", "ACCEPTED", "CONFIRMED", "STARTED", "ACTIVATED", "PORTED"));
    }

    @Test
    @SneakyThrows
    public void testPortOutRequest() {
        mockPortOutCreate();

        prepareTenantConfig();

        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        XmEntity portOut = objectMapper.readValue(getClass().getClassLoader().getResourceAsStream("mnp/portOutRequest.json"), XmEntity.class);

        XmEntity result = xmEntityService.save(portOut);
        System.out.println(result);
        assertNotNull(result.getId());
        assertSasTables(result, List.of("NEW", "NEW", "NEW"), List.of("NEW"));

        mockComparePersonalData(List.of("380669222222", "380669111111"));
        functionService.execute("COMPARE-DATA-FROM-CRM", IdOrKey.of(result.getId()), new HashMap<>());
        assertSasTables(result, List.of("NEW", "NEW", "NEW"), List.of("NEW"));

        mockPortOutAccept();
        functionService.execute("PROCESS-STATUS", new HashMap<>(Map.of(
            "messageId", "9938744f-3f08-440a-9124-6db72d386db1",
            "processId", "d9b25a0c-2817-498f-b51b-4ac759fc6445",
            "messageType", "ValidationResponse",
            "statusCode", "0"
            )), "GET");
        assertSasTables(result, List.of("ACCEPTED", "ACCEPTED", "REJECTED"), List.of("NEW", "ACCEPTED"));


        functionService.execute("UPDATE-STATUS", IdOrKey.of(result.getId()), new HashMap<>(Map.of(
            "stateKey", "SERVICE-PORT-OUT-ON"
        )));
        assertSasTables(result,
            List.of("SERVICE-PORT-OUT-ON", "SERVICE-PORT-OUT-ON", "REJECTED"),
            List.of("NEW", "ACCEPTED", "SERVICE-PORT-OUT-ON"));

        mockPortOutStart(result, List.of("380669222222", "380669111111"));
        xmEntityService.updateState(IdOrKey.of(result.getId()), "STARTED", Map.of(
            "numbers", List.of(Map.of("msisdn", "380669111111"), Map.of("msisdn", "380669222222")),
            "messageId", "9938744f-3f08-440a-9124-6db72d386db1"
        ));
        assertSasTables(result,
            List.of("STARTED", "STARTED", "REJECTED"),
            List.of("NEW", "ACCEPTED", "SERVICE-PORT-OUT-ON", "STARTED"));

        functionService.execute("NOTIFY-NUMBER-PROCESSED", IdOrKey.of(result.getId()), new HashMap<>(Map.of(
            "msisdn", "380669222222"
        )));
        assertSasTables(result,
            List.of("STARTED", "STARTED", "REJECTED"),
            List.of("NEW", "ACCEPTED", "SERVICE-PORT-OUT-ON", "STARTED"));

        mockPortOutDeactivated();
        functionService.execute("NOTIFY-NUMBER-PROCESSED", IdOrKey.of(result.getId()), new HashMap<>(Map.of(
            "msisdn", "380669111111"
        )));

        functionService.execute("PROCESS-STATUS", new HashMap<>(Map.of(
            "messageId", "9938744f-3f08-440a-9124-6db72d386db2",
            "messageType", "ValidationResponse",
            "processId", "d9b25a0c-2817-498f-b51b-4ac759fc6445",
            "statusCode", "0"
        )), "GET");

        assertSasTables(result,
            List.of("DEACTIVATED", "DEACTIVATED", "REJECTED"),
            List.of("NEW", "ACCEPTED", "SERVICE-PORT-OUT-ON", "STARTED", "DEACTIVATED"));

        functionService.execute("PROCESS-STATUS", new HashMap<>(Map.of(
            "messageId", "2ea40fa1-867d-4039-bff8-5ad164b1f2e6",
            "processId", "d9b25a0c-2817-498f-b51b-4ac759fc6445",
            "messageType", "ProcessStateChanged",
            "statusCode", "0",
            "portingDate", "2666-12-17T11:00:00Z"
        )), "GET");

        List<Map<String, Object>> mnpRequests = jdbcTemplate.queryForList("select * from XM_ENTITY.SAS_MNP_REQUEST where REQUEST_ID = ?", result.getId());
        mnpRequests.forEach(it -> {
            assertEquals("2666-12-17 13:00:00.0", it.get("PORTING_DATE").toString());
        });

        xmEntityService.updateState(IdOrKey.of(result.getId()), "PORTED", Map.of(
            "msisdn", "380669222222",
            "messageId", "431834d8-1bc7-40e6-a65f-42ebb157d901"
        ));
        assertSasTables(result,
            List.of("DEACTIVATED", "DEACTIVATED", "REJECTED"),
            List.of("NEW", "ACCEPTED", "SERVICE-PORT-OUT-ON", "STARTED", "DEACTIVATED"));

        xmEntityService.updateState(IdOrKey.of(result.getId()), "PORTED", Map.of(
            "msisdn", "380669111111",
            "messageId", "431834d8-1bc7-40e6-a65f-42ebb157d902"
        ));
        assertSasTables(result,
            List.of("PORTED", "PORTED", "REJECTED"),
            List.of("NEW", "ACCEPTED", "SERVICE-PORT-OUT-ON", "STARTED", "DEACTIVATED", "PORTED"));
    }

    private void prepareTenantConfig() throws JsonProcessingException {
        HashMap<String, Object> tenantConfig = new HashMap<>(tenantConfigService.getConfig());
        tenantConfig.put("disableCrmNotification", true);
        Map<String, Map<String, Map<String, Object>>> schedule = new HashMap<>((Map<String, Map<String, Map<String, Object>>>) tenantConfig.get("schedule"));
        Map<String, Map<String, Object>> exceptionScheduler = new HashMap<>(schedule.get("exceptionScheduler"));
        exceptionScheduler.put(LocalDate.now().toString(), Map.of(
            "type", "DAYON",
            "startTime", "00:00",
            "endTime", "23:59"
        ));
        schedule.put("exceptionScheduler", exceptionScheduler);
        tenantConfig.put("schedule", schedule);
        tenantConfig.put("rtm", Map.of("kafka", kafkaContainer.getBootstrapServers()));
        tenantConfigService.onRefresh(TENANT_CONFIG_YML, new ObjectMapper().writeValueAsString(tenantConfig));
    }

    private static void mockLdbCreateRequest(XmEntity result, ObjectMapper objectMapper) throws JsonProcessingException {
        result.getData().put("processId", "d9b25a0c-2817-498f-b51b-4ac759fc6445");
        WIRE_MOCK.stubFor(post(urlEqualTo("/cdb/api/v1/portingRequest"))
            .willReturn(aResponse().withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(objectMapper.writeValueAsString(result))));
    }


    private void mockPortOutDeactivated() {
        WIRE_MOCK.stubFor(put(urlEqualTo("/cdb/api/v1/portingRequest/d9b25a0c-2817-498f-b51b-4ac759fc6445/numbersDeactivated"))
            .willReturn(aResponse().withStatus(200)));
    }

    private void mockPortOutStart(XmEntity request, List<String> numbers) {
        numbers.forEach(number -> assertDeactivateNumber(request, number));
        WIRE_MOCK.stubFor(post(urlEqualTo("/api/communicationManagement/v2/communicationMessage/send"))
            .willReturn(aResponse().withStatus(200)));
    }

    private static void assertActivateNumber(XmEntity request, String number) {
        WIRE_MOCK.stubFor(post(urlEqualTo("/DSEntityProvisioning/api/ActivationAndConfiguration/v2/service"))
            .withRequestBody(matchingJsonPath("$.type", equalTo("MNP-ACTIVATION")))
            .withRequestBody(matchingJsonPath("$.relatedParty[0].id", equalTo(number)))
            .withRequestBody(matchingJsonPath("$.serviceCharacteristic[?(@.name=='key')].value",
                equalToJson("[\"d9b25a0c-2817-498f-b51b-4ac759fc6445-" + number + "\"]")))
            .withRequestBody(matchingJsonPath("$.serviceCharacteristic[?(@.name=='processId')].value",
                equalToJson("[\"d9b25a0c-2817-498f-b51b-4ac759fc6445\"]")))
            .withRequestBody(matchingJsonPath("$.serviceCharacteristic[?(@.name=='entityId')].value",
                equalToJson("[" + request.getId() + "]")))
            .withRequestBody(matchingJsonPath("$.serviceCharacteristic[?(@.name=='donorRn')].value",
                equalToJson("[\"3903\"]")))
            .withRequestBody(matchingJsonPath("$.serviceCharacteristic[?(@.name=='routingNumber')].value",
                equalToJson("[\"3901\"]")))
            .withRequestBody(matchingJsonPath("$.serviceCharacteristic[?(@.name=='channel')].value",
                equalToJson("[\"CRM\"]")))
            .withRequestBody(matchingJsonPath("$.serviceCharacteristic[?(@.name=='dateFrom')].value",
                equalToJson("[\"" + request.getData().get("portingDate").toString() + "\"]")))
            .willReturn(aResponse().withStatus(200)));
    }

    private static void assertDeactivateNumber(XmEntity request, String number) {
        WIRE_MOCK.stubFor(post(urlEqualTo("/DSEntityProvisioning/api/ActivationAndConfiguration/v2/service"))
            .withRequestBody(matchingJsonPath("$.type", equalTo("MNP-DEACTIVATION")))
            .withRequestBody(matchingJsonPath("$.relatedParty[0].id", equalTo(number)))
            .withRequestBody(matchingJsonPath("$.serviceCharacteristic[?(@.name=='key')].value",
                equalToJson("[\"d9b25a0c-2817-498f-b51b-4ac759fc6445-" + number + "\"]")))
            .withRequestBody(matchingJsonPath("$.serviceCharacteristic[?(@.name=='processId')].value",
                equalToJson("[\"d9b25a0c-2817-498f-b51b-4ac759fc6445\"]")))
            .withRequestBody(matchingJsonPath("$.serviceCharacteristic[?(@.name=='entityId')].value",
                equalToJson("[" + request.getId() + "]")))
            .withRequestBody(matchingJsonPath("$.serviceCharacteristic[?(@.name=='routingNumber')].value",
                equalToJson("[\"3903\"]")))
            .withRequestBody(matchingJsonPath("$.serviceCharacteristic[?(@.name=='dateFrom')].value",
                equalToJson("[\"" + request.getData().get("portingDate").toString() + "\"]")))
            .willReturn(aResponse().withStatus(200)));
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

    private void assertSasTables(XmEntity result, List<String> states, List<String> statesHistory) {
        List<Map<String, Object>> mnpRequests = jdbcTemplate.queryForList("select * from XM_ENTITY.SAS_MNP_REQUEST where REQUEST_ID = ?", result.getId());
        System.out.println(mnpRequests);
        assertEquals(states.size(), mnpRequests.size());
        for(int i = 0; i < states.size(); i++) {
            assertEquals(states.get(i), mnpRequests.get(i).get("NUMBER_STATUS"));
        }

        List<Map<String, Object>> mnpStatusHistory = jdbcTemplate.queryForList("select * from XM_ENTITY.SAS_MNP_REQUEST_STATE where REQUEST_ID = ? ORDER BY STATUS_DATE ASC", result.getId());
        System.out.println(mnpStatusHistory);
        assertEquals(statesHistory.size(), mnpStatusHistory.size());
        int i = 0;
        for (String it : statesHistory) {
            assertEquals(it, mnpStatusHistory.get(i++).get("STATUS"));
        }
    }

    @SneakyThrows
    private void mockPortInCreate() {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        WIRE_MOCK.stubFor(post(urlEqualTo("/oauth/token"))
            .withHeader("Authorization", matching("Basic .*"))
            .withHeader("Content-Type", containing("application/x-www-form-urlencoded"))
            .withRequestBody(equalTo("grant_type=client_credentials"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{ \"access_token\": \"mock-token\", \"token_type\": \"bearer\", \"expires_in\": 3600 }")));
        WIRE_MOCK.stubFor(post(urlEqualTo("/api/communicationManagement/v2/communicationMessage/send"))
            .willReturn(aResponse().withStatus(200)));
        WIRE_MOCK.stubFor(put(urlEqualTo("/cdb/api/v1/portingRequest/d9b25a0c-2817-498f-b51b-4ac759fc6445/confirm"))
            .willReturn(aResponse().withStatus(200)));
        WIRE_MOCK.stubFor(put(urlEqualTo("/cdb/api/v1/portingRequest/d9b25a0c-2817-498f-b51b-4ac759fc6445/numbersActivated"))
            .willReturn(aResponse().withStatus(200)));

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

    private void mockComparePersonalData(List<String> numbers) {
        numbers.forEach(this::assertCompareNumbers);
        WIRE_MOCK.stubFor(get(urlEqualTo("/api/v1/customer/380669000000"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{ \"calculationMethodCode\": \"2\" }")));
        WIRE_MOCK.stubFor(put(urlEqualTo("/cdb/api/v1/portingRequest/d9b25a0c-2817-498f-b51b-4ac759fc6445/donorExclude"))
            .willReturn(aResponse().withStatus(200)));
    }

    private void assertCompareNumbers(String number) {
        WIRE_MOCK.stubFor(get(urlEqualTo("/api/v1/customer/" + number))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{ \"calculationMethodCode\": \"3\" }")));
        WIRE_MOCK.stubFor(get(urlEqualTo("/api/v1/customer/" + number + "/blockedServiceList"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("[]")));
    }

}
