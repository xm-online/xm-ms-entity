package com.icthh.xm.ms.entity.mnp;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static com.icthh.xm.ms.entity.config.tenant.WebappTenantOverrideConfiguration.WIRE_MOCK;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformer;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.config.client.service.TenantConfigService;
import com.icthh.xm.commons.lep.XmLepScriptConfigServerResourceLoader;
import com.icthh.xm.commons.lep.api.LepManagementService;
import com.icthh.xm.commons.scheduler.domain.ScheduledEvent;
import com.icthh.xm.commons.scheduler.service.SchedulerHandler;
import com.icthh.xm.commons.scheduler.service.SchedulerService;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.topic.domain.TopicConfig;
import com.icthh.xm.commons.topic.message.MessageService;
import com.icthh.xm.commons.topic.service.KafkaTemplateService;
import com.icthh.xm.ms.entity.AbstractSpringBootTest;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.ext.IdOrKey;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
import com.icthh.xm.ms.entity.repository.search.XmEntitySearchRepository;
import com.icthh.xm.ms.entity.service.FunctionService;
import com.icthh.xm.ms.entity.service.SeparateTransactionExecutor;
import com.icthh.xm.ms.entity.service.XmEntityService;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.elasticsearch.index.query.QueryBuilders;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

@Slf4j
@ContextConfiguration(classes = {
    TestMnpUpdateModeConfiguration.class
})
@ActiveProfiles("standard-spec-path")
@WithMockUser(authorities = {"SUPER-ADMIN"})
@Category(TestMnpProcessIntTest.class)
public class TestMnpProcessIntTest extends AbstractSpringBootTest {

    public static final String TENANT_CONFIG_YML = "/config/tenants/XM/tenant-config.yml";
    static KafkaContainer kafkaContainer = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.0.1"));

    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Autowired
    private LepManagementService lepManager;

    @Autowired
    private XmEntitySearchRepository searchRepository;

    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

    @Autowired
    private SeparateTransactionExecutor separateTransactionExecutor;

    @MockBean
    private RestTemplate loadBalancedRestTemplateWithTimeout;

    @Autowired
    private List<RefreshableConfiguration> refreshableConfigurations;

    @Autowired
    private XmLepScriptConfigServerResourceLoader loader;

    @Autowired
    private XmEntityService xmEntityService;

    @Autowired
    private SchedulerHandler schedulerHandler;

    @Autowired
    private XmEntityRepository xmEntityRepository;

    @Autowired
    private TenantConfigService tenantConfigService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private FunctionService functionService;

    @Autowired
    private SchedulerService schedulerService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private XmAuthenticationContextHolder authContextHolder;

    @Autowired
    private XmLepScriptConfigServerResourceLoader lepLoader;

    @MockBean
    private KafkaTemplateService kafkaTemplateService;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    private WireMockServer wireMockServer;

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
        CallFunctionTransformer.functionService = functionService;
        CallFunctionTransformer.tenantContextHolder = tenantContextHolder;
        CallFunctionTransformer.lepManager = lepManager;

        MockitoAnnotations.initMocks(this);
        TenantContextUtils.setTenant(tenantContextHolder, "XM");

        var basePath = "/home/ssenko/work/mw-ms-config-repository";
        Collection<File> files = FileUtils.listFiles(new File(basePath + "/config/tenants/XM/entity"), null, true);
        Map<String, String> fileCache = new HashMap<>();
        refreshableConfigurations.stream().filter(it -> it instanceof XmLepScriptConfigServerResourceLoader).forEach(it -> {
            refresh(it, files, fileCache, basePath);
        });
        lepLoader.onRefresh("/config/tenants/XM/entity/lep/commons/sftp/Commons$$readSftpFile$$around.groovy",
            readFile("mnp/readSftpFile.groovy"));
        lepLoader.refreshFinished(List.of("/config/tenants/XM/entity/lep/commons/sftp/Commons$$readSftpFile$$around.groovy"));
        Thread.sleep(10000);

        lepManager.beginThreadContext();
        Collection<File> tenantConfig = List.of(new File(basePath + TENANT_CONFIG_YML));
        refreshableConfigurations.stream().filter(it -> !(it instanceof XmLepScriptConfigServerResourceLoader)).forEach(it -> {
            refresh(it, files, fileCache, basePath);
            refresh(it, tenantConfig, fileCache, basePath);
        });

        // TODO uncomment
//        WIRE_MOCK.stubFor(any(urlMatching(".*"))
//            .atPriority(10)  // Use a lower priority than your expected stubs.
//            .willReturn(aResponse()
//                .withFault(Fault.MALFORMED_RESPONSE_CHUNK)));
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
    @DirtiesContext
    public void testResendInWorkingTime() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger rootLogger = loggerContext.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        rootLogger.setLevel(Level.INFO);

        mockSystemToken();
        WIRE_MOCK.stubFor(get(urlPathEqualTo("/api/functions/RESEND-ONE-REQUEST-IN-WORKING-TIME"))
            .willReturn(aResponse().withTransformers("resend-one-request-in-working-time-transformer")));


        List<Long> ids = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        separateTransactionExecutor.doInSeparateTransaction(() -> {
            for (int i = 0; i < 2; i++) {
                XmEntity portIn = readPortInRequest(objectMapper);
                portIn.getData().put("nextState", Map.of("statusCode", "108"));
                portIn.setId(null);
                var result = xmEntityRepository.save(portIn);

                elasticsearchTemplate.index(new IndexQueryBuilder()
                    .withId("" + result.getId())
                    .withObject(result)
                    .build());
                ids.add(result.getId());
            }
            return null;
        });

        elasticsearchTemplate.refresh(XmEntity.class);
        var result = elasticsearchTemplate.queryForList(
            new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.commonTermsQuery("data.nextState.statusCode", "108")).build(),
            XmEntity.class);
        log.info("Found: {}", result.size());

        ScheduledEvent scheduledEvent = new ScheduledEvent();
        scheduledEvent.setUuid(UUID.randomUUID().toString());
        scheduledEvent.setTypeKey("resendRequestInWorkingTime");
        schedulerHandler.onEvent(scheduledEvent, "XM");
        TenantContextUtils.setTenant(tenantContextHolder, "XM");
        Thread.sleep(5000);

        var timeWhenCorrectExecuted = Instant.now();

        ScheduledEvent scheduledEvent2 = new ScheduledEvent();
        scheduledEvent2.setUuid(UUID.randomUUID().toString());
        scheduledEvent2.setTypeKey("resendRequestInWorkingTime");
        schedulerHandler.onEvent(scheduledEvent2, "XM");
        TenantContextUtils.setTenant(tenantContextHolder, "XM");

        Thread.sleep(5000);

        separateTransactionExecutor.doInSeparateTransaction(() -> {
            for (XmEntity xmEntity : xmEntityRepository.findAllById(ids)) {
                Map<String, Object> nextState = (Map<String, Object>) xmEntity.getData().get("nextState");
                assertNull(nextState);
            }
            return null;
        });
        var timeOptLock = xmEntityRepository.findOneByKeyAndTypeKey("MNP_RESEND_IN_WORKING_TIME", "MNP_RESEND_IN_WORKING_TIME");
        assertTrue(timeOptLock.getUpdateDate().isBefore(timeWhenCorrectExecuted));

    }

    @SneakyThrows
    private XmEntity readPortInRequest(ObjectMapper objectMapper) {
        return objectMapper.readValue(getClass().getClassLoader().getResourceAsStream("mnp/portInRequest.json"), XmEntity.class);
    }

    @Test
    @SneakyThrows
    @DirtiesContext
    public void testPortInRequest() {
        reset(kafkaTemplateService);
        mockServer();
        XmEntity result = portInProcess(false, "mnp/portInRequest.json");
        portInActivate(result, false);
        finishActivation(result);
        WireMock.verify(putRequestedFor(urlEqualTo("/crm/api/v1/portingRequest")));
        wireMockServer.stop();
    }

    @Test
    @SneakyThrows
    @DirtiesContext
    public void testPortInRequestCancel() {
        reset(kafkaTemplateService);
        mockServer();
        XmEntity result = portInProcess(false, "mnp/portInRequest.json");

        WIRE_MOCK.stubFor(get(urlEqualTo("/api/v1/customer/380985111112"))
            .willReturn(aResponse().withStatus(200)));
        WIRE_MOCK.stubFor(put(urlEqualTo("/cdb/api/v1/portingRequest/d9b25a0c-2817-498f-b51b-4ac759fc6445/cancel"))
            .willReturn(aResponse().withStatus(200)));
        xmEntityService.updateState(IdOrKey.of(result.getId()), "CANCELED", Map.of());
        functionService.execute("PROCESS-STATUS", new HashMap<>(Map.of(
            "messageId", "431834d8-1bc7-40e6-a65f-42ebb157d666",
            "processId", "d9b25a0c-2817-498f-b51b-4ac759fc6445",
            "messageType", "ValidationResponse",
            "portingDate", "2066-11-26T11:00:00Z",
            "statusCode", "0"
        )), "GET");

        assertSasTables(result, List.of("CANCELED", "CANCELED", "REJECTED"),
            List.of("VERIFICATION-STARTED", "REGISTERED", "ACCEPTED", "CONFIRMED", "CANCELED"));
        verifyKafkaEvent(result, "CANCELED");

        WireMock.verify(putRequestedFor(urlEqualTo("/crm/api/v1/portingRequest")));
        wireMockServer.stop();
    }

    @Test
    @SneakyThrows
    @DirtiesContext
    public void testPortInRequestCancelNewBilling() {
        reset(kafkaTemplateService);
        mockServer();
        XmEntity result = portInProcess(true, "mnp/portInRequestNewBilling.json");

        WIRE_MOCK.stubFor(put(urlEqualTo("/cdb/api/v1/portingRequest/d9b25a0c-2817-498f-b51b-4ac759fc6445/cancel"))
            .willReturn(aResponse().withStatus(200)));
        xmEntityService.updateState(IdOrKey.of(result.getId()), "CANCELED", Map.of());
        functionService.execute("PROCESS-STATUS", new HashMap<>(Map.of(
            "messageId", "431834d8-1bc7-40e6-a65f-42ebb157d666",
            "processId", "d9b25a0c-2817-498f-b51b-4ac759fc6445",
            "messageType", "ValidationResponse",
            "portingDate", "2066-11-26T11:00:00Z",
            "statusCode", "0"
        )), "GET");

        assertSasTables(result, List.of("CANCELED", "CANCELED", "REJECTED"),
            List.of("VERIFICATION-STARTED", "REGISTERED", "ACCEPTED", "CONFIRMED", "CANCELED"));
        verifyKafkaEvent(result, "CANCELED");

        WireMock.verify(0, putRequestedFor(urlEqualTo("/crm/api/v1/portingRequest")));
        WireMock.verify(0, putRequestedFor(urlEqualTo("/api/v1/customer/380985111112")));
        wireMockServer.stop();
    }

    @Test
    @SneakyThrows
    @DirtiesContext
    public void testPortInRequestNewBilling() {
        reset(kafkaTemplateService);
        XmEntity result = portInProcess(true, "mnp/portInRequestNewBilling.json");
        portInActivate(result, true);
        functionService.execute("BILLING-PORTING-COMPLETED", new HashMap<>(Map.of(
            "entityId", result.getId()
        )), "POST");
        finishActivation(result);
        WIRE_MOCK.verify(0, postRequestedFor(urlEqualTo("/api/communicationManagement/v2/communicationMessage/send")));
    }

    @SneakyThrows
    @Test
    @DirtiesContext
    public void testBroadcast() {
        mockSystemToken();
        reset(kafkaTemplateService);

        var content = readFile("mnp/broadcast.json");
        kafkaTemplate.send("CDB_ERROR_MESSAGES", content);
        var wrongContent = readFile("mnp/notbroadcast.json");
        kafkaTemplate.send("CDB_ERROR_MESSAGES", wrongContent);
        Thread.sleep(5000);
        verify(kafkaTemplateService).send(eq("CROSS-LDB-UPDATE"), argThat(it -> {
            var expected = readFile("mnp/expected_broadcast.json").trim();
            assertEquals(expected, it);
            return true;
        }));

        verifyNoMoreInteractions(kafkaTemplateService);
        reset(kafkaTemplateService);

        WIRE_MOCK.stubFor(post(urlEqualTo("/api/v1/mobileNumberLocations"))
            .withHeader("Authorization", equalTo("bearer mock-token"))
            .withHeader("Content-Type", equalTo("application/json;charset=UTF-8"))
            .withRequestBody(equalToJson("{\"msisdns\":[\"380959043208\",\"380950666666\"]}"))
            .willReturn(aResponse().withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(new ObjectMapper().writeValueAsString(List.of(
                    Map.of("msisdn", "380959043208", "routingNumber", "3906", "state", "PORTED"),
                    Map.of("msisdn", "380950666666", "routingNumber", "3906", "state", "PORTED")
                )))));
        functionService.execute("RUN-NUMBER-PORTATION-DIFF", Map.of("date", "2025-02-13"), "GET");
        verify(kafkaTemplateService).send(eq("CROSS-LDB-UPDATE"), argThat(it -> {
            var expected = readFile("mnp/expected_broadcast_2.json").trim();
            assertEquals(expected, it);
            return true;
        }));
        verifyNoMoreInteractions(kafkaTemplateService);
    }


    @SneakyThrows
    @Test
    @DirtiesContext
    public void testReturnNumber() {
        mockServer();
        reset(kafkaTemplateService);
        prepareTenantConfig(false);
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        var returnNumber = objectMapper.readValue(readFile("mnp/return_number.json"), XmEntity.class);
        var returnNumberResult = objectMapper.readValue(readFile("mnp/return_number.json"), XmEntity.class);
        String uuid = "d0b00a0c-0000-498f-b51b-4ac759fc6414";
        returnNumberResult.getData().put("processId", uuid);


        mockSystemToken();
        WIRE_MOCK.stubFor(post(urlEqualTo("/cdb/api/v1/returnNumber"))
            .willReturn(aResponse().withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(objectMapper.writeValueAsString(returnNumberResult))));
        WIRE_MOCK.stubFor(get(urlPathEqualTo("/api/v1/customer/380933646177"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{ \"calculationMethodCode\": \"1\" }")));
        WIRE_MOCK.stubFor(get(urlPathEqualTo("/api/v1/customer/380933646177/status"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{ \"statusCode\": \"Деактивирован\" }")));
        WIRE_MOCK.stubFor(post(urlEqualTo("/api/tasks"))
            .withHeader("Authorization", equalTo("bearer mock-token"))
            .withHeader("Content-Type", equalTo("application/json;charset=UTF-8"))
            .withRequestBody(matchingJsonPath("$.typeKey", equalTo("returnNumberRequestNew")))
            .withRequestBody(matchingJsonPath("$.channelType", equalTo("QUEUE")))
            .withRequestBody(matchingJsonPath("$.scheduleType", equalTo("ONE_TIME")))
            .withRequestBody(matchingJsonPath("$.targetMs", equalTo("entity")))
            .withRequestBody(matchingJsonPath("$.data", equalTo("{\"userKey\":\"user\",\"id\":951}"))));

        returnNumber = xmEntityService.save(returnNumber);

        ScheduledEvent scheduledEvent = new ScheduledEvent();
        scheduledEvent.setUuid(UUID.randomUUID().toString());
        scheduledEvent.setTypeKey("returnNumberRequestNew");
        scheduledEvent.setData(Map.of("id", returnNumber.getId()));
        schedulerService.onEvent(scheduledEvent);

        functionService.execute("PROCESS-STATUS", new HashMap<>(Map.of(
            "messageId", "9938744f-0000-440a-9124-6db72d386db2",
            "messageType", "ValidationResponse",
            "processId", uuid,
            "statusCode", "0"
        )), "GET");
        returnNumber = xmEntityService.findOne(IdOrKey.of(returnNumber.getId()));
        assertEquals("RETURNED", returnNumber.getStateKey());
        verifyNoMoreInteractions(kafkaTemplateService);

        wireMockServer.stop();
    }

    private static void mockSystemToken() {
        WIRE_MOCK.stubFor(post(urlEqualTo("/oauth/token"))
            .withHeader("Authorization", matching("Basic .*"))
            .withHeader("Content-Type", containing("application/x-www-form-urlencoded"))
            .withRequestBody(equalTo("grant_type=client_credentials"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{ \"access_token\": \"mock-token\", \"token_type\": \"bearer\", \"expires_in\": 3600 }")));
    }

    @SneakyThrows
    @Test
    @DirtiesContext
    public void testReturnNumberNewBilling() {
        mockServer();
        reset(kafkaTemplateService);
        prepareTenantConfig(true);
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        var returnNumberResult = objectMapper.readValue(readFile("mnp/return_number.json"), XmEntity.class);
        String uuid = "d0b00a0c-0000-498f-b51b-4ac759fc6414";
        returnNumberResult.getData().put("processId", uuid);

        mockSystemToken();
        WIRE_MOCK.stubFor(post(urlEqualTo("/cdb/api/v1/returnNumber"))
            .willReturn(aResponse().withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(objectMapper.writeValueAsString(returnNumberResult))));

        functionService.execute("FORCE-RETURN-NUMBER", new HashMap<>(Map.of(
            "msisdn", "380930912700"
        )), "GET");

        functionService.execute("PROCESS-STATUS", new HashMap<>(Map.of(
            "messageId", "9938744f-0000-440a-9124-6db72d386db2",
            "messageType", "ValidationResponse",
            "processId", uuid,
            "statusCode", "0"
        )), "GET");
        var returnNumber = xmEntityService.findOne(IdOrKey.ofKey(uuid));
        assertEquals("RETURNED", returnNumber.getStateKey());
        verifyNoMoreInteractions(kafkaTemplateService);
        wireMockServer.stop();
    }

    @SneakyThrows
    private String readFile(String resourcePath) {
        return IOUtils.toString(getClass().getClassLoader().getResourceAsStream(resourcePath), UTF_8);
    }

    @Test
    @SneakyThrows
    @DirtiesContext
    public void testPortOutRequestAnonymous() {
        reset(kafkaTemplateService);
        mockServer();

        mockCheckIsNewBilling("{\"id\": \"SXID380662582086\",\"characteristic\": [{\"name\": \"status\",\"value\": \"NOT_FOUND\"}]}");

        XmEntity result = createPortOutRequest("Anonymous", this::mockComparePersonalDataAnonymous, false, "ACCEPTED");

        functionService.execute("UPDATE-STATUS", IdOrKey.of(result.getId()), new HashMap<>(Map.of(
            "stateKey", "SERVICE-PORT-OUT-ON"
        )));
        assertSasTables(result,
            List.of("REJECTED", "SERVICE-PORT-OUT-ON", "SERVICE-PORT-OUT-ON", "REJECTED"),
            List.of("NEW", "ACCEPTED", "SERVICE-PORT-OUT-ON"));
        verifyKafkaEvent(result, "SERVICE-PORT-OUT-ON");

        runDeactivateForPortOutRequest(result, false);
        finishPortOutRequest(result);

        WireMock.verify(putRequestedFor(urlEqualTo("/crm/api/v1/portingRequest")));
        wireMockServer.stop();

        WIRE_MOCK.verify(1, postRequestedFor(urlEqualTo("/api/tasks"))
                .withHeader("Authorization", equalTo("bearer mock-token"))
                .withHeader("Content-Type", equalTo("application/json;charset=UTF-8"))
                .withRequestBody(matchingJsonPath("$.typeKey", equalTo("portOutRequestNew"))));

        verifyNoMoreInteractions(kafkaTemplateService);
    }

    @Test
    @SneakyThrows
    @DirtiesContext
    public void testPortOutRequestIndividual() {
        mockCheckIsNewBilling("{\"id\": \"SXID380662582086\",\"characteristic\": [{\"name\": \"status\",\"value\": \"NOT_FOUND\"}]}");

        reset(kafkaTemplateService);
        mockServer();
        XmEntity result = createPortOutRequest("Individual", this::mockComparePersonalDataIndividual, false, "ACCEPTED");

        functionService.execute("UPDATE-STATUS", IdOrKey.of(result.getId()), new HashMap<>(Map.of(
            "stateKey", "SERVICE-PORT-OUT-ON"
        )));
        assertSasTables(result,
            List.of("REJECTED", "SERVICE-PORT-OUT-ON", "SERVICE-PORT-OUT-ON", "REJECTED"),
            List.of("NEW", "ACCEPTED", "SERVICE-PORT-OUT-ON"));
        verifyKafkaEvent(result, "SERVICE-PORT-OUT-ON");

        runDeactivateForPortOutRequest(result, false);
        finishPortOutRequest(result);

        WireMock.verify(putRequestedFor(urlEqualTo("/crm/api/v1/portingRequest")));
        wireMockServer.stop();

        verifyNoMoreInteractions(kafkaTemplateService);
    }


    @Test
    @SneakyThrows
    @DirtiesContext
    public void testPortOutRequestAnonymousNewBilling() {
        reset(kafkaTemplateService);
        mockServer();

        mockCheckIsNewBilling("{\"id\": \"380669333333\",\"characteristic\": [{\"name\": \"status\",\"value\": \"MIGRATED\"},{\"name\": \"dateMigrated\",\"value\": \"2023-08-04T18:51:44.732Z\"}]}");
        WIRE_MOCK.stubFor(post(urlPathEqualTo("/tmf-api/processFlowManagement/v4/processFlow"))
            .willReturn(aResponse().withStatus(200)));

        XmEntity result = createPortOutRequest("Anonymous", this::mockComparePersonalDataAnonymousNewBilling, true, "ACCEPTED");

        functionService.execute("UPDATE-STATUS", IdOrKey.of(result.getId()), new HashMap<>(Map.of(
            "stateKey", "SERVICE-PORT-OUT-ON"
        )));
        assertSasTables(result,
            List.of("REJECTED", "SERVICE-PORT-OUT-ON", "SERVICE-PORT-OUT-ON", "REJECTED"),
            List.of("NEW", "ACCEPTED", "SERVICE-PORT-OUT-ON"));
        verifyKafkaEvent(result, "SERVICE-PORT-OUT-ON");

        runDeactivateForPortOutRequest(result, true);
        functionService.execute("BILLING-PORTING-COMPLETED", new HashMap<>(Map.of(
            "entityId", result.getId()
        )), "POST");
        finishPortOutRequest(result);

        WireMock.verify(0, putRequestedFor(urlEqualTo("/crm/api/v1/portingRequest")));
        wireMockServer.stop();

        WIRE_MOCK.verify(0, postRequestedFor(urlEqualTo("/api/tasks"))
            .withHeader("Authorization", equalTo("bearer mock-token"))
            .withHeader("Content-Type", equalTo("application/json;charset=UTF-8"))
            .withRequestBody(matchingJsonPath("$.typeKey", equalTo("portOutRequestNew"))));

        verifyNoMoreInteractions(kafkaTemplateService);
    }

    @Test
    @SneakyThrows
    @DirtiesContext
    public void testPortOutRejectAcceptAfterRejectBilling() {
        mockCheckIsNewBilling("{\"id\": \"380669333333\",\"characteristic\": [{\"name\": \"status\",\"value\": \"MIGRATED\"},{\"name\": \"dateMigrated\",\"value\": \"2023-08-04T18:51:44.732Z\"}]}");
        WIRE_MOCK.stubFor(post(urlPathEqualTo("/tmf-api/processFlowManagement/v4/processFlow"))
            .willReturn(aResponse().withStatus(200)));

        reset(kafkaTemplateService);
        mockServer();
        XmEntity result = createPortOutRequest("Individual", this::mockIndividualRejectNewBilling, true, "REJECTED");

        assertSasTables(result,
            List.of("REJECTED", "REJECTED", "REJECTED", "REJECTED"),
            List.of("NEW", "REJECTED"));

        xmEntityService.updateState(IdOrKey.of(result.getId()), "ACCEPTED", Map.of(
            "messageId", UUID.randomUUID().toString(),
            "messageType", "AutoAccept",
            "statusCode", "252"
        ));
    }

    private void mockIndividualRejectNewBilling() {
        WIRE_MOCK.stubFor(get(urlEqualTo("/api/product?@type=wirelessProduct&msisdn=380669111111"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("[]")));
        WIRE_MOCK.stubFor(get(urlEqualTo("/api/product?@type=wirelessProduct&msisdn=380669222222"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("[]")));
        WIRE_MOCK.stubFor(get(urlEqualTo("/api/product?@type=wirelessProduct&msisdn=380669333333"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("[]")));
        WIRE_MOCK.stubFor(get(urlEqualTo("/api/product?@type=wirelessProduct&msisdn=380669000000"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("[]")));


        WIRE_MOCK.stubFor(put(urlEqualTo("/cdb/api/v1/portingRequest/d9b25a0c-2817-498f-b51b-4ac759fc6445/reject"))
            .willReturn(aResponse().withStatus(200)));
    }

    private void mockCheckIsNewBilling(String body) {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger rootLogger = loggerContext.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        rootLogger.setLevel(Level.INFO);

        rejectNotMockedRequests();
        WIRE_MOCK.stubFor(get(urlPathEqualTo("/api/customerManagement/v3/customer/380669333333"))
            .withHeader("Profile", equalTo("CUSTOMER-STATUS"))
            .willReturn(aResponse().withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(body))
        );
    }

    private void mockComparePersonalDataAnonymousNewBilling() {

        String jsonBody = "[{\"id\":\"952\",\"relatedParty\":[{\"id\":\"1002\",\"role\":\"Owner\",\"partyId\":\"29002\",\"@type\":\"BssIndividual\"}],\"@type\":\"wirelessProduct\"}]";
        WIRE_MOCK.stubFor(get(urlEqualTo("/api/product?@type=wirelessProduct&msisdn=380669111111"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("[]")));
        WIRE_MOCK.stubFor(get(urlEqualTo("/api/product?@type=wirelessProduct&msisdn=380669222222"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("[]")));
        WIRE_MOCK.stubFor(get(urlEqualTo("/api/product?@type=wirelessProduct&msisdn=380669333333"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(jsonBody)));

        mockBarring();

        WIRE_MOCK.stubFor(get(urlEqualTo("/api/customerManagement/v3/customer/380669000000"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("[{\"characteristic\":[{\"name\":\"msisdn\",\"value\":\"380669000000\"},{\"name\":\"schemaType\",\"value\":\"POP\"}]}]")));

        WIRE_MOCK.stubFor(get(urlEqualTo("/api/customerManagement/v3/customer/380669111111"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("[{\"characteristic\":[{\"name\":\"msisdn\",\"value\":\"380669111111\"},{\"name\":\"schemaType\",\"value\":\"PRP\"}]}]")));

        WIRE_MOCK.stubFor(get(urlEqualTo("/api/customerManagement/v3/customer/380669222222"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("[{\"characteristic\":[{\"name\":\"msisdn\",\"value\":\"380669222222\"},{\"name\":\"schemaType\",\"value\":\"PRP\"}]}]")));

        WIRE_MOCK.stubFor(get(urlEqualTo("/api/customerManagement/v3/customer/380669333333"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("[{\"characteristic\":[{\"name\":\"msisdn\",\"value\":\"380669333333\"},{\"name\":\"schemaType\",\"value\":\"PRP\"}]}]")));

        WIRE_MOCK.stubFor(put(urlEqualTo("/cdb/api/v1/portingRequest/d9b25a0c-2817-498f-b51b-4ac759fc6445/donorExclude"))
            .willReturn(aResponse().withStatus(200)));
    }

    @Test
    @SneakyThrows
    @DirtiesContext
    public void testPortOutRequestIndividualNewBilling() {
        mockCheckIsNewBilling("{\"id\": \"380669333333\",\"characteristic\": [{\"name\": \"status\",\"value\": \"MIGRATED\"},{\"name\": \"dateMigrated\",\"value\": \"2023-08-04T18:51:44.732Z\"}]}");
        WIRE_MOCK.stubFor(post(urlPathEqualTo("/tmf-api/processFlowManagement/v4/processFlow"))
            .willReturn(aResponse().withStatus(200)));

        reset(kafkaTemplateService);
        mockServer();
        XmEntity result = createPortOutRequest("Individual", this::mockComparePersonalDataIndividualNewBilling, true, "ACCEPTED");

        functionService.execute("UPDATE-STATUS", IdOrKey.of(result.getId()), new HashMap<>(Map.of(
            "stateKey", "SERVICE-PORT-OUT-ON"
        )));
        assertSasTables(result,
            List.of("REJECTED", "SERVICE-PORT-OUT-ON", "SERVICE-PORT-OUT-ON", "REJECTED"),
            List.of("NEW", "ACCEPTED", "SERVICE-PORT-OUT-ON"));
        verifyKafkaEvent(result, "SERVICE-PORT-OUT-ON");

        runDeactivateForPortOutRequest(result, true);
        functionService.execute("BILLING-PORTING-COMPLETED", new HashMap<>(Map.of(
            "entityId", result.getId()
        )), "POST");
        finishPortOutRequest(result);

        WireMock.verify(0, putRequestedFor(urlEqualTo("/crm/api/v1/portingRequest")));
        wireMockServer.stop();

        verifyNoMoreInteractions(kafkaTemplateService);
    }


    private static void rejectNotMockedRequests() {
        WIRE_MOCK.stubFor(any(urlMatching(".*"))
            .atPriority(100)
            .willReturn(aResponse()
                .withStatus(500)
                .withBody("{\"error\":\"Not mocked url\"}")
            ));
    }


    private void finishPortOutRequest(XmEntity result) {
        functionService.execute("PROCESS-STATUS", new HashMap<>(Map.of(
            "messageId", "9938744f-3f08-440a-9124-6db72d386db2",
            "messageType", "ValidationResponse",
            "processId", "d9b25a0c-2817-498f-b51b-4ac759fc6445",
            "statusCode", "0"
        )), "GET");

        assertSasTables(result,
            List.of("REJECTED", "DEACTIVATED", "DEACTIVATED", "REJECTED"),
            List.of("NEW", "ACCEPTED", "SERVICE-PORT-OUT-ON", "STARTED", "DEACTIVATED"));
        verifyKafkaEvent(result, "DEACTIVATED");

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
        verifyKafkaEvent(result, "DEACTIVATED");

        xmEntityService.updateState(IdOrKey.of(result.getId()), "PORTED", Map.of(
            "msisdn", "380669222222",
            "messageId", "431834d8-1bc7-40e6-a65f-42ebb157d901"
        ));
        assertSasTables(result,
            List.of("REJECTED", "DEACTIVATED", "DEACTIVATED", "REJECTED"),
            List.of("NEW", "ACCEPTED", "SERVICE-PORT-OUT-ON", "STARTED", "DEACTIVATED"));

        xmEntityService.updateState(IdOrKey.of(result.getId()), "PORTED", Map.of(
            "msisdn", "380669111111",
            "messageId", "431834d8-1bc7-40e6-a65f-42ebb157d902"
        ));
        assertSasTables(result,
            List.of("REJECTED", "PORTED", "PORTED", "REJECTED"),
            List.of("NEW", "ACCEPTED", "SERVICE-PORT-OUT-ON", "STARTED", "DEACTIVATED", "PORTED"));
        verifyKafkaEvent(result, "PORTED");
    }

    private void runDeactivateForPortOutRequest(XmEntity result, boolean isNewBilling) {
        mockPortOutStart(result, List.of("380669222222", "380669111111"), isNewBilling);
        xmEntityService.updateState(IdOrKey.of(result.getId()), "STARTED", Map.of(
            "numbers", List.of(Map.of("msisdn", "380669111111"), Map.of("msisdn", "380669222222")),
            "messageId", "9938744f-3f08-440a-9124-6db72d386db1"
        ));
        assertSasTables(result,
            List.of("REJECTED", "STARTED", "STARTED", "REJECTED"),
            List.of("NEW", "ACCEPTED", "SERVICE-PORT-OUT-ON", "STARTED"));
        verifyKafkaEvent(result, "STARTED");

        functionService.execute("NOTIFY-NUMBER-PROCESSED", IdOrKey.of(result.getId()), new HashMap<>(Map.of(
            "msisdn", "380669222222"
        )));
        assertSasTables(result,
            List.of("REJECTED", "STARTED", "STARTED", "REJECTED"),
            List.of("NEW", "ACCEPTED", "SERVICE-PORT-OUT-ON", "STARTED"));

        mockPortOutDeactivated();
        functionService.execute("NOTIFY-NUMBER-PROCESSED", IdOrKey.of(result.getId()), new HashMap<>(Map.of(
            "msisdn", "380669111111"
        )));


    }

    @NotNull
    @SneakyThrows
    private XmEntity createPortOutRequest(String partyType, Runnable mockComparePersonalData, boolean isNewBilling, String nextStatus) throws IOException {
        mockPortOutCreate(isNewBilling);
        mockComparePersonalData.run();
        prepareTenantConfig(isNewBilling);

        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        XmEntity portOut = objectMapper.readValue(getClass().getClassLoader().getResourceAsStream("mnp/portOutRequest" + partyType + ".json"), XmEntity.class);

        XmEntity result = xmEntityService.save(portOut);
        System.out.println(result);
        assertNotNull(result.getId());
        assertSasTables(result, List.of("NEW", "NEW", "NEW", "NEW"), List.of("NEW"));

        // event (uuid=d0349bb3-8be3-423a-96d4-4cd1ea3e30df, id=14228938, key=null, name=null, typeKey=portOutRequestNew, stateKey=null, createdBy=middleware, startDate=2025-04-10T17:58:22.874629Z, endDate=2025-04-13T17:58:22.874629Z, handlingTime=2025-04-10T17:58:22.874760Z, channelType=QUEUE, data={processId=e3bb2655-0afa-4525-8093-1048d0930046, userKey=middleware, id=108820670})

        // functionService.execute("COMPARE-DATA-FROM-CRM", IdOrKey.of(result.getId()), new HashMap<>());
        ScheduledEvent scheduledEvent = new ScheduledEvent();
        scheduledEvent.setUuid(UUID.randomUUID().toString());
        scheduledEvent.setTypeKey("portOutRequestNew");
        scheduledEvent.setData(Map.of("id", result.getId(), "processId", result.getKey()));
        schedulerService.onEvent(scheduledEvent);
        TenantContextUtils.setTenant(tenantContextHolder, "XM");
        Thread.sleep(5000);
        ScheduledEvent scheduledEvent2 = new ScheduledEvent();
        scheduledEvent2.setUuid(UUID.randomUUID().toString());
        scheduledEvent2.setTypeKey("mnpComparePersonalData");
        scheduledEvent2.setData(Map.of());
        schedulerService.onEvent(scheduledEvent2);
        TenantContextUtils.setTenant(tenantContextHolder, "XM");
        Thread.sleep(5000);

        assertSasTables(result, List.of("NEW", "NEW", "NEW", "NEW"), List.of("NEW"));

        mockPortOutAccept();
        functionService.execute("PROCESS-STATUS", new HashMap<>(Map.of(
            "messageId", "9938744f-3f08-440a-9124-6db72d386db1",
            "processId", "d9b25a0c-2817-498f-b51b-4ac759fc6445",
            "messageType", "ValidationResponse",
            "statusCode", "0"
        )), "GET");
        assertSasTables(result, List.of("REJECTED", nextStatus, nextStatus, "REJECTED"), List.of("NEW", nextStatus));
        verifyKafkaEvent(result, nextStatus);
        return result;
    }


    private void mockServer() {
        wireMockServer = new WireMockServer(options().dynamicPort());
        wireMockServer.start();
        int port = wireMockServer.port();
        configureFor("localhost", port);
        stubFor(any(urlMatching(".*"))
            .atPriority(100)  // Use a lower priority than your expected stubs.
            .willReturn(aResponse()
                .withStatus(500)
                .withBody("{\"error\":\"Not mocked url\"}")
            ));
    }

    private XmEntity portInProcess(boolean isNewBilling, String name) throws IOException {
        mockPortInCreate(isNewBilling);
        prepareTenantConfig(isNewBilling);

        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        XmEntity portIn = objectMapper.readValue(getClass().getClassLoader().getResourceAsStream(name), XmEntity.class);

        XmEntity result = xmEntityService.save(portIn);
        System.out.println(result);
        assertNotNull(result.getId());
        verify(kafkaTemplateService).send("pending-portin-requests-queue",
            Base64.getEncoder().encodeToString(objectMapper.writeValueAsBytes(Map.of("entityId", result.getId())))
        );
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
        verifyKafkaEvent(result, "REGISTERED");

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

        verifyKafkaEvent(result, "ACCEPTED");
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
            "portingDate", "2066-11-26T11:00:00Z"
        )), "GET");
        assertSasTables(result, List.of("CONFIRMED", "CONFIRMED", "REJECTED"),
            List.of("VERIFICATION-STARTED", "REGISTERED", "ACCEPTED", "CONFIRMED"));
        verifyKafkaEvent(result, "CONFIRMED");

        return result;
    }

    private void verifyKafkaEvent(XmEntity result, String state) {
        XmEntity expectedEntity = xmEntityService.findOne(IdOrKey.of(result.getId()));
        var expected = createMessage(expectedEntity, state);
        verify(kafkaTemplateService).send(eq("LDB-REQUEST-UPDATES"), eq(0), eq(expectedEntity.getId().toString()), argThat(it -> {
            Map<?, ?> actualMap = getMap(it);
            actualMap.remove("updateDate");
            assertEquals(expected, actualMap);
            return true;
        }));
        verifyNoMoreInteractions(kafkaTemplateService);
        reset(kafkaTemplateService);
    }

    @SneakyThrows
    private static Map<?, ?> getMap(String it) {
        return new ObjectMapper().readValue(it, HashMap.class);
    }

    @SneakyThrows
    private Map<String, Object> createMessage(XmEntity portingRequest, String newStateKey) {
        var map = new HashMap<String, Object>() {
            {
                put("id", portingRequest.getId());
                put("requestType", portingRequest.getTypeKey().substring(portingRequest.getTypeKey().lastIndexOf('.') + 1));
                put("processId", portingRequest.getData().get("processId"));
                put("status", newStateKey);
                put("messageType", null);
                Map<String, Object> nextState = (Map<String, Object>) portingRequest.getData().get("nextState");
                put("statusCode", nextState != null ? nextState.get("statusCode") : null);
                put("numbers", ((List<Map<String, Object>>) portingRequest.getData().get("numbers")).stream().map(it ->
                    new HashMap<String, Object>() {{
                        put("msisdn", it.get("msisdn").toString());
                        put("statusCode", it.get("statusCode") == null ? null : it.get("statusCode").toString());
                    }}
                ).collect(toList()));
                put("portedDate", portingRequest.getData().get("portingDate"));
                put("channel", portingRequest.getData().get("channel"));
                put("isNewBilling", portingRequest.getData().get("isNewBilling"));
            }};

        return new ObjectMapper().readValue(new ObjectMapper().writeValueAsString(map), HashMap.class);
    }

    private void portInActivate(XmEntity result, boolean isNewBilling) {
        var entity = xmEntityService.findById(result.getId());

        List.of("380985111111", "380985222222").forEach(number -> assertActivateNumber(entity, number, isNewBilling));
        xmEntityService.updateState(IdOrKey.of(result.getId()), "STARTED", Map.of(
            "numbers", List.of(Map.of("msisdn", "380985111111"), Map.of("msisdn", "380985222222")),
            "messageId", "431834d8-1bc7-40e6-a65f-42ebb157d004"
        ));
        verifyKafkaEvent(result, "STARTED");
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
    }

    private void finishActivation(XmEntity result) {
        functionService.execute("PROCESS-STATUS", new HashMap<>(Map.of(
            "messageId", "431834d8-1bc7-40e6-a65f-42ebb157d005",
            "messageType", "ValidationResponse",
            "processId", "d9b25a0c-2817-498f-b51b-4ac759fc6445",
            "statusCode", "0"
        )), "GET");
        verifyKafkaEvent(result, "ACTIVATED");
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
        verifyKafkaEvent(result, "PORTED");
        assertSasTables(result,
            List.of("PORTED", "PORTED", "REJECTED"),
            List.of("VERIFICATION-STARTED", "REGISTERED", "ACCEPTED", "CONFIRMED", "STARTED", "ACTIVATED", "PORTED"));
    }

    private void prepareTenantConfig(boolean isNewBilling) throws JsonProcessingException {
        HashMap<String, Object> tenantConfig = new HashMap<>(tenantConfigService.getConfig());
        if (!isNewBilling) {
            tenantConfig.put("crm", Map.of(
                "baseUrl", "http://localhost:" + wireMockServer.port()
            ));
        }
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
        tenantConfig.put("ldb", new HashMap<>((Map<?, ?>) tenantConfig.get("ldb")));
        var newBillingCheck = (Map<String, Map<String, Boolean>>)tenantConfig.get("ldb");
        newBillingCheck.put("newBillingCheckEnabledFor", new HashMap<>(newBillingCheck.get("newBillingCheckEnabledFor")));
        newBillingCheck.get("newBillingCheckEnabledFor").put("INDIVIDUAL", true);
        newBillingCheck.get("newBillingCheckEnabledFor").put("ANONYMOUS", true);

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

    private void mockPortOutStart(XmEntity request, List<String> numbers, boolean isNewBilling) {
        numbers.forEach(number -> assertDeactivateNumber(request, number, isNewBilling));
        if (!isNewBilling) {
            WIRE_MOCK.stubFor(post(urlEqualTo("/api/communicationManagement/v2/communicationMessage/send"))
                .willReturn(aResponse().withStatus(200)));
        }
    }

    private static void assertActivateNumber(XmEntity request, String number, boolean isNewBilling) {
        MappingBuilder mapping = post(urlEqualTo("/DSEntityProvisioning/api/ActivationAndConfiguration/v2/service"))
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
            .willReturn(aResponse().withStatus(200));
        if (isNewBilling) {
            mapping = mapping.withRequestBody(matchingJsonPath("$.serviceCharacteristic[?(@.name=='isNewBilling')].value",
                equalToJson("[\"true\"]")));
        }
        WIRE_MOCK.stubFor(mapping);
    }

    private static void assertDeactivateNumber(XmEntity request, String number, boolean isNewBilling) {
        MappingBuilder mapping = post(urlEqualTo("/DSEntityProvisioning/api/ActivationAndConfiguration/v2/service"))
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
            .willReturn(aResponse().withStatus(200));
        if (isNewBilling) {
            mapping = mapping.withRequestBody(matchingJsonPath("$.serviceCharacteristic[?(@.name=='isNewBilling')].value",
                equalToJson("[\"true\"]")));
        }
        WIRE_MOCK.stubFor(mapping);
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
    private void mockPortInCreate(boolean isNewBilling) {
        if (!isNewBilling) {
            stubFor(put(urlEqualTo("/crm/api/v1/portingRequest")).atPriority(1).willReturn(aResponse().withStatus(200)));
        }

        mockSystemToken();
        if (!isNewBilling) {
            WIRE_MOCK.stubFor(post(urlEqualTo("/api/communicationManagement/v2/communicationMessage/send"))
                .willReturn(aResponse().withStatus(200)));
        }
        WIRE_MOCK.stubFor(put(urlEqualTo("/cdb/api/v1/portingRequest/d9b25a0c-2817-498f-b51b-4ac759fc6445/confirm"))
            .willReturn(aResponse().withStatus(200)));
        WIRE_MOCK.stubFor(put(urlEqualTo("/cdb/api/v1/portingRequest/d9b25a0c-2817-498f-b51b-4ac759fc6445/numbersActivated"))
            .willReturn(aResponse().withStatus(200)));
    }

    private void mockPortOutCreate(boolean isNewBilling) {
        mockSystemToken();
        if (!isNewBilling) {
            stubFor(put(urlEqualTo("/crm/api/v1/portingRequest")).atPriority(1).willReturn(aResponse().withStatus(200)));
            WIRE_MOCK.stubFor(post(urlEqualTo("/api/tasks"))
                .withHeader("Authorization", equalTo("bearer mock-token"))
                .withHeader("Content-Type", equalTo("application/json;charset=UTF-8"))
                .withRequestBody(matchingJsonPath("$.typeKey", equalTo("portOutRequestNew")))
                .withRequestBody(matchingJsonPath("$.channelType", equalTo("QUEUE")))
                .withRequestBody(matchingJsonPath("$.scheduleType", equalTo("ONE_TIME")))
                .withRequestBody(matchingJsonPath("$.targetMs", equalTo("entity")))
                .withRequestBody(matchingJsonPath("$.ttl", equalTo("259200")))
                .withRequestBody(containing("d9b25a0c-2817-498f-b51b-4ac759fc6445"))
                .willReturn(aResponse().withStatus(201)));
            WIRE_MOCK.stubFor(post(urlEqualTo("/api/communicationManagement/v2/communicationMessage/send"))
                .willReturn(aResponse().withStatus(200)));
        }
    }

    private void mockComparePersonalDataAnonymous() {
        assertCompareNumbers("380669111111");
        assertCompareNumbers("380669222222");
        WIRE_MOCK.stubFor(get(urlEqualTo("/api/v1/customer/380669000000"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{ \"calculationMethodCode\": \"2\" }")));
        WIRE_MOCK.stubFor(get(urlEqualTo("/api/v1/customer/380669333333"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{ \"calculationMethodCode\": \"1\" }")));

        stubFor(get(urlPathEqualTo("/crm/partyManagement/v1/individual"))
            .atPriority(1)
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("[{\"id\":\"380669333333\", \"registeredOwner\":\"true\"}]")
            ));

        WIRE_MOCK.stubFor(put(urlEqualTo("/cdb/api/v1/portingRequest/d9b25a0c-2817-498f-b51b-4ac759fc6445/donorExclude"))
            .willReturn(aResponse().withStatus(200)));
    }

    private void mockComparePersonalDataIndividualNewBilling() {
        String jsonBody = "[{\"id\":\"952\",\"relatedParty\":[{\"id\":\"1002\",\"role\":\"Owner\",\"partyId\":\"29002\",\"@type\":\"BssIndividual\"}],\"@type\":\"wirelessProduct\"}]";
        WIRE_MOCK.stubFor(get(urlEqualTo("/api/product?@type=wirelessProduct&msisdn=380669111111"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(jsonBody)));
        WIRE_MOCK.stubFor(get(urlEqualTo("/api/product?@type=wirelessProduct&msisdn=380669222222"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(jsonBody)));
        WIRE_MOCK.stubFor(get(urlEqualTo("/api/product?@type=wirelessProduct&msisdn=380669333333"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(jsonBody)));
        WIRE_MOCK.stubFor(get(urlEqualTo("/api/product?@type=wirelessProduct&msisdn=380669000000"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("[]")));

        WIRE_MOCK.stubFor(get(urlPathEqualTo("/tmf-api/party/v4/individual/29002"))
            .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody("{\"givenName\":\"John\",\"familyName\":\"Doe\"}")));

        WIRE_MOCK.stubFor(put(urlEqualTo("/cdb/api/v1/portingRequest/d9b25a0c-2817-498f-b51b-4ac759fc6445/donorExclude"))
            .willReturn(aResponse().withStatus(200)));

        mockBarring();
    }

    private static void mockBarring() {
        var noBlocking = "{\"id\": \"380662582086\",\"characteristics\": {\"barringList\": []}}";
        var blocking = "{\"id\": \"380662582086\",\"characteristics\": {\"barringList\": [{\"code\": \"FRBLKMNP\"}]}}\n";

        WIRE_MOCK.stubFor(get(urlPathEqualTo("/api/customerManagement/v3/customer/380669333333"))
            .withHeader("Profile", equalTo("PRODUCT-BASED-DATA"))
            .willReturn(aResponse().withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(blocking))
        );
        WIRE_MOCK.stubFor(get(urlPathEqualTo("/api/customerManagement/v3/customer/380669222222"))
            .withHeader("Profile", equalTo("PRODUCT-BASED-DATA"))
            .willReturn(aResponse().withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(noBlocking))
        );
        WIRE_MOCK.stubFor(get(urlPathEqualTo("/api/customerManagement/v3/customer/380669111111"))
            .withHeader("Profile", equalTo("PRODUCT-BASED-DATA"))
            .willReturn(aResponse().withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(noBlocking))
        );
    }

    private void mockComparePersonalDataIndividual() {
        String json = "[" +
            "{\"id\":380669111111,\"givenName\":\"John\",\"familyName\":\"Doe\",\"middleName\":\"Smith\",\"itn\":\"\",\"registeredOwner\":true,\"individualIdentification\":[{\"type\":\"passport\",\"identificationId\":\"TEST\"}]}," +
            "{\"id\":380669222222,\"givenName\":\"John\",\"familyName\":\"Doe\",\"middleName\":\"Smith\",\"itn\":\"\",\"registeredOwner\":true,\"individualIdentification\":[{\"type\":\"passport\",\"identificationId\":\"TEST\"}]}," +
            "{\"id\":380669333333,\"givenName\":\"John\",\"familyName\":\"Doe\",\"middleName\":\"Smith\",\"itn\":\"\",\"registeredOwner\":true,\"individualIdentification\":[{\"type\":\"passport\",\"identificationId\":\"TEST\"}]}" +
            "]";
        stubFor(get(urlPathEqualTo("/crm/partyManagement/v1/individual"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(json)
            ));
        WIRE_MOCK.stubFor(get(urlEqualTo("/api/v1/customer/380669000000/blockedServiceList"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("[]")));
        WIRE_MOCK.stubFor(get(urlEqualTo("/api/v1/customer/380669111111/blockedServiceList"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("[]")));
        WIRE_MOCK.stubFor(get(urlEqualTo("/api/v1/customer/380669222222/blockedServiceList"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("[]")));

        String blocking = "[{\"serviceName\":\"Збереження номера (тех. блокування)\",\"serviceCode\":\"FRBLKMNP\",\"dateFrom\":\"2021-11-08T18:02:53Z\"}]";
        WIRE_MOCK.stubFor(get(urlEqualTo("/api/v1/customer/380669333333/blockedServiceList"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(blocking)));

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


    public static class CallFunctionTransformer extends ResponseDefinitionTransformer {

        public static FunctionService functionService;
        public static TenantContextHolder tenantContextHolder;
        public static LepManagementService lepManager;

        @Override
        public boolean applyGlobally() {
            return false;
        }

        @Override
        public String getName() {
            return "resend-one-request-in-working-time-transformer";
        }

        @Override
        @SneakyThrows
        public ResponseDefinition transform(Request request, ResponseDefinition response, FileSource files, Parameters parameters) {

            TenantContextUtils.setTenant(tenantContextHolder, "XM");
            try (var context = lepManager.beginThreadContext()) {

                OAuth2Authentication auth = mock(OAuth2Authentication.class);
                when(auth.getAuthorities()).thenReturn(List.of(new SimpleGrantedAuthority("SUPER-ADMIN")));
                SecurityContextHolder.getContext().setAuthentication(auth);

                var path = request.getUrl();
                var url = new URL(request.getAbsoluteUrl()).toURI();
                String prefix = "/api/functions/";
                var parsed = UriComponentsBuilder
                    .fromUri(url)
                    .build();
                Map params = parsed.getQueryParams().toSingleValueMap();
                var fName = parsed.getPathSegments().get(parsed.getPathSegments().size() - 1);
                if (path.startsWith(prefix)) {
                    functionService.execute(fName, params, "GET");
                } else {
                    return ResponseDefinitionBuilder
                        .like(response)
                        .but()
                        .withStatus(404)
                        .build();
                }

                return ResponseDefinitionBuilder
                    .like(response)
                    .but()
                    .withStatus(200)
                    .build();

            } catch (Exception e) {
                log.error("Error during resend-one-request-in-working-time-transformer", e);
                throw e;
            }
        }

    }


}
