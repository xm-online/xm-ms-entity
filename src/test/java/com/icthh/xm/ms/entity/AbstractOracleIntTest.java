package com.icthh.xm.ms.entity;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.icthh.xm.commons.lep.api.LepEngineSession;
import com.icthh.xm.commons.lep.api.LepManagementService;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.ms.entity.config.ApplicationProperties;
import com.icthh.xm.ms.entity.config.OracleTenantListConfiguration;
import com.icthh.xm.ms.entity.config.OracleTestContainer;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.service.XmEntitySpecService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.transaction.BeforeTransaction;

/**
 * Base class for integration tests that must run on a real Oracle: json value extraction and ordering
 * behave differently there than on PostgreSQL.
 *
 * <p>The schema is built by Liquibase, exactly as in production, so the changelog itself is covered too.
 */
@Slf4j
@ActiveProfiles("oracle-test")
@Import(OracleTenantListConfiguration.class)
public abstract class AbstractOracleIntTest extends AbstractJupiterSpringBootTest {

    public static final String TENANT = "TEST";

    @DynamicPropertySource
    static void oracleProperties(DynamicPropertyRegistry registry) {
        var container = OracleTestContainer.getInstance();
        registry.add("spring.datasource.url", container::getJdbcUrl);
        registry.add("spring.datasource.username", container::getUsername);
        registry.add("spring.datasource.password", container::getPassword);
    }

    @Autowired
    protected TenantContextHolder tenantContextHolder;
    @Autowired
    protected LepManagementService lepManagementService;
    @Autowired
    protected XmEntitySpecService xmEntitySpecService;
    @Autowired
    protected ApplicationProperties applicationProperties;

    private static final AtomicReference<ConfigurableApplicationContext> SHARED_CONTEXT = new AtomicReference<>();

    private LepEngineSession lepEngineSession;

    @Autowired
    void rememberSharedContext(ApplicationContext applicationContext) {
        SHARED_CONTEXT.set((ConfigurableApplicationContext) applicationContext);
    }

    /**
     * Closes the Spring context shared by these tests and stops the database container. Called by
     * {@link com.icthh.xm.ms.entity.config.DatabaseTestResourcesListener} once the last test class of this kind
     * has finished, so the rest of the suite does not pay for an idle context and container.
     */
    public static void releaseSharedResources() {
        ConfigurableApplicationContext context = SHARED_CONTEXT.getAndSet(null);
        if (context != null) {
            context.close();
        }
        OracleTestContainer.stop();
        log.info("Released shared Oracle test resources: context {}, container stopped",
            context != null ? "closed" : "was not created");
    }

    /**
     * Runs both before the test transaction (the tenant must be known when the connection is taken) and before
     * each test (for classes that are not transactional), hence idempotent.
     */
    @BeforeEach
    @BeforeTransaction
    public void setUpTenantContext() {
        TenantContextUtils.setTenant(tenantContextHolder, TENANT);
        if (lepEngineSession == null) {
            lepEngineSession = lepManagementService.beginThreadContext();
        }
    }

    @AfterEach
    public void tearDownTenantContext() {
        if (lepEngineSession != null) {
            lepEngineSession.close();
            lepEngineSession = null;
        }
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
    }

    @SneakyThrows
    protected void pushDbSearchSpec() {
        String yml = IOUtils.toString(
            new ClassPathResource("config/specs/xmentityspec-dbsearch.yml").getInputStream(), UTF_8);
        String key = applicationProperties.getSpecificationFolderPathPattern()
            .replace("{tenantName}", TENANT)
            .replace("*.yml", "dbsearch.yml");
        xmEntitySpecService.onRefresh(key, yml);
        xmEntitySpecService.refreshFinished(List.of(key));
    }

    protected static XmEntity newEntity(String typeKey, String name, Map<String, Object> data) {
        return new XmEntity()
            .typeKey(typeKey)
            .key(UUID.randomUUID().toString())
            .name(name)
            .startDate(Instant.now())
            .updateDate(Instant.now())
            .data(data);
    }
}
